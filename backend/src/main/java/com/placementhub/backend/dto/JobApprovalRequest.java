package com.placementhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class JobApprovalRequest {
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "APPROVED|REJECTED", message = "Status must be APPROVED or REJECTED")
    private String status;

    /** Placement tier for an approved job (default REGULAR). */
    @Pattern(regexp = "REGULAR|DREAM|SUPER_DREAM", message = "Tier must be REGULAR, DREAM or SUPER_DREAM")
    private String tier;
}
