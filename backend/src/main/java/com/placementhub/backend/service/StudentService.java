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

    public List<JobResponse> getApprovedJobs() {
        return jobRepository.findByStatus("APPROVED").stream()
                .map(jobMapper::toResponse)
                .toList();
    }

    public List<JobResponse> getRecommendedJobs() {
        Student student = getCurrentStudent();
        return jobRepository.findByStatus("APPROVED").stream()
                .filter(job -> matchesStudent(job, student))
                .map(jobMapper::toResponse)
                .toList();
    }

    private boolean matchesStudent(Job job, Student student) {
        boolean cgpaOk = job.getMinCgpa() == null || student.getCgpa() == null
                || student.getCgpa().compareTo(job.getMinCgpa()) >= 0;

        boolean branchOk = job.getEligibleBranches() == null || job.getEligibleBranches().isBlank()
                || student.getBranch() == null
                || job.getEligibleBranches().toLowerCase().contains(student.getBranch().toLowerCase());

        return cgpaOk && branchOk;
    }

    /**
     * Application and its first timeline entry are written in one transaction.
     * The duplicate check below is still racy under concurrent requests; Milestone 4 adds
     * the UNIQUE(student_id, job_id) constraint that closes it.
     */
    @Transactional
    public ApplicationResponse applyToJob(Long jobId) {
        Student student = getCurrentStudent();

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        if (!"APPROVED".equals(job.getStatus())) {
            throw new BusinessRuleException("JOB_NOT_OPEN", "This job is not open for applications");
        }

        if (applicationRepository.existsByStudentIdAndJobId(student.getId(), jobId)) {
            throw new ConflictException("APPLICATION_ALREADY_EXISTS", "You have already applied to this job");
        }

        Application application = new Application();
        application.setStudent(student);
        application.setJob(job);
        application = applicationRepository.save(application);

        ApplicationTimeline timelineEntry = new ApplicationTimeline();
        timelineEntry.setApplication(application);
        timelineEntry.setStatus("APPLIED");
        timelineEntry.setRemarks("Application submitted");
        timelineRepository.save(timelineEntry);

        return applicationMapper.toResponse(application);
    }

    public List<ApplicationResponse> getMyApplications() {
        Student student = getCurrentStudent();
        return applicationMapper.toResponses(applicationRepository.findByStudentId(student.getId()));
    }

    public ApplicationResponse getApplicationDetails(Long applicationId) {
        Student student = getCurrentStudent();

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        if (!application.getStudent().getId().equals(student.getId())) {
            throw new NotFoundException("Application not found");
        }

        return applicationMapper.toResponse(application);
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
