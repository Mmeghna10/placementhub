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
    /** The job's placement tier. */
    private String jobTier;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    private List<TimelineEntryResponse> timeline;

    /** Send back with a status update; a mismatch means someone else changed it first. */
    private Long version;

    /** Statuses the recruiter may move this application to (empty from OFFERED onwards: the student decides). */
    private List<String> allowedNextStatuses;

    /** Who applied. Only filled in for the recruiter who owns the job. */
    private ApplicantSummary applicant;

    /** The booked interview slot, if any. */
    private InterviewBookingSummary interview;

    /** While OFFERED: when the offer lapses if the student has not accepted or declined. */
    private LocalDateTime offerExpiresAt;

    /** While WAITLISTED: the stored rank (recruiter view) and the live position (student view, 1 = next). */
    private Integer waitlistRank;
    private Integer waitlistPosition;
}
