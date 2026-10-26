package com.placementhub.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** A job's waitlisted application ids, next in line first. */
@Data
public class WaitlistOrderRequest {

    @NotNull(message = "applicationIds is required")
    private List<Long> applicationIds;
}
