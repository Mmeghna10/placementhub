package com.placementhub.backend.service;

import com.placementhub.backend.exception.*;
import com.placementhub.backend.dto.*;
import com.placementhub.backend.entity.*;
import com.placementhub.backend.mapper.JobMapper;
import com.placementhub.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    static final int MAX_PAGE_SIZE = 100;

    private final StudentRepository studentRepository;
    private final RecruiterRepository recruiterRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final JobMapper jobMapper;

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

    public PageResponse<StudentSummaryResponse> getAllStudents(int page, int size) {
        return PageResponse.from(studentRepository.findAll(newestFirst(page, size))
                .map(this::mapToStudentSummary));
    }

    public PageResponse<RecruiterSummaryResponse> getAllRecruiters(int page, int size) {
        return PageResponse.from(recruiterRepository.findAll(newestFirst(page, size))
                .map(this::mapToRecruiterSummary));
    }

    public PageResponse<JobResponse> getAllJobs(int page, int size) {
        return PageResponse.from(jobRepository.findAll(newestFirst(page, size))
                .map(jobMapper::toResponse));
    }

    /** Clamps client-supplied paging so a request cannot ask for an unbounded page. */
    private Pageable newestFirst(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by("id").descending());
    }

    public List<JobResponse> getPendingJobs() {
        return jobRepository.findByStatus("PENDING").stream()
                .map(jobMapper::toResponse)
                .toList();
    }



    @Transactional
    public JobResponse updateJobApproval(Long jobId, JobApprovalRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        job.setStatus(request.getStatus());
        job = jobRepository.save(job);

        return jobMapper.toResponse(job);
    }

    public AnalyticsResponse getAnalytics() {
        List<String> statuses = Arrays.asList("APPLIED", "SHORTLISTED", "INTERVIEW", "OFFERED", "REJECTED");

        List<ApplicationsByStatusResponse> byStatus = statuses.stream()
                .map(status -> new ApplicationsByStatusResponse(status, applicationRepository.countByStatus(status)))
                .toList();

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
}
