package com.placementhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplicationStatusUpdateRequest {
    // Only known values pass validation; whether the move is allowed from the current
    // status is checked by ApplicationStatus.canMoveTo in the service.
    @NotBlank(message = "Status is required")
    // Statuses a recruiter can set. ACCEPTED/DECLINED are the student's, EXPIRED the deadline's.
    @Pattern(regexp = "APPLIED|SHORTLISTED|INTERVIEW|WAITLISTED|OFFERED|REJECTED",
            message = "Status must be one of APPLIED, SHORTLISTED, INTERVIEW, WAITLISTED, OFFERED, REJECTED")
    private String status;

    @Size(max = 255, message = "Remarks must be at most 255 characters")
    private String remarks;

    /**
     * The application version the client last saw. Optional for older clients; when sent,
     * a different current version is rejected with 409 instead of overwriting.
     */
    private Long version;
}
