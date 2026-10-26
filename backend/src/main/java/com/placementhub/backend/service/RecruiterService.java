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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationTimelineRepository timelineRepository;
    private final JobMapper jobMapper;
    private final ApplicationMapper applicationMapper;
    private final InterviewSlotRepository slotRepository;
    private final OfferService offerService;
    private final WaitlistService waitlistService;
    private final OfferDeskStatsService offerDeskStats;
    private final Clock clock;

    private Recruiter getCurrentRecruiter() {
        String email = SecurityUtils.getCurrentUserEmail();
        return recruiterRepository.findByUser_Email(email)
                .orElseThrow(() -> new NotFoundException("Recruiter profile not found"));
    }

    public RecruiterProfileResponse getProfile() {
        return mapToProfileResponse(getCurrentRecruiter());
    }

    @Transactional
    public RecruiterProfileResponse updateProfile(RecruiterProfileUpdateRequest request) {
        Recruiter recruiter = getCurrentRecruiter();

        recruiter.setFullName(request.getFullName());
        recruiter.setPhone(request.getPhone());
        recruiter.setDesignation(request.getDesignation());

        // The personal-profile page sends company fields too (empty before a company exists).
        // Only create a company once a name is given; never blank out an existing company's name,
        // since companies.name is NOT NULL.
        boolean hasCompanyName = request.getCompanyName() != null && !request.getCompanyName().isBlank();
        Company company = recruiter.getCompany();
        if (company == null && hasCompanyName) {
            company = new Company();
        } else if (company != null && !hasCompanyName) {
            throw new BusinessRuleException("COMPANY_NAME_REQUIRED", "Company name is required");
        }

        if (company != null) {
            company.setName(request.getCompanyName().trim());
            company.setDescription(request.getCompanyDescription());
            company.setWebsite(request.getCompanyWebsite());
            company.setIndustry(request.getCompanyIndustry());
            company.setLocation(request.getCompanyLocation());
            company = companyRepository.save(company);
            recruiter.setCompany(company);
        }

        recruiter = recruiterRepository.save(recruiter);

        return mapToProfileResponse(recruiter);
    }

    @Transactional
    public JobResponse createJob(JobRequest request) {
        Recruiter recruiter = getCurrentRecruiter();

        if (recruiter.getCompany() == null) {
            throw new BusinessRuleException("COMPANY_PROFILE_REQUIRED", "Please complete your company profile before posting a job");
        }

        Job job = new Job();
        applyJobRequest(job, request);
        job.setRecruiter(recruiter);
        job.setCompany(recruiter.getCompany());
        job.setStatus(JobStatus.PENDING);

        job = jobRepository.save(job);
        return jobMapper.toResponse(job);
    }

    public List<JobResponse> getMyJobs() {
        Recruiter recruiter = getCurrentRecruiter();
        return jobRepository.findByRecruiterId(recruiter.getId()).stream()
                .map(jobMapper::toResponse)
                .toList();
    }

    @Transactional
    public JobResponse updateJob(Long jobId, JobRequest request) {
        Recruiter recruiter = getCurrentRecruiter();
        Job job = getOwnedJob(jobId, recruiter);

        applyJobRequest(job, request);
        // Any edit sends the job back for admin review, so approved content cannot be
        // changed after moderation. The UPDATE waits for in-flight applications holding the
        // job's shared lock (see StudentService.applyToJob) and @Version rejects an edit
        // that raced with another edit or an approval.
        job.setStatus(JobStatus.PENDING);
        job = jobRepository.saveAndFlush(job);
        return jobMapper.toResponse(job);
    }

    /** The recruiter's Offer Desk: only their own jobs (the id comes from the login). */
    public OfferDeskResponse getOfferDesk() {
        return offerDeskStats.offerDesk(getCurrentRecruiter().getId());
    }

    /** Offers every free opening of this job to the waitlist, in rank order. */
    @Transactional
    public List<ApplicationResponse> fillFromWaitlist(Long jobId) {
        Job job = getOwnedJob(jobId, getCurrentRecruiter());
        waitlistService.fillOpenings(job);
        return applicationMapper.toRecruiterResponses(applicationRepository.findByJobIdOrderByAppliedAtAsc(job.getId()));
    }

    /** Saves a new waitlist order (application ids, next in line first). */
    @Transactional
    public List<ApplicationResponse> reorderWaitlist(Long jobId, List<Long> applicationIds) {
        Job job = getOwnedJob(jobId, getCurrentRecruiter());
        waitlistService.reorder(job, applicationIds);
        return applicationMapper.toRecruiterResponses(applicationRepository.findByJobIdOrderByAppliedAtAsc(job.getId()));
    }

    /**
     * Stops accepting applications. Existing applications and interview slots continue.
     * Like an edit, the UPDATE waits for applications already in progress to commit; any
     * application that starts afterwards is refused because the job is no longer APPROVED.
     */
    @Transactional
    public JobResponse closeJob(Long jobId) {
        Job job = requireOwnedJob(jobId);
        if (job.getStatus() == JobStatus.CLOSED) {
            throw new BusinessRuleException("JOB_ALREADY_CLOSED", "This job is already closed");
        }
        if (job.getStatus() != JobStatus.APPROVED) {
            throw new BusinessRuleException("JOB_NOT_OPEN", "Only an approved (open) job can be closed");
        }
        job.setStatus(JobStatus.CLOSED);
        job = jobRepository.saveAndFlush(job);
        return jobMapper.toResponse(job);
    }

    @Transactional
    public void deleteJob(Long jobId) {
        Recruiter recruiter = getCurrentRecruiter();
        Job job = getOwnedJob(jobId, recruiter);
        if (applicationRepository.existsByJobId(job.getId())) {
            throw new ConflictException("JOB_HAS_APPLICATIONS",
                    "This job already has applications and cannot be deleted");
        }
        // No applications means no bookings, so any interview slots can go with the job.
        slotRepository.deleteByJobId(job.getId());
        jobRepository.delete(job);
    }

    public List<ApplicationResponse> getApplicantsForJob(Long jobId) {
        Recruiter recruiter = getCurrentRecruiter();
        Job job = getOwnedJob(jobId, recruiter);

        return applicationMapper.toRecruiterResponses(
                applicationRepository.findByJobIdOrderByAppliedAtAsc(job.getId()));
    }

    /**
     * Moves an application to its next status. Three safeguards against conflicting updates:
     *
     * 1. State machine: only transitions allowed by ApplicationStatus (e.g. a REJECTED
     *    application cannot be OFFERED later).
     * 2. Client version (optional): if the recruiter's screen showed an older version than
     *    the current one, the change is refused with 409 instead of overwriting.
     * 3. @Version: if two transactions update the same row at the same time, the second
     *    UPDATE matches no row (version already incremented) and fails with 409.
     *
     * The status change and its timeline entry commit together or not at all.
     */
    @Transactional
    public ApplicationResponse updateApplicationStatus(Long applicationId, ApplicationStatusUpdateRequest request) {
        Recruiter recruiter = getCurrentRecruiter();

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new NotFoundException("Application not found");
        }

        if (request.getVersion() != null && !request.getVersion().equals(application.getVersion())) {
            throw new ConflictException("APPLICATION_MODIFIED",
                    "This application was updated by someone else. Refresh and try again.");
        }

        ApplicationStatus current = application.getStatus();
        ApplicationStatus next = ApplicationStatus.valueOf(request.getStatus());
        if (!current.canMoveTo(next)) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "An application cannot move from " + current + " to " + next
                            + (current.isFinal() ? " (" + current + " is final)" : ""));
        }

        if (next == ApplicationStatus.OFFERED) {
            // Takes an opening and the student's one pending-offer place, or refuses.
            offerService.prepareOffer(application);
        }
        if (next == ApplicationStatus.WAITLISTED) {
            waitlistService.addToWaitlist(application);
        } else {
            application.setWaitlistRank(null);
        }

        application.setStatus(next);
        application.setUpdatedAt(LocalDateTime.now(clock));
        application = applicationRepository.saveAndFlush(application);

        ApplicationTimeline entry = new ApplicationTimeline();
        entry.setApplication(application);
        entry.setStatus(next.name());
        entry.setRemarks(request.getRemarks());
        timelineRepository.save(entry);

        return applicationMapper.toRecruiterResponse(application);
    }

    /** The current recruiter's job, or 404 (also for other recruiters' jobs). */
    Job requireOwnedJob(Long jobId) {
        return getOwnedJob(jobId, getCurrentRecruiter());
    }

    private Job getOwnedJob(Long jobId, Recruiter recruiter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new NotFoundException("Job not found");
        }
        return job;
    }

    private void applyJobRequest(Job job, JobRequest request) {
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setJobType(request.getJobType());
        job.setLocation(request.getLocation());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setMinCgpa(request.getMinCgpa());
        job.setEligibleBranches(request.getEligibleBranches());
        job.setApplicationDeadline(request.getApplicationDeadline());
        if (request.getOpenings() != null) {
            int committed = job.getOffersPending() + job.getOffersAccepted();
            if (request.getOpenings() < committed) {
                throw new BusinessRuleException("OPENINGS_BELOW_OFFERS",
                        "This job already has " + committed + " offer" + (committed == 1 ? "" : "s")
                                + " made or accepted, so it needs at least that many openings.");
            }
            job.setOpenings(request.getOpenings());
        }
        if (request.getOfferDeadlineHours() != null) {
            job.setOfferDeadlineHours(request.getOfferDeadlineHours());
        }
    }

    private RecruiterProfileResponse mapToProfileResponse(Recruiter recruiter) {
        RecruiterProfileResponse response = new RecruiterProfileResponse();
        response.setId(recruiter.getId());
        response.setEmail(recruiter.getUser().getEmail());
        response.setFullName(recruiter.getFullName());
        response.setPhone(recruiter.getPhone());
        response.setDesignation(recruiter.getDesignation());

        if (recruiter.getCompany() != null) {
            response.setCompanyId(recruiter.getCompany().getId());
            response.setCompanyName(recruiter.getCompany().getName());
            response.setCompanyDescription(recruiter.getCompany().getDescription());
            response.setCompanyWebsite(recruiter.getCompany().getWebsite());
            response.setCompanyIndustry(recruiter.getCompany().getIndustry());
            response.setCompanyLocation(recruiter.getCompany().getLocation());
        }

        return response;
    }
}
