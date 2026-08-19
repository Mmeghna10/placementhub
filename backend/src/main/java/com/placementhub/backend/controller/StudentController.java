package com.placementhub.backend.controller;

import com.placementhub.backend.dto.*;
import com.placementhub.backend.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/profile")
    public ResponseEntity<StudentProfileResponse> getProfile() {
        return ResponseEntity.ok(studentService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<StudentProfileResponse> updateProfile(@RequestBody StudentProfileUpdateRequest request) {
        return ResponseEntity.ok(studentService.updateProfile(request));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<JobResponse>> getJobs() {
        return ResponseEntity.ok(studentService.getApprovedJobs());
    }

    @GetMapping("/jobs/recommended")
    public ResponseEntity<List<JobResponse>> getRecommendedJobs() {
        return ResponseEntity.ok(studentService.getRecommendedJobs());
    }

    @PostMapping("/jobs/{jobId}/apply")
    public ResponseEntity<ApplicationResponse> applyToJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(studentService.applyToJob(jobId));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications() {
        return ResponseEntity.ok(studentService.getMyApplications());
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationDetails(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getApplicationDetails(id));
    }
}