package com.placementhub.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecruiterProfileUpdateRequest {

    @Size(max = 150, message = "Full name must be at most 150 characters")
    private String fullName;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Size(max = 100, message = "Designation must be at most 100 characters")
    private String designation;

    // Optional: the personal-profile page sends this as null before a company exists.
    @Size(max = 150, message = "Company name must be at most 150 characters")
    private String companyName;

    @Size(max = 10000, message = "Company description must be at most 10000 characters")
    private String companyDescription;

    @Size(max = 255, message = "Company website must be at most 255 characters")
    private String companyWebsite;

    @Size(max = 100, message = "Industry must be at most 100 characters")
    private String companyIndustry;

    @Size(max = 150, message = "Company location must be at most 150 characters")
    private String companyLocation;
}
