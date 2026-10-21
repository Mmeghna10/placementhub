package com.placementhub.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class JobRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @Size(max = 10000, message = "Description must be at most 10000 characters")
    private String description;

    @Size(max = 20, message = "Job type must be at most 20 characters")
    private String jobType;

    @Size(max = 150, message = "Location must be at most 150 characters")
    private String location;

    @PositiveOrZero(message = "Minimum salary cannot be negative")
    private Integer salaryMin;

    @PositiveOrZero(message = "Maximum salary cannot be negative")
    private Integer salaryMax;

    @Size(max = 2000, message = "Required skills must be at most 2000 characters")
    private String requiredSkills;

    @DecimalMin(value = "0.0", message = "Minimum CGPA must be between 0 and 10")
    @DecimalMax(value = "10.0", message = "Minimum CGPA must be between 0 and 10")
    private BigDecimal minCgpa;

    @Size(max = 2000, message = "Eligible branches must be at most 2000 characters")
    private String eligibleBranches;

    private LocalDate applicationDeadline;

    @JsonIgnore
    @AssertTrue(message = "Minimum salary cannot be greater than maximum salary")
    public boolean isSalaryRangeValid() {
        return salaryMin == null || salaryMax == null || salaryMin <= salaryMax;
    }
}
