package com.placementhub.backend.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of an application. Only the transitions listed here are allowed:
 *
 *   APPLIED ──► SHORTLISTED ──► INTERVIEW ──► OFFERED ──► ACCEPTED   (student)
 *      │             │              │            ├──────► DECLINED   (student)
 *      │             │              │            └──────► EXPIRED    (deadline passed)
 *      └─────────────┴──────────────┴──────► REJECTED
 *
 * {@link #allowedNext()} lists the moves a recruiter may make. Once OFFERED, the decision
 * is the student's (accept or decline) or the deadline's, never the recruiter's; see
 * OfferService. ACCEPTED, DECLINED, EXPIRED and REJECTED are final.
 */
public enum ApplicationStatus {
    APPLIED,
    SHORTLISTED,
    INTERVIEW,
    OFFERED,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    REJECTED;

    /** Moves a recruiter may make from this status. */
    public Set<ApplicationStatus> allowedNext() {
        return switch (this) {
            case APPLIED -> EnumSet.of(SHORTLISTED, REJECTED);
            case SHORTLISTED -> EnumSet.of(INTERVIEW, REJECTED);
            case INTERVIEW -> EnumSet.of(OFFERED, REJECTED);
            case OFFERED, ACCEPTED, DECLINED, EXPIRED, REJECTED -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canMoveTo(ApplicationStatus next) {
        return allowedNext().contains(next);
    }

    /** No further change is possible, by anyone. */
    public boolean isFinal() {
        return this == ACCEPTED || this == DECLINED || this == EXPIRED || this == REJECTED;
    }
}
