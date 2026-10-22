package com.placementhub.backend.controller;

import jakarta.validation.Valid;
import com.placementhub.backend.dto.*;
import com.placementhub.backend.service.InterviewSlotService;
import com.placementhub.backend.service.StudentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Student", description = "Profile, job search, applications and interview booking. Requires ROLE_STUDENT.")
@RequestMapping("/api/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final StudentService studentService;
    private final InterviewSlotService interviewSlotService;

    @GetMapping("/profile")
    public ResponseEntity<StudentProfileResponse> getProfile() {
        return ResponseEntity.ok(studentService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<StudentProfileResponse> updateProfile(@Valid @RequestBody StudentProfileUpdateRequest request) {
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

    /**
     * Search open jobs with optional filters, one page at a time.
     * sort: newest (default), deadline (closing soonest first) or salary (highest first).
     */
    @Operation(summary = "Search open jobs (paginated)", description = "Filters are optional. q matches title, company or skills; eligibleOnly applies the same CGPA/branch rules as applying. sort = newest | deadline | salary. size is capped at 50.")
    @GetMapping("/jobs/search")
    public ResponseEntity<PageResponse<JobResponse>> searchJobs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "false") boolean eligibleOnly,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(studentService.searchJobs(q, jobType, location, eligibleOnly, sort, page, size));
    }

    @Operation(summary = "Get one job", description = "Open jobs, or closed jobs the student applied to; otherwise 404. acceptingApplications tells the UI whether to show Apply.")
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobResponse> getJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(studentService.getJob(jobId));
    }

    @Operation(summary = "Apply to a job", description = "201 on success. 409 APPLICATION_ALREADY_EXISTS (also under simultaneous clicks), 422 JOB_NOT_OPEN / DEADLINE_PASSED / NOT_ELIGIBLE.")
    @PostMapping("/jobs/{jobId}/apply")
    public ResponseEntity<ApplicationResponse> applyToJob(@PathVariable Long jobId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.applyToJob(jobId));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications() {
        return ResponseEntity.ok(studentService.getMyApplications());
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationDetails(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getApplicationDetails(id));
    }

    /** Upcoming interview slots for this application's job (INTERVIEW stage only). */
    @GetMapping("/applications/{id}/slots")
    public ResponseEntity<List<InterviewSlotResponse>> getInterviewSlots(@PathVariable Long id) {
        return ResponseEntity.ok(interviewSlotService.getSlotsForStudent(id));
    }

    @Operation(summary = "Book an interview slot", description = "Application must be in INTERVIEW. 201 on success; 409 SLOT_FULL or ALREADY_BOOKED; 422 NOT_IN_INTERVIEW_STAGE / SLOT_IN_PAST.")
    @PostMapping("/applications/{id}/slots/{slotId}/booking")
    public ResponseEntity<ApplicationResponse> bookInterviewSlot(@PathVariable Long id, @PathVariable Long slotId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(interviewSlotService.bookSlot(id, slotId));
    }

    @DeleteMapping("/applications/{id}/booking")
    public ResponseEntity<ApplicationResponse> cancelInterviewBooking(@PathVariable Long id) {
        return ResponseEntity.ok(interviewSlotService.cancelBooking(id));
    }
}
