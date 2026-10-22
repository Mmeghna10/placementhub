package com.placementhub.backend.it;

import com.placementhub.backend.dto.JobResponse;
import com.placementhub.backend.dto.PageResponse;
import com.placementhub.backend.entity.*;
import com.placementhub.backend.enums.JobStatus;
import com.placementhub.backend.enums.Role;
import com.placementhub.backend.exception.NotFoundException;
import com.placementhub.backend.repository.ApplicationRepository;
import com.placementhub.backend.service.EligibilityPolicy;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Student job search against MySQL. Every job title carries a random marker, and every
 * query searches for it, so leftover rows from other tests cannot affect the results.
 * Rolled back after each test.
 */
@Transactional
class JobSearchIntegrationTest extends MySqlIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Kolkata"));

    @Autowired StudentService studentService;
    @Autowired EligibilityPolicy eligibilityPolicy;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;

    private String marker;
    private Recruiter recruiter;
    private Student student;
    private final Map<String, Job> jobs = new HashMap<>();

    @BeforeEach
    void seed() {
        marker = "zq" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        Company company = new Company();
        company.setName("Searchable Labs");
        em.persist(company);

        recruiter = new Recruiter();
        recruiter.setUser(user(Role.RECRUITER));
        recruiter.setCompany(company);
        em.persist(recruiter);

        student = new Student();
        student.setUser(user(Role.STUDENT));
        student.setCgpa(new BigDecimal("7.80"));
        student.setBranch("CSE");
        em.persist(student);

        //        key            title suffix          type          location     salaryMax  deadline             minCgpa  branches     status
        job("backend",  "Backend Engineer",   "FULL_TIME",  "Pune",      90_000, TODAY.plusDays(30), "7.00", "CSE, IT",  JobStatus.APPROVED);
        job("intern",   "Data Intern",        "INTERNSHIP", "Remote",    20_000, TODAY.plusDays(3),  null,   null,       JobStatus.APPROVED);
        job("strict",   "Quant Analyst",      "FULL_TIME",  "Mumbai",   150_000, null,               "9.00", null,       JobStatus.APPROVED);
        job("mech",     "Design Engineer",    "FULL_TIME",  "Pune",      60_000, TODAY.plusDays(10), null,   "Mechanical", JobStatus.APPROVED);
        job("today",    "Last Day Role",      "INTERNSHIP", "Pune",      null,   TODAY,              null,   null,       JobStatus.APPROVED);
        job("expired",  "Expired Role",       "FULL_TIME",  "Pune",      80_000, TODAY.minusDays(1), null,   null,       JobStatus.APPROVED);
        job("pending",  "Pending Role",       "FULL_TIME",  "Pune",      80_000, null,               null,   null,       JobStatus.PENDING);
        job("closed",   "Closed Role",        "FULL_TIME",  "Pune",      80_000, null,               null,   null,       JobStatus.CLOSED);
        em.flush();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(student.getUser().getEmail(), null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private List<String> titles(PageResponse<JobResponse> page) {
        return page.content().stream().map(j -> j.getTitle().replace(marker + " ", "")).toList();
    }

    @Test
    void onlyOpenJobsAreReturned() {
        // q is the marker alone, so every job of this test is a candidate.
        PageResponse<JobResponse> result = studentService.searchJobs(marker, null, null, false, "newest", 0, 50);

        assertThat(titles(result)).containsExactlyInAnyOrder(
                "Backend Engineer", "Data Intern", "Quant Analyst", "Design Engineer", "Last Day Role");
        assertThat(result.content()).allMatch(JobResponse::getAcceptingApplications);
    }

    @Test
    void textSearchMatchesTitleCompanyAndSkillsCaseInsensitively() {
        assertThat(titles(studentService.searchJobs(marker.toUpperCase(), null, null, false, null, 0, 50))).hasSize(5);
        assertThat(studentService.searchJobs("searchable labs", null, null, false, null, 0, 50).content())
                .extracting(JobResponse::getTitle).anyMatch(t -> t.contains(marker));
        assertThat(studentService.searchJobs("kafka-" + marker, null, null, false, null, 0, 50).content())
                .extracting(JobResponse::getTitle).containsExactly(marker + " Backend Engineer");
    }

    @Test
    void likeWildcardsInTheSearchTextAreTreatedLiterally() {
        // "%" would match everything if it were passed to LIKE unescaped.
        assertThat(studentService.searchJobs("%", null, null, false, null, 0, 50).content())
                .noneMatch(j -> j.getTitle().contains(marker));
    }

    @Test
    void filtersByTypeAndLocation() {
        assertThat(titles(studentService.searchJobs(marker, "INTERNSHIP", null, false, null, 0, 50)))
                .containsExactlyInAnyOrder("Data Intern", "Last Day Role");
        assertThat(titles(studentService.searchJobs(marker, "FULL_TIME", "pune", false, null, 0, 50)))
                .containsExactlyInAnyOrder("Backend Engineer", "Design Engineer");
    }

    @Test
    void eligibleOnlyInSqlAgreesWithTheJavaEligibilityPolicy() {
        List<String> sql = titles(studentService.searchJobs(marker, null, null, true, null, 0, 50));
        List<String> java = jobs.values().stream()
                .filter(j -> j.getStatus() == JobStatus.APPROVED)
                .filter(j -> j.getApplicationDeadline() == null || !TODAY.isAfter(j.getApplicationDeadline()))
                .filter(j -> eligibilityPolicy.isEligible(j, student))
                .map(j -> j.getTitle().replace(marker + " ", ""))
                .toList();

        // CGPA 7.8, branch CSE: not Quant Analyst (needs 9.0), not Design Engineer (Mechanical).
        assertThat(sql).containsExactlyInAnyOrderElementsOf(java)
                .containsExactlyInAnyOrder("Backend Engineer", "Data Intern", "Last Day Role");
    }

    @Test
    void sortsAndPaginates() {
        assertThat(titles(studentService.searchJobs(marker, null, null, false, "salary", 0, 50)))
                .startsWith("Quant Analyst", "Backend Engineer", "Design Engineer", "Data Intern")
                .endsWith("Last Day Role"); // no salary: last
        assertThat(titles(studentService.searchJobs(marker, null, null, false, "deadline", 0, 50)))
                .containsExactly("Last Day Role", "Data Intern", "Design Engineer", "Backend Engineer", "Quant Analyst");

        PageResponse<JobResponse> second = studentService.searchJobs(marker, null, null, false, "deadline", 1, 2);
        assertThat(second.totalElements()).isEqualTo(5);
        assertThat(second.totalPages()).isEqualTo(3);
        assertThat(titles(second)).containsExactly("Design Engineer", "Backend Engineer");
    }

    @Test
    void pageSizeIsCapped() {
        assertThat(studentService.searchJobs(marker, null, null, false, null, 0, 10_000).size()).isEqualTo(50);
    }

    @Test
    void searchIsTwoQueriesNoMatterHowManyJobs() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        em.clear();
        stats.clear();

        studentService.searchJobs(marker, null, null, false, null, 0, 2);

        // one page query with company joined + one count query.
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void closedJobIsVisibleOnlyToStudentsWhoApplied() {
        Job closed = jobs.get("closed");
        assertThatThrownBy(() -> studentService.getJob(closed.getId())).isInstanceOf(NotFoundException.class);

        Application application = new Application();
        application.setStudent(student);
        application.setJob(closed);
        applicationRepository.saveAndFlush(application);

        JobResponse job = studentService.getJob(closed.getId());
        assertThat(job.getAcceptingApplications()).isFalse();
        assertThat(studentService.getJob(jobs.get("backend").getId()).getAcceptingApplications()).isTrue();
        assertThatThrownBy(() -> studentService.getJob(jobs.get("pending").getId())).isInstanceOf(NotFoundException.class);
    }

    // ------------------------------------------------------------------ data

    private User user(Role role) {
        User user = new User();
        user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("not-a-real-hash");
        user.setRole(role);
        em.persist(user);
        return user;
    }

    private void job(String key, String title, String type, String location, Integer salaryMax, LocalDate deadline,
                     String minCgpa, String branches, JobStatus status) {
        Job job = new Job();
        job.setRecruiter(recruiter);
        job.setCompany(recruiter.getCompany());
        job.setTitle(marker + " " + title);
        job.setJobType(type);
        job.setLocation(location);
        job.setSalaryMax(salaryMax);
        job.setApplicationDeadline(deadline);
        job.setMinCgpa(minCgpa == null ? null : new BigDecimal(minCgpa));
        job.setEligibleBranches(branches);
        job.setStatus(status);
        job.setRequiredSkills(key.equals("backend") ? "Java, Kafka-" + marker : "General");
        em.persist(job);
        jobs.put(key, job);
    }
}
