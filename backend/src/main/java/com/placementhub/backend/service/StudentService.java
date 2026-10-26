package com.placementhub.backend.service;

import com.placementhub.backend.exception.*;
import com.placementhub.backend.dto.*;
import com.placementhub.backend.entity.*;
import com.placementhub.backend.enums.ApplicationStatus;
import com.placementhub.backend.enums.JobStatus;
import com.placementhub.backend.mapper.ApplicationMapper;
import com.placementhub.backend.mapper.JobMapper;
import com.placementhub.backend.repository.*;
import com.placementhub.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reads run in a read-only transaction (class default); each write use case is one
 * read-write transaction, so its inserts/updates commit or roll back together.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationTimelineRepository timelineRepository;
    private final JobMapper jobMapper;
    private final ApplicationMapper applicationMapper;
    private final EligibilityPolicy eligibilityPolicy;
    private final Clock clock;
    private final PlacementRules placementRules;

    private Student getCurrentStudent() {
        String email = SecurityUtils.getCurrentUserEmail();
        return studentRepository.findByUser_Email(email)
                .orElseThrow(() -> new NotFoundException("Student profile not found"));
    }

    public StudentProfileResponse getProfile() {
        return mapToProfileResponse(getCurrentStudent());
    }

    @Transactional
    public StudentProfileResponse updateProfile(StudentProfileUpdateRequest request) {
        Student student = getCurrentStudent();

        student.setFullName(request.getFullName());
        student.setPhone(request.getPhone());
        student.setCollege(request.getCollege());
        student.setDegree(request.getDegree());
        student.setBranch(request.getBranch());
        student.setGraduationYear(request.getGraduationYear());
        student.setCgpa(request.getCgpa());
        student.setResumeUrl(request.getResumeUrl());
        student.setSkills(request.getSkills());
        student.setLinkedinUrl(request.getLinkedinUrl());
        student.setGithubUrl(request.getGithubUrl());
        student.setProfileCompleted(true);

        student = studentRepository.save(student);
        return mapToProfileResponse(student);
    }

    /** Approved jobs whose deadline has not passed (today in the app's time zone). */
    public List<JobResponse> getApprovedJobs() {
        return openJobs().stream()
                .map(jobMapper::toResponse)
                .toList();
    }

    /** Open jobs the student is eligible for, judged by the same rules as applying. */
    public List<JobResponse> getRecommendedJobs() {
        Student student = getCurrentStudent();
        return openJobs().stream()
                .filter(job -> eligibilityPolicy.isEligible(job, student))
                .map(jobMapper::toResponse)
                .toList();
    }

    private List<Job> openJobs() {
        return jobRepository.findOpenJobs(JobStatus.APPROVED, LocalDate.now(clock));
    }

    /** Allowed sort keys for job search, mapped to entity properties (never raw user input). */
    private static final Map<String, Sort> JOB_SORTS = Map.of(
            "newest", Sort.by(Sort.Direction.DESC, "postedAt").and(Sort.by(Sort.Direction.DESC, "id")),
            "deadline", Sort.by(Sort.Order.asc("applicationDeadline").nullsLast()).and(Sort.by(Sort.Direction.DESC, "id")),
            "salary", Sort.by(Sort.Order.desc("salaryMax").nullsLast()).and(Sort.by(Sort.Direction.DESC, "id")));

    static final int MAX_PAGE_SIZE = 50;

    /**
     * Searches open jobs page by page. All filtering, eligibility and paging happen in one
     * SQL query (JobRepository.searchOpenJobs) instead of loading every job into memory.
     */
    public PageResponse<JobResponse> searchJobs(String text, String jobType, String location,
                                                boolean eligibleOnly, String sort, int page, int size) {
        Student student = eligibleOnly ? getCurrentStudent() : null;
        String branch = student == null || student.getBranch() == null || student.getBranch().isBlank()
                ? null : likePattern(student.getBranch());

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                JOB_SORTS.getOrDefault(sort == null ? "newest" : sort, JOB_SORTS.get("newest")));

        Page<Job> jobs = jobRepository.searchOpenJobs(JobStatus.APPROVED, LocalDate.now(clock),
                likePattern(text), blankToNull(jobType), likePattern(location),
                eligibleOnly, student == null ? null : student.getCgpa(), branch,
                student == null ? 0 : placementRules.minimumTierRankFor(student), pageable);

        return PageResponse.from(jobs.map(job -> {
            JobResponse response = jobMapper.toResponse(job);
            response.setAcceptingApplications(true);
            return response;
        }));
    }

    /**
     * One job for the job details page. Open jobs are visible to every student; a job that
     * has closed (or passed its deadline) stays visible only to students who applied to it,
     * so they can still see what they applied for. Anything else is 404.
     */
    public JobResponse getJob(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new NotFoundException("Job not found"));
        boolean open = isAcceptingApplications(job);
        if (!open && !applicationRepository.existsByStudentIdAndJobId(getCurrentStudent().getId(), jobId)) {
            throw new NotFoundException("Job not found");
        }
        JobResponse response = jobMapper.toResponse(job);
        response.setAcceptingApplications(open);
        return response;
    }

    private boolean isAcceptingApplications(Job job) {
        return job.getStatus() == JobStatus.APPROVED
                && (job.getApplicationDeadline() == null
                    || !LocalDate.now(clock).isAfter(job.getApplicationDeadline()));
    }

    /** "  Data  " -> "%data%"; blank -> null (filter not applied). LIKE wildcards are escaped. */
    static String likePattern(String value) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            return null;
        }
        String escaped = trimmed.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Applies the current student to a job. Every rule is checked on the server, inside one
     * transaction:
     *
     * 1. The job row is read with a shared lock (see JobRepository.findByIdForApplication),
     *    so it cannot be closed, edited or have its deadline moved until this commits.
     *    Decision rule: an application is accepted if the job is APPROVED and today (in
     *    app.timezone) is on or before its deadline at the moment this transaction reads
     *    the locked job row.
     * 2. Eligibility (CGPA, branch) is checked with the same policy as "Recommended".
     * 3. UNIQUE(student_id, job_id) decides duplicates. The exists() check only gives a
     *    friendly early answer; two simultaneous requests can both pass it, and then the
     *    database rejects the second insert, which is reported as the same 409.
     */
    @Transactional
    public ApplicationResponse applyToJob(Long jobId) {
        Student student = getCurrentStudent();

        Job job = jobRepository.findByIdForApplication(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));
        // Phase 3: a placed student may only apply to higher tiers, up to the maximum.
        placementRules.refusalReason(student, job).ifPresent(reason -> {
            throw new BusinessRuleException("ALREADY_PLACED", reason);
        });

        if (job.getStatus() != JobStatus.APPROVED) {
            throw new BusinessRuleException("JOB_NOT_OPEN", "This job is not accepting applications");
        }
        LocalDate today = LocalDate.now(clock);
        if (job.getApplicationDeadline() != null && today.isAfter(job.getApplicationDeadline())) {
            throw new BusinessRuleException("DEADLINE_PASSED",
                    "The application deadline for this job (" + job.getApplicationDeadline() + ") has passed");
        }
        eligibilityPolicy.ineligibilityReason(job, student).ifPresent(reason -> {
            throw new BusinessRuleException("NOT_ELIGIBLE", reason);
        });

        if (applicationRepository.existsByStudentIdAndJobId(student.getId(), jobId)) {
            throw alreadyApplied();
        }

        Application application = new Application();
        application.setStudent(student);
        application.setJob(job);
        try {
            application = applicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException e) {
            if (isConstraint(e, "uk_applications_student_job")) {
                throw alreadyApplied();
            }
            throw e;
        }

        ApplicationTimeline timelineEntry = new ApplicationTimeline();
        timelineEntry.setApplication(application);
        timelineEntry.setStatus(ApplicationStatus.APPLIED.name());
        timelineEntry.setRemarks("Application submitted");
        timelineRepository.save(timelineEntry);

        return applicationMapper.toResponse(application);
    }

    private static ConflictException alreadyApplied() {
        return new ConflictException("APPLICATION_ALREADY_EXISTS", "You have already applied to this job");
    }

    static boolean isConstraint(DataIntegrityViolationException e, String constraintName) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(constraintName);
    }

    public List<ApplicationResponse> getMyApplications() {
        Student student = getCurrentStudent();
        return applicationMapper.toResponses(applicationRepository.findByStudentId(student.getId()));
    }

    public ApplicationResponse getApplicationDetails(Long applicationId) {
        Application application = requireOwnApplication(applicationId);
        ApplicationResponse response = applicationMapper.toResponse(application);
        if (application.getStatus() == ApplicationStatus.WAITLISTED) {
            response.setWaitlistPosition(waitlistPositionOf(application));
        }
        return response;
    }

    private int waitlistPositionOf(Application application) {
        return (int) applicationRepository.countAheadOnWaitlist(
                application.getJob().getId(), application.getWaitlistRank(), application.getId()) + 1;
    }

    /** The current student's application, or 404 (also for other students' applications). */
    Application requireOwnApplication(Long applicationId) {
        Student student = getCurrentStudent();

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        if (!application.getStudent().getId().equals(student.getId())) {
            throw new NotFoundException("Application not found");
        }
        return application;
    }

    private StudentProfileResponse mapToProfileResponse(Student student) {
        StudentProfileResponse response = new StudentProfileResponse();
        response.setId(student.getId());
        response.setEmail(student.getUser().getEmail());
        response.setFullName(student.getFullName());
        response.setPhone(student.getPhone());
        response.setCollege(student.getCollege());
        response.setDegree(student.getDegree());
        response.setBranch(student.getBranch());
        response.setGraduationYear(student.getGraduationYear());
        response.setCgpa(student.getCgpa());
        response.setResumeUrl(student.getResumeUrl());
        response.setSkills(student.getSkills());
        response.setLinkedinUrl(student.getLinkedinUrl());
        response.setGithubUrl(student.getGithubUrl());
        response.setProfileCompleted(student.getProfileCompleted());
        return response;
    }
}
