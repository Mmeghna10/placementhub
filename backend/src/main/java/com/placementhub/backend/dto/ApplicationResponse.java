package com.placementhub.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    private List<TimelineEntryResponse> timeline;

    /** Send back with a status update; a mismatch means someone else changed it first. */
    private Long version;

    /** Statuses this application may move to next (empty once OFFERED or REJECTED). */
    private List<String> allowedNextStatuses;

    /** Who applied. Only filled in for the recruiter who owns the job. */
    private ApplicantSummary applicant;

    /** The booked interview slot, if any. */
    private InterviewBookingSummary interview;
}
