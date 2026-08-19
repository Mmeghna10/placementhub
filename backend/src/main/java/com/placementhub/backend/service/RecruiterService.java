package com.placementhub.backend.service;

import com.placementhub.backend.dto.*;
import com.placementhub.backend.entity.*;
import com.placementhub.backend.repository.*;
import com.placementhub.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationTimelineRepository timelineRepository;

    private Recruiter getCurrentRecruiter() {
        String email = SecurityUtils.getCurrentUserEmail();
        return recruiterRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Recruiter profile not found"));
    }

    public RecruiterProfileResponse getProfile() {
        return mapToProfileResponse(getCurrentRecruiter());
    }

    public RecruiterProfileResponse updateProfile(RecruiterProfileUpdateRequest request) {
        Recruiter recruiter = getCurrentRecruiter();

        recruiter.setFullName(request.getFullName());
        recruiter.setPhone(request.getPhone());
        recruiter.setDesignation(request.getDesignation());

        Company company = recruiter.getCompany();
        if (company == null) {
            company = new Company();
        }
        company.setName(request.getCompanyName());
        company.setDescription(request.getCompanyDescription());
        company.setWebsite(request.getCompanyWebsite());
        company.setIndustry(request.getCompanyIndustry());
        company.setLocation(request.getCompanyLocation());
        company = companyRepository.save(company);

        recruiter.setCompany(company);
        recruiter = recruiterRepository.save(recruiter);

        return mapToProfileResponse(recruiter);
    }

    public JobResponse createJob(JobRequest request) {
        Recruiter recruiter = getCurrentRecruiter();

        if (recruiter.getCompany() == null) {
            throw new RuntimeException("Please complete your company profile before posting a job");
        }

        Job job = new Job();
        applyJobRequest(job, request);
        job.setRecruiter(recruiter);
        job.setCompany(recruiter.getCompany());
        job.setStatus("PENDING");

        job = jobRepository.save(job);
        return mapToJobResponse(job);
    }

    public List<JobResponse> getMyJobs() {
        Recruiter recruiter = getCurrentRecruiter();
        return jobRepository.findByRecruiterId(recruiter.getId()).stream()
                .map(this::mapToJobResponse)
                .collect(Collectors.toList());
    }

    public JobResponse updateJob(Long jobId, JobRequest request) {
        Recruiter recruiter = getCurrentRecruiter();
        Job job = getOwnedJob(jobId, recruiter);

        applyJobRequest(job, request);
        job = jobRepository.save(job);
        return mapToJobResponse(job);
    }

    public void deleteJob(Long jobId) {
        Recruiter recruiter = getCurrentRecruiter();
        Job job = getOwnedJob(jobId, recruiter);
        jobRepository.delete(job);
    }

    public List<ApplicationResponse> getApplicantsForJob(Long jobId) {
        Recruiter recruiter = getCurrentRecruiter();
        Job job = getOwnedJob(jobId, recruiter);

        return applicationRepository.findByJobId(job.getId()).stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
    }

    public ApplicationResponse updateApplicationStatus(Long applicationId, ApplicationStatusUpdateRequest request) {
        Recruiter recruiter = getCurrentRecruiter();

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("You do not have access to this application");
        }

        application.setStatus(request.getStatus());
        applicationRepository.save(application);

        ApplicationTimeline entry = new ApplicationTimeline();
        entry.setApplication(application);
        entry.setStatus(request.getStatus());
        entry.setRemarks(request.getRemarks());
        timelineRepository.save(entry);

        return mapToApplicationResponse(application);
    }

    private Job getOwnedJob(Long jobId, Recruiter recruiter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("You do not have access to this job");
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

    private JobResponse mapToJobResponse(Job job) {
        JobResponse response = new JobResponse();
        response.setId(job.getId());
        response.setTitle(job.getTitle());
        response.setDescription(job.getDescription());
        response.setJobType(job.getJobType());
        response.setLocation(job.getLocation());
        response.setSalaryMin(job.getSalaryMin());
        response.setSalaryMax(job.getSalaryMax());
        response.setRequiredSkills(job.getRequiredSkills());
        response.setMinCgpa(job.getMinCgpa());
        response.setEligibleBranches(job.getEligibleBranches());
        response.setApplicationDeadline(job.getApplicationDeadline());
        response.setStatus(job.getStatus());
        response.setCompanyName(job.getCompany().getName());
        response.setCompanyId(job.getCompany().getId());
        response.setRecruiterId(job.getRecruiter().getId());
        response.setPostedAt(job.getPostedAt());
        return response;
    }

    private ApplicationResponse mapToApplicationResponse(Application application) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId());
        response.setJobId(application.getJob().getId());
        response.setJobTitle(application.getJob().getTitle());
        response.setCompanyName(application.getJob().getCompany().getName());
        response.setStatus(application.getStatus());
        response.setAppliedAt(application.getAppliedAt());
        response.setUpdatedAt(application.getUpdatedAt());

        List<TimelineEntryResponse> timeline = timelineRepository
                .findByApplicationIdOrderByChangedAtAsc(application.getId())
                .stream()
                .map(t -> {
                    TimelineEntryResponse te = new TimelineEntryResponse();
                    te.setStatus(t.getStatus());
                    te.setRemarks(t.getRemarks());
                    te.setChangedAt(t.getChangedAt());
                    return te;
                })
                .collect(Collectors.toList());
        response.setTimeline(timeline);

        return response;
    }
}