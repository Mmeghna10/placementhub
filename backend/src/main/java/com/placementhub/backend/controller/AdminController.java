package com.placementhub.backend.controller;

import com.placementhub.backend.dto.*;
import com.placementhub.backend.service.AdminService;
import com.placementhub.backend.service.PlacementRules;
import com.placementhub.backend.entity.PlacementPolicy;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Admin", description = "Dashboard, user lists, job approval and analytics. Requires ROLE_ADMIN.")
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final PlacementRules placementRules;

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

    /** The campus placement rules (tiers and upgrades). */
    @GetMapping("/placement-policy")
    public ResponseEntity<PlacementPolicyDto> getPlacementPolicy() {
        return ResponseEntity.ok(toDto(placementRules.policy()));
    }

    @PutMapping("/placement-policy")
    public ResponseEntity<PlacementPolicyDto> updatePlacementPolicy(@Valid @RequestBody PlacementPolicyDto request) {
        return ResponseEntity.ok(toDto(placementRules.update(request)));
    }

    private static PlacementPolicyDto toDto(PlacementPolicy policy) {
        PlacementPolicyDto dto = new PlacementPolicyDto();
        dto.setAllowUpgrades(policy.getAllowUpgrades());
        dto.setMaxAcceptedOffers(policy.getMaxAcceptedOffers());
        return dto;
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
