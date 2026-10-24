package com.placementhub.backend.controller;

import com.placementhub.backend.dto.*;
import com.placementhub.backend.service.InterviewSlotService;
import com.placementhub.backend.service.RecruiterService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.placementhub.backend.config.ApiExamples;
import com.placementhub.backend.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Recruiter", description = "Company profile, job postings, applicants, status changes and interview slots. Requires ROLE_RECRUITER; only the recruiter's own jobs are visible (others return 404).")
@RequestMapping("/api/recruiter")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterController {

    private final RecruiterService recruiterService;
    private final InterviewSlotService interviewSlotService;

    @GetMapping("/profile")
    public ResponseEntity<RecruiterProfileResponse> getProfile() {
        return ResponseEntity.ok(recruiterService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<RecruiterProfileResponse> updateProfile(@Valid @RequestBody RecruiterProfileUpdateRequest request) {
        return ResponseEntity.ok(recruiterService.updateProfile(request));
    }

    @PostMapping("/jobs")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recruiterService.createJob(request));
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

    @Operation(summary = "Move an application to its next status", description = "Allowed: APPLIED->SHORTLISTED->INTERVIEW->OFFERED, or ->REJECTED. Send the version you displayed: 409 APPLICATION_MODIFIED / CONCURRENT_UPDATE if someone changed it first; 422 INVALID_STATUS_TRANSITION.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(name = "Shortlist", value = ApiExamples.STATUS_UPDATE_REQUEST)))
    @ApiResponse(responseCode = "200", description = "Status changed; the timeline has a new entry", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ApplicationResponse.class), examples = @ExampleObject(value = ApiExamples.APPLICATION_RESPONSE)))
    @ApiResponse(responseCode = "404", description = "Not an application for one of your jobs", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class), examples = {@ExampleObject(name = "Not found", value = ApiExamples.NOT_FOUND)}))
    @ApiResponse(responseCode = "409", description = "Someone changed it first", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class), examples = {@ExampleObject(name = "Stale version", value = ApiExamples.APPLICATION_MODIFIED)}))
    @ApiResponse(responseCode = "422", description = "Move not allowed", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class), examples = {@ExampleObject(name = "Invalid transition", value = ApiExamples.INVALID_STATUS_TRANSITION)}))
    @PutMapping("/applications/{id}/status")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(@PathVariable Long id, @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return ResponseEntity.ok(recruiterService.updateApplicationStatus(id, request));
    }

    /** Stop accepting applications for an approved job. */
    @Operation(summary = "Close a job to new applications", description = "Waits for applications already in progress, then refuses new ones (JOB_NOT_OPEN).")
    @PostMapping("/jobs/{id}/close")
    public ResponseEntity<JobResponse> closeJob(@PathVariable Long id) {
        return ResponseEntity.ok(recruiterService.closeJob(id));
    }

    @GetMapping("/jobs/{jobId}/slots")
    public ResponseEntity<List<InterviewSlotResponse>> getSlots(@PathVariable Long jobId) {
        return ResponseEntity.ok(interviewSlotService.getSlotsForRecruiter(jobId));
    }

    @Operation(summary = "Create an interview slot", description = "201 on success. Capacity 1-100; must start in the future (422 SLOT_IN_PAST).")
    @PostMapping("/jobs/{jobId}/slots")
    public ResponseEntity<InterviewSlotResponse> createSlot(@PathVariable Long jobId,
                                                            @Valid @RequestBody InterviewSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(interviewSlotService.createSlot(jobId, request));
    }

    @DeleteMapping("/jobs/{jobId}/slots/{slotId}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long jobId, @PathVariable Long slotId) {
        interviewSlotService.deleteSlot(jobId, slotId);
        return ResponseEntity.noContent().build();
    }
}