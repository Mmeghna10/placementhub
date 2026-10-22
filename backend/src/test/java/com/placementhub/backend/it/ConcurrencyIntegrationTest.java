package com.placementhub.backend.it;

import com.placementhub.backend.dto.ApplicationStatusUpdateRequest;
import com.placementhub.backend.entity.*;
import com.placementhub.backend.enums.ApplicationStatus;
import com.placementhub.backend.enums.JobStatus;
import com.placementhub.backend.enums.Role;
import com.placementhub.backend.exception.ApiException;
import com.placementhub.backend.exception.BusinessRuleException;
import com.placementhub.backend.repository.*;
import com.placementhub.backend.service.InterviewSlotService;
import com.placementhub.backend.service.RecruiterService;
import com.placementhub.backend.service.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the Milestone 4 invariants under truly simultaneous requests against real MySQL.
 *
 * Each test releases N threads at the same instant (a start gate), each calling the real
 * service in its own transaction, then checks both the responses and the rows in the
 * database. Mocked repositories could not show any of this: the guarantees come from
 * MySQL's unique keys, row locks and atomic UPDATEs.
 *
 * Not @Transactional: the threads need committed data, and each request must commit or
 * roll back on its own. Rows use random emails, so runs do not interfere.
 */
class ConcurrencyIntegrationTest extends MySqlIntegrationTest {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int THREADS = 20;

    @Autowired StudentService studentService;
    @Autowired RecruiterService recruiterService;
    @Autowired InterviewSlotService interviewSlotService;
    @Autowired UserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired RecruiterRepository recruiterRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired JobRepository jobRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired InterviewSlotRepository slotRepository;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------- Scenario A

    @Test
    void simultaneousDuplicateApplicationsCreateExactlyOneRow() throws Exception {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, JobStatus.APPROVED, null);
        Student student = student();

        // The same student clicks Apply from 20 tabs at the same instant.
        List<Outcome> outcomes = race(THREADS, i -> () -> {
            loginAs(student.getUser().getEmail());
            studentService.applyToJob(job.getId());
        });

        assertThat(count(outcomes, "OK")).isEqualTo(1);
        assertThat(count(outcomes, "APPLICATION_ALREADY_EXISTS")).isEqualTo(THREADS - 1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM applications WHERE student_id = ? AND job_id = ?",
                Integer.class, student.getId(), job.getId())).isEqualTo(1);
        // The losing requests rolled back completely: one timeline row, not twenty.
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM application_timeline t JOIN applications a ON a.id = t.application_id
                WHERE a.student_id = ? AND a.job_id = ?""",
                Integer.class, student.getId(), job.getId())).isEqualTo(1);
    }

    // ------------------------------------------------------------- Scenario B

    @Test
    void closingAJobWaitsForAnApplicationThatIsAlreadyInProgress() throws Exception {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, JobStatus.APPROVED, null);

        // Thread A plays an application in progress: it holds the job's shared lock
        // (exactly what applyToJob takes) for 1.5 seconds before committing.
        CountDownLatch lockHeld = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> inFlight = pool.submit(() -> transactionTemplate.executeWithoutResult(tx -> {
            jobRepository.findByIdForApplication(job.getId()).orElseThrow();
            lockHeld.countDown();
            sleep(1500);
        }));
        assertThat(lockHeld.await(10, TimeUnit.SECONDS)).isTrue();

        // Meanwhile the recruiter closes the job. Its UPDATE needs an exclusive lock, so it
        // must wait until the in-flight application has committed.
        loginAs(recruiter.getUser().getEmail());
        long started = System.nanoTime();
        recruiterService.closeJob(job.getId());
        long waitedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        inFlight.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(waitedMs).as("close should block on the in-flight application's lock").isGreaterThanOrEqualTo(1000);

        // Any application that starts after the close sees the new state and is refused.
        Student student = student();
        loginAs(student.getUser().getEmail());
        assertThatThrownBy(() -> studentService.applyToJob(job.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("JOB_NOT_OPEN");
    }

    @Test
    void applicationsRacingAJobClosureAreEitherFullyAcceptedOrRefused() throws Exception {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, JobStatus.APPROVED, null);
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            students.add(student());
        }

        // 20 different students apply while, at the same instant, the recruiter closes the job.
        List<Outcome> outcomes = race(THREADS + 1, i -> () -> {
            if (i == THREADS) {
                loginAs(recruiter.getUser().getEmail());
                recruiterService.closeJob(job.getId());
            } else {
                loginAs(students.get(i).getUser().getEmail());
                studentService.applyToJob(job.getId());
            }
        });

        long accepted = count(outcomes, "OK") - 1; // minus the close itself
        long refused = count(outcomes, "JOB_NOT_OPEN");
        assertThat(count(outcomes, "OK")).isGreaterThanOrEqualTo(1);
        assertThat(accepted + refused).as("every request ended in a clear accept or refuse").isEqualTo(THREADS);
        // No "phantom" applications: exactly the accepted ones exist.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM applications WHERE job_id = ?",
                Integer.class, job.getId())).isEqualTo((int) accepted);
        assertThat(jdbc.queryForObject("SELECT status FROM jobs WHERE id = ?", String.class, job.getId()))
                .isEqualTo("CLOSED");
    }

    @Test
    void deadlineAndEligibilityAreEnforcedOnTheServer() {
        Recruiter recruiter = recruiter();
        Job expired = job(recruiter, JobStatus.APPROVED, LocalDate.now(APP_ZONE).minusDays(1));
        Job dueToday = job(recruiter, JobStatus.APPROVED, LocalDate.now(APP_ZONE));
        Job highCgpa = job(recruiter, JobStatus.APPROVED, null);
        highCgpa.setMinCgpa(new java.math.BigDecimal("9.50"));
        jobRepository.save(highCgpa);

        Student student = student();
        loginAs(student.getUser().getEmail());

        assertThatThrownBy(() -> studentService.applyToJob(expired.getId()))
                .extracting("code").isEqualTo("DEADLINE_PASSED");
        assertThatThrownBy(() -> studentService.applyToJob(highCgpa.getId()))
                .extracting("code").isEqualTo("NOT_ELIGIBLE");
        // The deadline day itself is still open.
        assertThat(studentService.applyToJob(dueToday.getId()).getStatus()).isEqualTo("APPLIED");
        // Expired jobs are not offered at all.
        assertThat(studentService.getApprovedJobs()).extracting("id")
                .doesNotContain(expired.getId()).contains(dueToday.getId());
    }

    // ------------------------------------------------------------- Scenario C

    @Test
    void aSlotIsNeverOverbookedEvenWhenStudentsBookAtTheSameInstant() throws Exception {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, JobStatus.APPROVED, null);
        int capacity = 3;
        InterviewSlot slot = slot(job, capacity);

        List<Application> applications = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            applications.add(application(student(), job, ApplicationStatus.INTERVIEW));
        }

        // 20 students, 3 seats, everyone clicks Book at once.
        List<Outcome> outcomes = race(THREADS, i -> () -> {
            Application application = applications.get(i);
            loginAs(application.getStudent().getUser().getEmail());
            interviewSlotService.bookSlot(application.getId(), slot.getId());
        });

        assertThat(count(outcomes, "OK")).isEqualTo(capacity);
        assertThat(count(outcomes, "SLOT_FULL")).isEqualTo(THREADS - capacity);
        assertThat(jdbc.queryForObject("SELECT booked_count FROM interview_slots WHERE id = ?",
                Integer.class, slot.getId())).isEqualTo(capacity);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM interview_bookings WHERE slot_id = ?",
                Integer.class, slot.getId())).isEqualTo(capacity);
    }

    @Test
    void oneStudentDoubleClickingTwoSlotsGetsOneBookingAndNoLeakedSeat() throws Exception {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, JobStatus.APPROVED, null);
        InterviewSlot first = slot(job, 5);
        InterviewSlot second = slot(job, 5);
        Application application = application(student(), job, ApplicationStatus.INTERVIEW);

        List<Outcome> outcomes = race(THREADS, i -> () -> {
            loginAs(application.getStudent().getUser().getEmail());
            interviewSlotService.bookSlot(application.getId(), (i % 2 == 0 ? first : second).getId());
        });

        assertThat(count(outcomes, "OK")).isEqualTo(1);
        assertThat(count(outcomes, "ALREADY_BOOKED")).isEqualTo(THREADS - 1);
        // Losers took a seat in reserveSeat and then rolled back: the seat must come back.
        Integer seatsTaken = jdbc.queryForObject(
                "SELECT SUM(booked_count) FROM interview_slots WHERE id IN (?, ?)",
                Integer.class, first.getId(), second.getId());
        assertThat(seatsTaken).isEqualTo(1);
    }

    // ------------------------------------------------------------- Scenario D

    @Test
    void concurrentStatusUpdatesFromTheSameVersionLetExactlyOneWin() throws Exception {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, JobStatus.APPROVED, null);
        Application application = application(student(), job, ApplicationStatus.APPLIED);
        Long seenVersion = applicationRepository.findById(application.getId()).orElseThrow().getVersion();

        // Several recruiter tabs, all showing the same version, change the status at once.
        List<Outcome> outcomes = race(THREADS, i -> () -> {
            loginAs(recruiter.getUser().getEmail());
            ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
            request.setStatus(i % 2 == 0 ? "SHORTLISTED" : "REJECTED");
            request.setVersion(seenVersion);
            recruiterService.updateApplicationStatus(application.getId(), request);
        });

        assertThat(count(outcomes, "OK")).isEqualTo(1);
        // Losers are refused either by the version check (APPLICATION_MODIFIED) or, if they
        // raced past it, by @Version at UPDATE time (CONCURRENT_UPDATE). Never overwritten.
        assertThat(count(outcomes, "APPLICATION_MODIFIED") + count(outcomes, "CONCURRENT_UPDATE"))
                .isEqualTo(THREADS - 1);

        Map<String, Object> row = jdbc.queryForMap("SELECT status, version FROM applications WHERE id = ?",
                application.getId());
        assertThat(row.get("version")).isEqualTo(seenVersion + 1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM application_timeline WHERE application_id = ?",
                Integer.class, application.getId())).isEqualTo(1);
    }

    @Test
    void invalidTransitionsAreRefused() {
        Recruiter recruiter = recruiter();
        Job job = job(recruiter, JobStatus.APPROVED, null);
        Application application = application(student(), job, ApplicationStatus.REJECTED);
        loginAs(recruiter.getUser().getEmail());

        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("OFFERED");

        assertThatThrownBy(() -> recruiterService.updateApplicationStatus(application.getId(), request))
                .extracting("code").isEqualTo("INVALID_STATUS_TRANSITION");
    }

    // ------------------------------------------------------------- race harness

    /** One task's result: "OK", the ApiException code, or the unexpected exception's name. */
    private record Outcome(String result) {}

    /**
     * Runs `threads` tasks, releases them all at the same instant, waits for them and
     * returns how each one ended.
     */
    private List<Outcome> race(int threads, Function<Integer, ThrowingRunnable> taskFactory) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                ThrowingRunnable task = taskFactory.apply(i);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        task.run();
                        return new Outcome("OK");
                    } catch (ApiException e) {
                        return new Outcome(e.getCode());
                    } catch (OptimisticLockingFailureException e) {
                        return new Outcome("CONCURRENT_UPDATE");
                    } catch (Exception e) {
                        return new Outcome(e.getClass().getSimpleName() + ": " + e.getMessage());
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(60, TimeUnit.SECONDS));
            }
            // Anything other than OK or a known business code would be a bug: show it.
            assertThat(outcomes).extracting(Outcome::result)
                    .allMatch(r -> r.equals("OK") || r.matches("[A-Z_]+"), "unexpected failures: " + summary(outcomes));
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static long count(List<Outcome> outcomes, String result) {
        return outcomes.stream().filter(o -> o.result().equals(result)).count();
    }

    private static Map<String, Long> summary(List<Outcome> outcomes) {
        return outcomes.stream().collect(Collectors.groupingBy(Outcome::result, Collectors.counting()));
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------- test data (committed)

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
        company.setName("Concurrency Co " + UUID.randomUUID());
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
        student.setCgpa(new java.math.BigDecimal("8.00"));
        student.setBranch("CSE");
        return studentRepository.save(student);
    }

    private Job job(Recruiter recruiter, JobStatus status, LocalDate deadline) {
        Job job = new Job();
        job.setRecruiter(recruiter);
        job.setCompany(recruiter.getCompany());
        job.setTitle("Concurrency test role");
        job.setStatus(status);
        job.setApplicationDeadline(deadline);
        return jobRepository.save(job);
    }

    private Application application(Student student, Job job, ApplicationStatus status) {
        Application application = new Application();
        application.setStudent(student);
        application.setJob(job);
        application.setStatus(status);
        return applicationRepository.save(application);
    }

    private InterviewSlot slot(Job job, int capacity) {
        InterviewSlot slot = new InterviewSlot();
        slot.setJob(job);
        slot.setStartsAt(LocalDateTime.now(APP_ZONE).plusDays(1).withNano(0));
        slot.setDurationMinutes(30);
        slot.setCapacity(capacity);
        return slotRepository.save(slot);
    }
}
