package com.placementhub.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A recruiter's Offer Desk: openings, pending offers, waitlists and recent offer activity
 * across all of their jobs (Offer Desk phase 4).
 */
public record OfferDeskResponse(Totals totals, List<JobDesk> jobs, List<Event> recentEvents) {

    /**
     * acceptanceRate: accepted ÷ decided offers (accepted, released, declined, expired), or
     * null before any offer was decided. autoRefills: offers made automatically from a waitlist.
     */
    public record Totals(int openings, int filled, int pendingOffers, int waitlisted,
                         Double acceptanceRate, int autoRefills) {
    }

    public record JobDesk(Long jobId, String title, String tier, String status, int openings, int filled,
                          int pendingOffers, int waitlisted, List<PendingOffer> offers) {
    }

    public record PendingOffer(Long applicationId, String studentName, LocalDateTime expiresAt) {
    }

    public record Event(Long applicationId, String jobTitle, String studentName, String status, String remarks,
                        LocalDateTime at) {
    }
}
