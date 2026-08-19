package com.placementhub.backend.service;

import com.placementhub.backend.dto.*;
import com.placementhub.backend.entity.*;
import com.placementhub.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final StudentRepository studentRepository;
    private final RecruiterRepository recruiterRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
                studentRepository.count(),
                recruiterRepository.count(),
                companyRepository.count(),
                jobRepository.count(),
                jobRepository.countByStatus("PENDING"),
                applicationRepository.count()
        );
    }

    public List<StudentSummaryResponse> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(this::mapToStudentSummary)
                .collect(Collectors.toList());
    }

    public List<RecruiterSummaryResponse> getAllRecruiters() {
        return recruiterRepository.findAll().stream()
                .map(this::mapToRecruiterSummary)
                .collect(Collectors.toList());
    }

    public List<JobResponse> getAllJobs() {
        return jobRepository.findAll().stream()
                .map(this::mapToJobResponse)
                .collect(Collectors.toList());
    }

    public List<JobResponse> getPendingJobs() {
        return jobRepository.findByStatus("PENDING").stream()
                .map(this::mapToJobResponse)
                .collect(Collectors.toList());
    }

    public JobResponse updateJobApproval(Long jobId, JobApprovalRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        job.setStatus(request.getStatus());
        job = jobRepository.save(job);

        return mapToJobResponse(job);
    }

    public AnalyticsResponse getAnalytics() {
        List<String> statuses = Arrays.asList("APPLIED", "SHORTLISTED", "INTERVIEW", "OFFERED", "REJECTED");

        List<ApplicationsByStatusResponse> byStatus = statuses.stream()
                .map(status -> new ApplicationsByStatusResponse(status, applicationRepository.countByStatus(status)))
                .collect(Collectors.toList());

        return new AnalyticsResponse(
                byStatus,
                jobRepository.count(),
                jobRepository.countByStatus("APPROVED"),
                jobRepository.countByStatus("REJECTED")
        );
    }

    private StudentSummaryResponse mapToStudentSummary(Student student) {
        StudentSummaryResponse response = new StudentSummaryResponse();
        response.setId(student.getId());
        response.setEmail(student.getUser().getEmail());
        response.setFullName(student.getFullName());
        response.setCollege(student.getCollege());
        response.setBranch(student.getBranch());
        response.setGraduationYear(student.getGraduationYear());
        return response;
    }

    private RecruiterSummaryResponse mapToRecruiterSummary(Recruiter recruiter) {
        RecruiterSummaryResponse response = new RecruiterSummaryResponse();
        response.setId(recruiter.getId());
        response.setEmail(recruiter.getUser().getEmail());
        response.setFullName(recruiter.getFullName());
        response.setCompanyName(recruiter.getCompany() != null ? recruiter.getCompany().getName() : null);
        response.setDesignation(recruiter.getDesignation());
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
}