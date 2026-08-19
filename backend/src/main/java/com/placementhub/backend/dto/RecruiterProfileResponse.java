package com.placementhub.backend.dto;

import lombok.Data;

@Data
public class RecruiterProfileResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String designation;
    private Long companyId;
    private String companyName;
    private String companyDescription;
    private String companyWebsite;
    private String companyIndustry;
    private String companyLocation;
}