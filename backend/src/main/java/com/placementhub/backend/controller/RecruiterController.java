package com.placementhub.backend.controller;

import com.placementhub.backend.dto.*;
import com.placementhub.backend.service.RecruiterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recruiter")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterController {

    private final RecruiterService recruiterService;

    @GetMapping("/profile")
    public ResponseEntity<RecruiterProfileResponse> getProfile() {
        return ResponseEntity.ok(recruiterService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<RecruiterProfileResponse> updateProfile(@RequestBody RecruiterProfileUpdateRequest request) {
        return ResponseEntity.ok(recruiterService.updateProfile(request));
    }

    @PostMapping("/jobs")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) {
        return ResponseEntity.ok(recruiterService.createJob(request));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<JobResponse>> getMyJobs() {
        return ResponseEntity.ok(recruiterService.getMyJobs());
    }

    @PutMapping("/jobs/{id}")
    public ResponseEntity<JobResponse> updateJob(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        return ResponseEntity.ok(recruiterService.updateJob(id, request));
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        recruiterService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<List<ApplicationResponse>> getApplicants(@PathVariable Long jobId) {
        return ResponseEntity.ok(recruiterService.getApplicantsForJob(jobId));
    }

    @PutMapping("/applications/{id}/status")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(@PathVariable Long id, @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return ResponseEntity.ok(recruiterService.updateApplicationStatus(id, request));
    }
}