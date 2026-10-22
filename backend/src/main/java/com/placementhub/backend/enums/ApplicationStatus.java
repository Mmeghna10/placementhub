package com.placementhub.backend.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of an application. Only the transitions listed here are allowed:
 *
 *   APPLIED ──► SHORTLISTED ──► INTERVIEW ──► OFFERED
 *      │             │              │
 *      └─────────────┴──────────────┴──────► REJECTED
 *
 * OFFERED and REJECTED are final. Keeping the rules on the enum means every caller
 * (recruiter API, future admin tools, background jobs) enforces the same state machine.
 */
public enum ApplicationStatus {
    APPLIED,
    SHORTLISTED,
    INTERVIEW,
    OFFERED,
    REJECTED;

    public Set<ApplicationStatus> allowedNext() {
        return switch (this) {
            case APPLIED -> EnumSet.of(SHORTLISTED, REJECTED);
            case SHORTLISTED -> EnumSet.of(INTERVIEW, REJECTED);
            case INTERVIEW -> EnumSet.of(OFFERED, REJECTED);
            case OFFERED, REJECTED -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canMoveTo(ApplicationStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isFinal() {
        return allowedNext().isEmpty();
    }
}
