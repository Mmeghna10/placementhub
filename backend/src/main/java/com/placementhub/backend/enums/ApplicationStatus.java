package com.placementhub.backend.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of an application. Only the transitions listed here are allowed:
 *
 *   APPLIED ──► SHORTLISTED ──► INTERVIEW ──► OFFERED ──► ACCEPTED   (student)
 *      │             │              │  ▲         ├──────► DECLINED   (student)
 *      │             │              ▼  │         └──────► EXPIRED    (deadline passed)
 *      │             │          WAITLISTED  (promoted to OFFERED by the recruiter, or
 *      │             │              │        automatically when an opening frees up)
 *      └─────────────┴──────────────┴──────► REJECTED
 *
 * Set by the placement rules (phase 3), never by a recruiter: WITHDRAWN (the student was
 * placed and may no longer take this job) and RELEASED (an ACCEPTED offer given up for a
 * higher-tier one).
 *
 * {@link #allowedNext()} lists the moves a recruiter may make. Once OFFERED, the decision
 * is the student's (accept or decline) or the deadline's, never the recruiter's; see
 * OfferService. ACCEPTED is final for the recruiter; the rest of the end states are final.
 */
public enum ApplicationStatus {
    APPLIED,
    SHORTLISTED,
    INTERVIEW,
    WAITLISTED,
    OFFERED,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    REJECTED,
    WITHDRAWN,
    RELEASED;

    /** Moves a recruiter may make from this status. */
    public Set<ApplicationStatus> allowedNext() {
        return switch (this) {
            case APPLIED -> EnumSet.of(SHORTLISTED, REJECTED);
            case SHORTLISTED -> EnumSet.of(INTERVIEW, REJECTED);
            case INTERVIEW -> EnumSet.of(OFFERED, WAITLISTED, REJECTED);
            case WAITLISTED -> EnumSet.of(OFFERED, REJECTED);
            case OFFERED, ACCEPTED, DECLINED, EXPIRED, REJECTED, WITHDRAWN, RELEASED -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canMoveTo(ApplicationStatus next) {
        return allowedNext().contains(next);
    }

    /** No further change is possible, by anyone. */
    public boolean isFinal() {
        return this == ACCEPTED || this == DECLINED || this == EXPIRED || this == REJECTED
                || this == WITHDRAWN || this == RELEASED;
    }
}
