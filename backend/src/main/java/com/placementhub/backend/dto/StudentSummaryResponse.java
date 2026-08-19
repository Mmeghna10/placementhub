package com.placementhub.backend.dto;

import lombok.Data;

@Data
public class StudentSummaryResponse {
    private Long id;
    private String email;
    private String fullName;
    private String college;
    private String branch;
    private Integer graduationYear;
}