package com.placementhub.backend.service;

import com.placementhub.backend.exception.*;
import com.placementhub.backend.dto.*;
import com.placementhub.backend.entity.*;
import com.placementhub.backend.mapper.ApplicationMapper;
import com.placementhub.backend.mapper.JobMapper;
import com.placementhub.backend.repository.*;
import com.placementhub.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        job.setStatus("PENDING");

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
        // changed after moderation.
        job.setStatus("PENDING");
        job = jobRepository.save(job);
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
        jobRepository.delete(job);
    }

    public List<ApplicationResponse> getApplicantsForJob(Long jobId) {
        Recruiter recruiter = getCurrentRecruiter();
        Job job = getOwnedJob(jobId, recruiter);

        return applicationMapper.toResponses(applicationRepository.findByJobId(job.getId()));
    }

    /** Status change and its timeline entry commit together (or not at all). */
    @Transactional
    public ApplicationResponse updateApplicationStatus(Long applicationId, ApplicationStatusUpdateRequest request) {
        Recruiter recruiter = getCurrentRecruiter();

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new NotFoundException("Application not found");
        }

        application.setStatus(request.getStatus());
        application.setUpdatedAt(java.time.LocalDateTime.now());
        applicationRepository.save(application);

        ApplicationTimeline entry = new ApplicationTimeline();
        entry.setApplication(application);
        entry.setStatus(request.getStatus());
        entry.setRemarks(request.getRemarks());
        timelineRepository.save(entry);

        return applicationMapper.toResponse(application);
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
