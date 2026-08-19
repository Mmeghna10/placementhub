package com.placementhub.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class JobResponse {
    private Long id;
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
    private String status;
    private String companyName;
    private Long companyId;
    private Long recruiterId;
    private LocalDateTime postedAt;
}