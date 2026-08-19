package com.placementhub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AnalyticsResponse {
    private List<ApplicationsByStatusResponse> applicationsByStatus;
    private long totalJobsPosted;
    private long approvedJobs;
    private long rejectedJobs;
}