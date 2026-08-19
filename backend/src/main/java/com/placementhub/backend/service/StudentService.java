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
public class StudentService {

    private final StudentRepository studentRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationTimelineRepository timelineRepository;

    private Student getCurrentStudent() {
        String email = SecurityUtils.getCurrentUserEmail();
        return studentRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));
    }

    public StudentProfileResponse getProfile() {
        return mapToProfileResponse(getCurrentStudent());
    }

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
                .map(this::mapToJobResponse)
                .collect(Collectors.toList());
    }

    public List<JobResponse> getRecommendedJobs() {
        Student student = getCurrentStudent();
        return jobRepository.findByStatus("APPROVED").stream()
                .filter(job -> matchesStudent(job, student))
                .map(this::mapToJobResponse)
                .collect(Collectors.toList());
    }

    private boolean matchesStudent(Job job, Student student) {
        boolean cgpaOk = job.getMinCgpa() == null || student.getCgpa() == null
                || student.getCgpa().compareTo(job.getMinCgpa()) >= 0;

        boolean branchOk = job.getEligibleBranches() == null || job.getEligibleBranches().isBlank()
                || student.getBranch() == null
                || job.getEligibleBranches().toLowerCase().contains(student.getBranch().toLowerCase());

        return cgpaOk && branchOk;
    }

    public ApplicationResponse applyToJob(Long jobId) {
        Student student = getCurrentStudent();

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!"APPROVED".equals(job.getStatus())) {
            throw new RuntimeException("This job is not open for applications");
        }

        if (applicationRepository.existsByStudentIdAndJobId(student.getId(), jobId)) {
            throw new RuntimeException("You have already applied to this job");
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

        return mapToApplicationResponse(application);
    }

    public List<ApplicationResponse> getMyApplications() {
        Student student = getCurrentStudent();
        return applicationRepository.findByStudentId(student.getId()).stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
    }

    public ApplicationResponse getApplicationDetails(Long applicationId) {
        Student student = getCurrentStudent();

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!application.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("You do not have access to this application");
        }

        return mapToApplicationResponse(application);
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