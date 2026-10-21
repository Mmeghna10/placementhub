package com.placementhub.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class StudentProfileUpdateRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must be at most 150 characters")
    private String fullName;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Size(max = 150, message = "College must be at most 150 characters")
    private String college;

    @Size(max = 100, message = "Degree must be at most 100 characters")
    private String degree;

    @Size(max = 100, message = "Branch must be at most 100 characters")
    private String branch;

    @Min(value = 1950, message = "Graduation year looks invalid")
    @Max(value = 2100, message = "Graduation year looks invalid")
    private Integer graduationYear;

    @DecimalMin(value = "0.0", message = "CGPA must be between 0 and 10")
    @DecimalMax(value = "10.0", message = "CGPA must be between 0 and 10")
    private BigDecimal cgpa;

    @Size(max = 255, message = "Resume URL must be at most 255 characters")
    private String resumeUrl;

    @Size(max = 2000, message = "Skills must be at most 2000 characters")
    private String skills;

    @Size(max = 255, message = "LinkedIn URL must be at most 255 characters")
    private String linkedinUrl;

    @Size(max = 255, message = "GitHub URL must be at most 255 characters")
    private String githubUrl;
}
