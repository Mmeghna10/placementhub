package com.placementhub.backend.controller;

import com.placementhub.backend.dto.*;
import com.placementhub.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/students")
    public ResponseEntity<PageResponse<StudentSummaryResponse>> getStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllStudents(page, size));
    }

    @GetMapping("/recruiters")
    public ResponseEntity<PageResponse<RecruiterSummaryResponse>> getRecruiters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllRecruiters(page, size));
    }

    @GetMapping("/jobs")
    public ResponseEntity<PageResponse<JobResponse>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllJobs(page, size));
    }

    @GetMapping("/jobs/pending")
    public ResponseEntity<List<JobResponse>> getPendingJobs() {
        return ResponseEntity.ok(adminService.getPendingJobs());
    }

    @PutMapping("/jobs/{jobId}/approve")
    public ResponseEntity<JobResponse> approveJob(@PathVariable Long jobId, @Valid @RequestBody JobApprovalRequest request) {
        return ResponseEntity.ok(adminService.updateJobApproval(jobId, request));
    }

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(adminService.getAnalytics());
    }
}
