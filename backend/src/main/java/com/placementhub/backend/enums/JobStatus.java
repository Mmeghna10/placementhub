package com.placementhub.backend.enums;

/**
 * PENDING  – waiting for admin review (new or edited job); not visible to students.
 * APPROVED – open for applications (until its deadline).
 * REJECTED – refused by the admin.
 * CLOSED   – the recruiter stopped accepting applications; existing ones continue.
 */
public enum JobStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CLOSED
}
