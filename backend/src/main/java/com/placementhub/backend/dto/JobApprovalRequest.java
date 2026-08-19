package com.placementhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobApprovalRequest {
    @NotBlank(message = "Status is required")
    private String status;
}