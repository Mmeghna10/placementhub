package com.placementhub.backend.it;

import com.placementhub.backend.dto.ApplicationResponse;
import com.placementhub.backend.dto.JobResponse;
import com.placementhub.backend.entity.*;
import com.placementhub.backend.enums.JobStatus;
import com.placementhub.backend.enums.Role;
import com.placementhub.backend.service.AdminService;
import com.placementhub.backend.service.RecruiterService;
import com.placementhub.backend.service.StudentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against N+1 regressions: the number of SQL statements per read use case must
 * stay constant no matter how many rows are returned.
 *
 * Each test runs in a transaction that is rolled back afterwards, so nothing is left in
 * the test database. The persistence context is cleared before measuring so every entity
 * really has to be loaded from MySQL.
 */
@Transactional
class QueryCountIntegrationTest extends MySqlIntegrationTest {

    private static final int APPLICATIONS = 5;
    private static final int TIMELINE_ENTRIES_EACH = 2;

    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired StudentService studentService;
    @Autowired RecruiterService recruiterService;
    @Autowired AdminService adminService;

    private Statistics stats;
    private String studentEmail;
    private String recruiterEmail;
    private Long firstJobId;

    @BeforeEach
    void seed() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        String suffix = UUID.randomUUID().toString();

        Company company = new Company();
        company.setName("Acme " + suffix);
        em.persist(company);

        recruiterEmail = "rec-" + suffix + "@example.com";
        Recruiter recruiter = new Recruiter();
        recruiter.setUser(user(recruiterEmail, Role.RECRUITER));
        recruiter.setFullName("Rita Recruiter");
        recruiter.setCompany(company);
        em.persist(recruiter);

        studentEmail = "stu-" + suffix + "@example.com";
        Student student = new Student();
        student.setUser(user(studentEmail, Role.STUDENT));
        student.setFullName("Sam Student");
        em.persist(student);

        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < APPLICATIONS; i++) {
            Job job = new Job();
            job.setRecruiter(recruiter);
            job.setCompany(company);
            job.setTitle("Engineer " + i);
            job.setStatus(JobStatus.APPROVED);
            em.persist(job);
            jobs.add(job);

            Application application = new Application();
            application.setStudent(student);
            application.setJob(job);
            em.persist(application);

            for (int t = 0; t < TIMELINE_ENTRIES_EACH; t++) {
                ApplicationTimeline entry = new ApplicationTimeline();
                entry.setApplication(application);
                entry.setStatus(t == 0 ? "APPLIED" : "SHORTLISTED");
                em.persist(entry);
            }
        }
        firstJobId = jobs.get(0).getId();

        em.flush();
        em.clear();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private User user(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("not-a-real-hash");
        user.setRole(role);
        em.persist(user);
        return user;
    }

    private void loginAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    /** Runs the call with an empty persistence context and returns how many statements it ran. */
    private <T> Measured<T> measure(Supplier<T> call) {
        em.clear();
        stats.clear();
        T result = call.get();
        return new Measured<>(result, stats.getPrepareStatementCount());
    }

    private record Measured<T>(T result, long statements) {}

    @Test
    void myApplicationsUsesFourQueriesRegardlessOfCount() {
        loginAs(studentEmail);

        Measured<List<ApplicationResponse>> run = measure(studentService::getMyApplications);

        assertThat(run.result()).hasSize(APPLICATIONS);
        assertThat(run.result()).allSatisfy(a -> {
            assertThat(a.getCompanyName()).startsWith("Acme");
            assertThat(a.getTimeline()).extracting("status").containsExactly("APPLIED", "SHORTLISTED");
        });
        // student+user, applications+job+company, all timelines in one IN query, all
        // interview bookings in one IN query (Milestone 4).
        // Before Milestone 3: 1 + 1 + one timeline query per application + eager loads.
        assertThat(run.statements()).isEqualTo(4);
    }

    @Test
    void approvedJobListIsASingleQuery() {
        Measured<List<JobResponse>> run = measure(studentService::getApprovedJobs);

        assertThat(run.result()).hasSizeGreaterThanOrEqualTo(APPLICATIONS);
        assertThat(run.statements()).isEqualTo(1);
    }

    @Test
    void recruiterJobListUsesTwoQueries() {
        loginAs(recruiterEmail);

        Measured<List<JobResponse>> run = measure(recruiterService::getMyJobs);

        assertThat(run.result()).hasSize(APPLICATIONS);
        // recruiter+user+company, then jobs+company.
        assertThat(run.statements()).isEqualTo(2);
    }

    @Test
    void applicantListDoesNotQueryPerApplicant() {
        loginAs(recruiterEmail);

        Measured<List<ApplicationResponse>> run = measure(() -> recruiterService.getApplicantsForJob(firstJobId));

        assertThat(run.result()).hasSize(1);
        // recruiter, owned job, applications+job+company+student+user, timelines, bookings.
        assertThat(run.result()).allSatisfy(a -> assertThat(a.getApplicant().getFullName()).isEqualTo("Sam Student"));
        assertThat(run.statements()).isLessThanOrEqualTo(5);
    }

    @Test
    void adminStudentPageDoesNotQueryPerStudent() {
        Measured<?> run = measure(() -> adminService.getAllStudents(0, 20));

        // page query with users joined (+ a count query when there is more than one page).
        assertThat(run.statements()).isLessThanOrEqualTo(2);
    }
}
