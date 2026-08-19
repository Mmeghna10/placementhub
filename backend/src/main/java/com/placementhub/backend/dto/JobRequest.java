package com.placementhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class JobRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String jobType;
    private String location;
    private Integer salaryMin;
    private Integer salaryMax;
    private String requiredSkills;
    private BigDecimal minCgpa;
    private String eligibleBranches;
    private LocalDate applicationDeadline;
}