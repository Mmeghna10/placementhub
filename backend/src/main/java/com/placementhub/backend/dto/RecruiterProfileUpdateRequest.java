package com.placementhub.backend.dto;

import lombok.Data;

@Data
public class RecruiterProfileUpdateRequest {
    private String fullName;
    private String phone;
    private String designation;
    private String companyName;
    private String companyDescription;
    private String companyWebsite;
    private String companyIndustry;
    private String companyLocation;
}