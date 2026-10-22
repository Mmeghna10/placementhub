package com.placementhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplicationStatusUpdateRequest {
    // Restricts the allowed values only; which transitions are legal is enforced in Milestone 4.
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "APPLIED|SHORTLISTED|INTERVIEW|OFFERED|REJECTED",
            message = "Status must be one of APPLIED, SHORTLISTED, INTERVIEW, OFFERED, REJECTED")
    private String status;

    @Size(max = 255, message = "Remarks must be at most 255 characters")
    private String remarks;
}
