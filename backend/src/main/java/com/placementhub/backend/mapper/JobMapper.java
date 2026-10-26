package com.placementhub.backend.mapper;

import com.placementhub.backend.dto.JobResponse;
import com.placementhub.backend.entity.Job;
import org.springframework.stereotype.Component;

/**
 * Job entity → API response. Reads job.company (fetch it with the job to avoid one
 * query per job) and only the id of job.recruiter, which needs no extra query.
 */
@Component
public class JobMapper {

    public JobResponse toResponse(Job job) {
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
        response.setStatus(job.getStatus().name());
        response.setCompanyName(job.getCompany().getName());
        response.setCompanyId(job.getCompany().getId());
        response.setRecruiterId(job.getRecruiter().getId());
        response.setPostedAt(job.getPostedAt());
        response.setOpenings(job.getOpenings());
        response.setOfferDeadlineHours(job.getOfferDeadlineHours());
        response.setOffersPending(job.getOffersPending());
        response.setOffersAccepted(job.getOffersAccepted());
        response.setTier(job.getTier().name());
        return response;
    }
}
