package com.placementhub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminDashboardResponse {
    private long totalStudents;
    private long totalRecruiters;
    private long totalCompanies;
    private long totalJobs;
    private long pendingJobs;
    private long totalApplications;
}