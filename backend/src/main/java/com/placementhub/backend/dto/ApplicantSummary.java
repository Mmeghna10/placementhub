package com.placementhub.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

/** The applicant's profile as shown to the recruiter who owns the job (no contact phone). */
@Data
public class ApplicantSummary {
    private Long studentId;
    private String fullName;
    private String email;
    private String college;
    private String degree;
    private String branch;
    private Integer graduationYear;
    private BigDecimal cgpa;
    private String skills;
    private String resumeUrl;
    private String linkedinUrl;
    private String githubUrl;
}
