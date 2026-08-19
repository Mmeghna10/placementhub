package com.placementhub.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StudentProfileResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String college;
    private String degree;
    private String branch;
    private Integer graduationYear;
    private BigDecimal cgpa;
    private String resumeUrl;
    private String skills;
    private String linkedinUrl;
    private String githubUrl;
    private Boolean profileCompleted;
}