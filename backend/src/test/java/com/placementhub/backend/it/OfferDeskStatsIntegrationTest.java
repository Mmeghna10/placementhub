package com.placementhub.backend.it;

import com.placementhub.backend.dto.ApplicationStatusUpdateRequest;
import com.placementhub.backend.dto.OfferDeskResponse;
import com.placementhub.backend.dto.PlacementOverviewResponse;
import com.placementhub.backend.entity.*;
import com.placementhub.backend.enums.ApplicationStatus;
import com.placementhub.backend.enums.JobStatus;
import com.placementhub.backend.enums.Role;
import com.placementhub.backend.repository.*;
import com.placementhub.backend.service.OfferDeskStatsService;
import com.placementhub.backend.service.OfferService;
import com.placementhub.backend.service.RecruiterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Offer Desk dashboards (phase 4) report the same numbers the offer flows produce.
 * The recruiter desk is checked exactly (it only covers this test's recruiter); the
 * campus-wide overview is checked by how much it changes, since other tests share the database.
 */
class OfferDeskStatsIntegrationTest extends MySqlIntegrationTest {

    @Autowired RecruiterService recruiterService;
    @Autowired OfferService offerService;
    @Autowired OfferDeskStatsService stats;
    @Autowired UserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired RecruiterRepository recruiterRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired JobRepository jobRepository;
    @Autowired ApplicationRepository applicationRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void theRecruitersDeskShowsOpeningsOffersWaitlistAndActivity() {
        Recruiter recruiter = recruiter();
        Job a = job(recruiter, "Backend Engineer", 2);
        Job b = job(recruiter, "Data Analyst", 1);

        Student pending = student("Pia Pending");
        offer(recruiter, application(pending, a));
        Student accepted = student("Arun Accepted");
        Application acceptedApp = offer(recruiter, application(accepted, a));
        loginAs(accepted.getUser().getEmail());
        offerService.accept(acceptedApp.getId());
        waitlist(recruiter, application(student("Wen Waiting"), a));

        Student decliner = student("Dev Decliner");
        Application declinedApp = offer(recruiter, application(decliner, b));
        waitlist(recruiter, application(student("Rhea Refill"), b));
        loginAs(decliner.getUser().getEmail());
        offerService.decline(declinedApp.getId()); // Rhea gets the opening automatically

        OfferDeskResponse desk = stats.offerDesk(recruiter.getId());

        assertThat(desk.totals().openings()).isEqualTo(3);
        assertThat(desk.totals().filled()).isEqualTo(1);
        assertThat(desk.totals().pendingOffers()).isEqualTo(2); // Pia, and Rhea from the refill
        assertThat(desk.totals().waitlisted()).isEqualTo(1);   // Wen
        assertThat(desk.totals().acceptanceRate()).isEqualTo(0.5); // 1 accepted, 1 declined
        assertThat(desk.totals().autoRefills()).isEqualTo(1);

        OfferDeskResponse.JobDesk deskA = desk.jobs().stream().filter(j -> j.jobId().equals(a.getId())).findFirst().orElseThrow();
        assertThat(deskA.filled()).isEqualTo(1);
        assertThat(deskA.offers()).extracting(OfferDeskResponse.PendingOffer::studentName).containsExactly("Pia Pending");
        assertThat(deskA.offers().get(0).expiresAt()).isNotNull();

        assertThat(desk.recentEvents()).extracting(OfferDeskResponse.Event::status)
                .contains("ACCEPTED", "DECLINED", "WAITLISTED", "OFFERED");
        assertThat(desk.recentEvents()).anyMatch(e -> e.studentName().equals("Rhea Refill")
                && e.remarks().contains("automatically from the waitlist"));

        // Another recruiter's desk shows none of it.
        assertThat(stats.offerDesk(recruiter().getId()).jobs()).isEmpty();
    }

    @Test
    void theCampusOverviewCountsPlacementsTiersAndOffers() {
        PlacementOverviewResponse before = stats.placementOverview();

        Recruiter recruiter = recruiter();
        Job job = job(recruiter, "Overview Role", 2);
        Student placed = student("Placed Student");
        Application app = offer(recruiter, application(placed, job));
        loginAs(placed.getUser().getEmail());
        offerService.accept(app.getId());
        student("Unplaced Student");

        PlacementOverviewResponse after = stats.placementOverview();

        assertThat(after.students() - before.students()).isEqualTo(2);
        assertThat(after.placedStudents() - before.placedStudents()).isEqualTo(1);
        assertThat(after.placedByTier().get("REGULAR") - before.placedByTier().get("REGULAR")).isEqualTo(1);
        assertThat(after.offers().get("ACCEPTED") - before.offers().get("ACCEPTED")).isEqualTo(1);
        assertThat(after.openings() - before.openings()).isEqualTo(2);
        assertThat(after.filled() - before.filled()).isEqualTo(1);
        assertThat(after.placementRate()).isBetween(0.0, 1.0);
        assertThat(after.avgHoursToDecide()).isNotNull();
        assertThat(after.topCompanies()).isNotEmpty();
    }

    // ------------------------------------------------------------- test data (committed)

    private Application offer(Recruiter recruiter, Application application) {
        move(recruiter, application, "OFFERED");
        return applicationRepository.findById(application.getId()).orElseThrow();
    }

    private void waitlist(Recruiter recruiter, Application application) {
        move(recruiter, application, "WAITLISTED");
    }

    private void move(Recruiter recruiter, Application application, String status) {
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus(status);
        loginAs(recruiter.getUser().getEmail());
        recruiterService.updateApplicationStatus(application.getId(), request);
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null, List.of()));
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
        company.setName("Stats Co " + UUID.randomUUID());
        company = companyRepository.save(company);
        Recruiter recruiter = new Recruiter();
        recruiter.setUser(user(Role.RECRUITER));
        recruiter.setFullName("Rita Recruiter");
        recruiter.setCompany(company);
        return recruiterRepository.save(recruiter);
    }

    private Student student(String name) {
        Student student = new Student();
        student.setUser(user(Role.STUDENT));
        student.setFullName(name);
        return studentRepository.save(student);
    }

    private Job job(Recruiter recruiter, String title, int openings) {
        Job job = new Job();
        job.setRecruiter(recruiter);
        job.setCompany(recruiter.getCompany());
        job.setTitle(title);
        job.setStatus(JobStatus.APPROVED);
        job.setOpenings(openings);
        return jobRepository.save(job);
    }

    private Application application(Student student, Job job) {
        Application application = new Application();
        application.setStudent(student);
        application.setJob(job);
        application.setStatus(ApplicationStatus.INTERVIEW);
        return applicationRepository.save(application);
    }
}
