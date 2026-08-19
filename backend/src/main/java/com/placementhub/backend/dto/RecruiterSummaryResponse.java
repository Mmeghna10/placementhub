package com.placementhub.backend.dto;

import lombok.Data;

@Data
public class RecruiterSummaryResponse {
    private Long id;
    private String email;
    private String fullName;
    private String companyName;
    private String designation;
}