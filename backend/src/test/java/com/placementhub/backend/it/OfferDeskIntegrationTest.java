package com.placementhub.backend.it;

import com.placementhub.backend.dto.ApplicationStatusUpdateRequest;
import com.placementhub.backend.dto.JobRequest;
import com.placementhub.backend.entity.*;
import com.placementhub.backend.enums.ApplicationStatus;
import com.placementhub.backend.enums.JobStatus;
import com.placementhub.backend.enums.Role;
import com.placementhub.backend.exception.ApiException;
import com.placementhub.backend.repository.*;
import com.placementhub.backend.service.InterviewSlotService;
import com.placementhub.backend.service.OfferService;
import com.placementhub.backend.service.RecruiterService;
import com.placementhub.backend.service.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Offer Desk invariants under truly simultaneous requests against real MySQL:
 * openings are never over-offered, a student holds one pending offer and is placed once,
 * an offer ends exactly once, and a student is never booked into two overlapping interviews.
 *
 * Same harness as ConcurrencyIntegrationTest: N threads released at one instant, each in
 * its own transaction, then the responses and the rows are checked.
 */
class OfferDeskIntegrationTest extends MySqlIntegrationTest {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int THREADS = 20;

    @Autowired RecruiterService recruiterService;
    @Autowired StudentService studentService;
    @Autowired OfferService offerService;
    @Autowired InterviewSlotService interviewSlotService;
    @Autowired UserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired RecruiterRepository recruiterRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired JobRepository jobRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired InterviewSlotRepository slotRepository;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------- openings

    @Test
    void openingsAreNeverOverOfferedWhenRecruitersOfferAtTheSameInstant() throws Exception {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, 3, 72);
        List<Application> candidates = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            candidates.add(application(student(), job, ApplicationStatus.INTERVIEW));
        }

        // 20 offers for 3 openings, all at once (e.g. a recruiting team clicking together).
        List<String> outcomes = race(THREADS, i -> () -> {
            loginAs(recruiter.getUser().getEmail());
            recruiterService.updateApplicationStatus(candidates.get(i).getId(), offer());
        });

        assertThat(count(outcomes, "OK")).isEqualTo(3);
        assertThat(count(outcomes, "NO_OPENINGS_LEFT")).isEqualTo(THREADS - 3);
        assertThat(seats(job)).isEqualTo(List.of(3, 0));
        assertThat(countWithStatus(job, "OFFERED")).isEqualTo(3);
        // Refused offers rolled back completely: no student holds a stray pending offer.
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM students s JOIN applications a ON a.student_id = s.id
                WHERE a.job_id = ? AND s.pending_offer_application_id IS NOT NULL""", Integer.class, job.getId()))
                .isEqualTo(3);
    }

    @Test
    void aStudentHoldsOnlyOnePendingOfferEvenWhenCompaniesOfferTogether() throws Exception {
        Student student = student();
        List<Recruiter> recruiters = new ArrayList<>();
        List<Application> applications = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Recruiter recruiter = recruiter();
            recruiters.add(recruiter);
            applications.add(application(student, job(recruiter, 1, 72), ApplicationStatus.INTERVIEW));
        }

        // 20 companies offer the same student at the same instant.
        List<String> outcomes = race(THREADS, i -> () -> {
            loginAs(recruiters.get(i).getUser().getEmail());
            recruiterService.updateApplicationStatus(applications.get(i).getId(), offer());
        });

        assertThat(count(outcomes, "OK")).isEqualTo(1);
        assertThat(count(outcomes, "STUDENT_HAS_PENDING_OFFER")).isEqualTo(THREADS - 1);
        // The refused companies kept their openings: their seat reservations rolled back.
        assertThat(jdbc.queryForObject("""
                SELECT SUM(j.offers_pending) FROM jobs j JOIN applications a ON a.job_id = j.id
                WHERE a.student_id = ?""", Integer.class, student.getId())).isEqualTo(1);
        Long pending = jdbc.queryForObject("SELECT pending_offer_application_id FROM students WHERE id = ?",
                Long.class, student.getId());
        assertThat(applicationRepository.findById(pending).orElseThrow().getStatus()).isEqualTo(ApplicationStatus.OFFERED);
    }

    // ------------------------------------------------------------- deciding an offer

    @Test
    void anOfferIsAcceptedExactlyOnceEvenWhenTheStudentDoubleClicks() throws Exception {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, 2, 72);
        Student student = student();
        Application application = offered(recruiter, job, student);
        String studentEmail = student.getUser().getEmail();

        List<String> outcomes = race(THREADS, i -> () -> {
            loginAs(studentEmail);
            offerService.accept(application.getId());
        });

        assertThat(count(outcomes, "OK")).isEqualTo(1);
        assertThat(outcomes).allMatch(o -> o.equals("OK") || o.equals("CONCURRENT_UPDATE") || o.equals("OFFER_NOT_PENDING"));
        assertThat(status(application)).isEqualTo("ACCEPTED");
        assertThat(seats(job)).isEqualTo(List.of(0, 1));
        assertThat(studentPointers(student)).isEqualTo(Arrays.asList(null, application.getId()));
        assertThat(timelineCount(application, "ACCEPTED")).isEqualTo(1);
    }

    @Test
    void acceptAndDeclineAtTheSameInstantEndTheOfferExactlyOnce() throws Exception {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, 1, 72);
        Student student = student();
        Application application = offered(recruiter, job, student);
        String studentEmail = student.getUser().getEmail();

        // Two tabs: half the clicks accept, half decline.
        List<String> outcomes = race(THREADS, i -> () -> {
            loginAs(studentEmail);
            if (i % 2 == 0) {
                offerService.accept(application.getId());
            } else {
                offerService.decline(application.getId());
            }
        });

        assertThat(count(outcomes, "OK")).isEqualTo(1);
        String finalStatus = status(application);
        assertThat(finalStatus).isIn("ACCEPTED", "DECLINED");
        // Counters match whichever decision won; nothing half-applied.
        if (finalStatus.equals("ACCEPTED")) {
            assertThat(seats(job)).isEqualTo(List.of(0, 1));
            assertThat(studentPointers(student)).isEqualTo(Arrays.asList(null, application.getId()));
        } else {
            assertThat(seats(job)).isEqualTo(List.of(0, 0));
            assertThat(studentPointers(student)).isEqualTo(Arrays.asList(null, null));
        }
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM application_timeline WHERE application_id = ? AND status IN ('ACCEPTED', 'DECLINED')""",
                Integer.class, application.getId())).isEqualTo(1);
    }

    @Test
    void expiryRacingADeclineEndsTheOfferOnceAndFreesTheOpening() throws Exception {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, 1, 72);
        Student student = student();
        Application application = offered(recruiter, job, student);
        jdbc.update("UPDATE applications SET offer_expires_at = ? WHERE id = ?",
                LocalDateTime.now(APP_ZONE).minusMinutes(1), application.getId());
        String studentEmail = student.getUser().getEmail();

        // The background job (possibly on several servers) and the student's decline collide.
        List<String> outcomes = race(THREADS, i -> () -> {
            if (i % 2 == 0) {
                offerService.expire(application.getId());
            } else {
                loginAs(studentEmail);
                offerService.decline(application.getId());
            }
        });

        assertThat(status(application)).isIn("EXPIRED", "DECLINED");
        assertThat(seats(job)).isEqualTo(List.of(0, 0));
        assertThat(studentPointers(student)).isEqualTo(Arrays.asList(null, null));
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM application_timeline WHERE application_id = ? AND status IN ('EXPIRED', 'DECLINED')""",
                Integer.class, application.getId())).isEqualTo(1);
        assertThat(outcomes).allMatch(o -> o.matches("OK|CONCURRENT_UPDATE|OFFER_NOT_PENDING"));
    }

    @Test
    void anExpiredOfferIsPickedUpByTheJobAndCannotBeAccepted() {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, 1, 72);
        Student student = student();
        Application application = offered(recruiter, job, student);
        LocalDateTime past = LocalDateTime.now(APP_ZONE).minusSeconds(5).withNano(0);
        jdbc.update("UPDATE applications SET offer_expires_at = ? WHERE id = ?", past, application.getId());

        // Before the job runs, accepting a lapsed offer is still refused.
        loginAs(student.getUser().getEmail());
        assertThatThrownBy(() -> offerService.accept(application.getId()))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("OFFER_EXPIRED");

        List<Long> due = applicationRepository.findDueOfferIds(LocalDateTime.now(APP_ZONE), PageRequest.of(0, 1000));
        assertThat(due).contains(application.getId());
        assertThat(offerService.expire(application.getId())).isTrue();
        assertThat(offerService.expire(application.getId())).as("second run changes nothing").isFalse();

        assertThat(status(application)).isEqualTo("EXPIRED");
        assertThat(seats(job)).isEqualTo(List.of(0, 0));
        // The student is free for another offer again.
        Application next = offered(recruiter(), null, student);
        assertThat(status(next)).isEqualTo("OFFERED");
    }

    @Test
    void aPlacedStudentCannotApplyOrReceiveAnotherOffer() {
        Recruiter recruiter = recruiter();
        Student student = student();
        Application application = offered(recruiter, job(recruiter, 1, 72), student);
        loginAs(student.getUser().getEmail());
        offerService.accept(application.getId());

        Recruiter other = recruiter();
        Job otherJob = job(other, 1, 72);
        loginAs(student.getUser().getEmail());
        assertThatThrownBy(() -> studentService.applyToJob(otherJob.getId()))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ALREADY_PLACED");

        Application earlier = application(student, otherJob, ApplicationStatus.INTERVIEW);
        loginAs(other.getUser().getEmail());
        assertThatThrownBy(() -> recruiterService.updateApplicationStatus(earlier.getId(), offer()))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("STUDENT_ALREADY_PLACED");
        assertThat(seats(otherJob)).isEqualTo(List.of(0, 0));
    }

    @Test
    void openingsCannotBeReducedBelowOffersAlreadyMade() {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, 2, 72);
        offered(recruiter, job, student());
        offered(recruiter, job, student());

        JobRequest edit = new JobRequest();
        edit.setTitle(job.getTitle());
        edit.setOpenings(1);
        loginAs(recruiter.getUser().getEmail());
        assertThatThrownBy(() -> recruiterService.updateJob(job.getId(), edit))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("OPENINGS_BELOW_OFFERS");

        edit.setOpenings(4);
        assertThat(recruiterService.updateJob(job.getId(), edit).getOpenings()).isEqualTo(4);
    }

    // ------------------------------------------------------------- interview clashes

    @Test
    void aStudentIsNeverBookedIntoTwoOverlappingInterviews() throws Exception {
        Student student = student();
        LocalDateTime tenAm = LocalDateTime.now(APP_ZONE).plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Recruiter a = recruiter();
        Recruiter b = recruiter();
        Application atA = application(student, job(a, 1, 72), ApplicationStatus.INTERVIEW);
        Application atB = application(student, job(b, 1, 72), ApplicationStatus.INTERVIEW);
        InterviewSlot slotA = slot(atA.getJob(), tenAm, 45, 5);
        InterviewSlot slotB = slot(atB.getJob(), tenAm.plusMinutes(30), 45, 5); // overlaps 10:30-10:45

        // Two tabs booking both companies' overlapping slots at the same instant.
        List<String> outcomes = race(THREADS, i -> () -> {
            loginAs(student.getUser().getEmail());
            if (i % 2 == 0) {
                interviewSlotService.bookSlot(atA.getId(), slotA.getId());
            } else {
                interviewSlotService.bookSlot(atB.getId(), slotB.getId());
            }
        });

        assertThat(count(outcomes, "OK")).isEqualTo(1);
        assertThat(outcomes).allMatch(o -> o.matches("OK|INTERVIEW_CLASH|ALREADY_BOOKED"));
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM interview_bookings b JOIN applications a ON a.id = b.application_id
                WHERE a.student_id = ?""", Integer.class, student.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT booked_count FROM interview_slots WHERE id = ?", Integer.class, slotA.getId())
                + jdbc.queryForObject("SELECT booked_count FROM interview_slots WHERE id = ?", Integer.class, slotB.getId()))
                .as("no seat leaked by the refused bookings").isEqualTo(1);

        // Back-to-back interviews that do not overlap are fine.
        Application atC = application(student, job(recruiter(), 1, 72), ApplicationStatus.INTERVIEW);
        InterviewSlot afterBoth = slot(atC.getJob(), tenAm.plusHours(2), 30, 5);
        loginAs(student.getUser().getEmail());
        interviewSlotService.bookSlot(atC.getId(), afterBoth.getId());
    }

    // ------------------------------------------------------------- waitlist (phase 2)

    @Test
    void aDeclinedOrExpiredOfferGoesToTheNextOnTheWaitlistAutomatically() {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, 1, 72);
        Student first = student();
        Application offer = offered(recruiter, job, first);
        Application next = waitlisted(recruiter, job, student());
        Application after = waitlisted(recruiter, job, student());

        loginAs(first.getUser().getEmail());
        offerService.decline(offer.getId());

        assertThat(status(next)).isEqualTo("OFFERED");
        assertThat(status(after)).isEqualTo("WAITLISTED");
        assertThat(seats(job)).isEqualTo(List.of(1, 0));
        assertThat(jdbc.queryForObject("SELECT remarks FROM application_timeline WHERE application_id = ? AND status = 'OFFERED'",
                String.class, next.getId())).contains("automatically from the waitlist");

        // The promoted candidate does not reply in time: the opening moves on again.
        jdbc.update("UPDATE applications SET offer_expires_at = ? WHERE id = ?",
                LocalDateTime.now(APP_ZONE).minusMinutes(1), next.getId());
        assertThat(offerService.expire(next.getId())).isTrue();
        assertThat(status(after)).isEqualTo("OFFERED");
        assertThat(seats(job)).isEqualTo(List.of(1, 0));
    }

    @Test
    void aBusyCandidateIsSkippedAndGetsTheOpeningOnceFree() {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, 1, 72);
        Student first = student();
        Application offer = offered(recruiter, job, first);
        Student busy = student();
        Application busyHere = waitlisted(recruiter, job, busy);
        Recruiter other = recruiter();
        Application busyElsewhere = offered(other, job(other, 1, 72), busy); // holds another offer

        loginAs(first.getUser().getEmail());
        offerService.decline(offer.getId());
        assertThat(status(busyHere)).as("skipped: holds another offer").isEqualTo("WAITLISTED");
        assertThat(seats(job)).isEqualTo(List.of(0, 0));

        // The busy student declines the other offer and is now free: the waiting job retries.
        loginAs(busy.getUser().getEmail());
        offerService.decline(busyElsewhere.getId());
        assertThat(status(busyHere)).isEqualTo("OFFERED");
        assertThat(seats(job)).isEqualTo(List.of(1, 0));
    }

    @Test
    void simultaneousDeclinesNeverPromoteACandidateTwice() throws Exception {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, 10, 72);
        List<Student> holders = new ArrayList<>();
        List<Application> offers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Student holder = student();
            holders.add(holder);
            offers.add(offered(recruiter, job, holder));
        }
        List<Application> waitlist = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            waitlist.add(waitlisted(recruiter, job, student()));
        }

        // All 10 offer holders decline at the same instant; only 3 candidates are waiting.
        List<String> outcomes = race(10, i -> () -> {
            loginAs(holders.get(i).getUser().getEmail());
            offerService.decline(offers.get(i).getId());
        });

        assertThat(count(outcomes, "OK")).as("no decline fails because of a promotion race").isEqualTo(10);
        assertThat(waitlist).allMatch(a -> status(a).equals("OFFERED"));
        assertThat(seats(job)).isEqualTo(List.of(3, 0));
        for (Application promoted : waitlist) {
            assertThat(timelineCount(promoted, "OFFERED")).isEqualTo(1);
        }
    }

    @Test
    void manyFillClicksAtOnceOfferEachFreeOpeningOnce() throws Exception {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, 4, 72);
        offered(recruiter, job, student());
        List<Application> waitlist = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            waitlist.add(waitlisted(recruiter, job, student()));
        }

        // 3 free openings; 20 "fill from waitlist" clicks at the same instant.
        List<String> outcomes = race(THREADS, i -> () -> {
            loginAs(recruiter.getUser().getEmail());
            recruiterService.fillFromWaitlist(job.getId());
        });

        assertThat(count(outcomes, "OK")).isEqualTo(THREADS);
        assertThat(seats(job)).isEqualTo(List.of(4, 0));
        // Rank order: the first three on the waitlist got the openings.
        assertThat(waitlist.stream().map(this::status).toList())
                .containsExactly("OFFERED", "OFFERED", "OFFERED", "WAITLISTED", "WAITLISTED");
    }

    @Test
    void theRecruiterCanReorderTheWaitlistButNotFromAStaleList() {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, 1, 72);
        Student first = student();
        Application offer = offered(recruiter, job, first);
        Application a = waitlisted(recruiter, job, student());
        Application b = waitlisted(recruiter, job, student());

        loginAs(recruiter.getUser().getEmail());
        recruiterService.reorderWaitlist(job.getId(), List.of(b.getId(), a.getId()));
        assertThatThrownBy(() -> recruiterService.reorderWaitlist(job.getId(), List.of(a.getId())))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("WAITLIST_CHANGED");

        loginAs(first.getUser().getEmail());
        offerService.decline(offer.getId());
        assertThat(status(b)).as("b was moved to the front").isEqualTo("OFFERED");
        assertThat(status(a)).isEqualTo("WAITLISTED");
    }

    // ------------------------------------------------------------- race harness

    private List<String> race(int threads, Function<Integer, ThrowingRunnable> taskFactory) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                ThrowingRunnable task = taskFactory.apply(i);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        task.run();
                        return "OK";
                    } catch (ApiException e) {
                        return e.getCode();
                    } catch (OptimisticLockingFailureException e) {
                        return "CONCURRENT_UPDATE";
                    } catch (Exception e) {
                        return e.getClass().getSimpleName() + ": " + e.getMessage();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : futures) {
                outcomes.add(future.get(60, TimeUnit.SECONDS));
            }
            assertThat(outcomes).allMatch(r -> r.equals("OK") || r.matches("[A-Z_]+"),
                    "unexpected failures: " + outcomes.stream().collect(Collectors.groupingBy(o -> o, Collectors.counting())));
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static long count(List<String> outcomes, String result) {
        return outcomes.stream().filter(result::equals).count();
    }

    // ------------------------------------------------------------- reading the database

    /** [offers_pending, offers_accepted] straight from the jobs row. */
    private List<Integer> seats(Job job) {
        return jdbc.queryForObject("SELECT offers_pending, offers_accepted FROM jobs WHERE id = ?",
                (rs, n) -> List.of(rs.getInt(1), rs.getInt(2)), job.getId());
    }

    /** [pending_offer_application_id, placed_application_id] of the student. */
    private List<Long> studentPointers(Student student) {
        return jdbc.queryForObject("SELECT pending_offer_application_id, placed_application_id FROM students WHERE id = ?",
                (rs, n) -> Arrays.asList(rs.getObject(1, Long.class), rs.getObject(2, Long.class)), student.getId());
    }

    private String status(Application application) {
        return jdbc.queryForObject("SELECT status FROM applications WHERE id = ?", String.class, application.getId());
    }

    private int countWithStatus(Job job, String status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM applications WHERE job_id = ? AND status = ?",
                Integer.class, job.getId(), status);
    }

    private int timelineCount(Application application, String status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM application_timeline WHERE application_id = ? AND status = ?",
                Integer.class, application.getId(), status);
    }

    // ------------------------------------------------------------- test data (committed)

    private static ApplicationStatusUpdateRequest offer() {
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("OFFERED");
        return request;
    }

    /** An application at `job` (a new one for `recruiter` if null) that the recruiter has offered. */
    private Application offered(Recruiter recruiter, Job job, Student student) {
        Job target = job != null ? job : job(recruiter, 1, 72);
        Application application = application(student, target, ApplicationStatus.INTERVIEW);
        loginAs(recruiter.getUser().getEmail());
        recruiterService.updateApplicationStatus(application.getId(), offer());
        SecurityContextHolder.clearContext();
        return applicationRepository.findById(application.getId()).orElseThrow();
    }

    /** An interviewed application that the recruiter puts on the job's waitlist. */
    private Application waitlisted(Recruiter recruiter, Job job, Student student) {
        Application application = application(student, job, ApplicationStatus.INTERVIEW);
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("WAITLISTED");
        loginAs(recruiter.getUser().getEmail());
        recruiterService.updateApplicationStatus(application.getId(), request);
        SecurityContextHolder.clearContext();
        return application;
    }

    private void loginAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    private User user(Role role) {
        User user = new User();
        user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("not-a-real-hash");
        user.setRole(role);
        return userRepository.save(user);
    }

    private Recruiter recruiter() {
        Company company = new Company();
        company.setName("Offer Co " + UUID.randomUUID());
        company = companyRepository.save(company);

        Recruiter recruiter = new Recruiter();
        recruiter.setUser(user(Role.RECRUITER));
        recruiter.setFullName("Rita Recruiter");
        recruiter.setCompany(company);
        return recruiterRepository.save(recruiter);
    }

    private Student student() {
        Student student = new Student();
        student.setUser(user(Role.STUDENT));
        student.setFullName("Student " + UUID.randomUUID().toString().substring(0, 8));
        student.setCgpa(new BigDecimal("8.00"));
        student.setBranch("CSE");
        return studentRepository.save(student);
    }

    private Job job(Recruiter recruiter, int openings, int offerDeadlineHours) {
        Job job = new Job();
        job.setRecruiter(recruiter);
        job.setCompany(recruiter.getCompany());
        job.setTitle("Offer test role");
        job.setStatus(JobStatus.APPROVED);
        job.setOpenings(openings);
        job.setOfferDeadlineHours(offerDeadlineHours);
        return jobRepository.save(job);
    }

    private Application application(Student student, Job job, ApplicationStatus status) {
        Application application = new Application();
        application.setStudent(student);
        application.setJob(job);
        application.setStatus(status);
        return applicationRepository.save(application);
    }

    private InterviewSlot slot(Job job, LocalDateTime startsAt, int minutes, int capacity) {
        InterviewSlot slot = new InterviewSlot();
        slot.setJob(job);
        slot.setStartsAt(startsAt);
        slot.setDurationMinutes(minutes);
        slot.setCapacity(capacity);
        return slotRepository.save(slot);
    }
}
