-- Offer Desk, phase 2: a ranked waitlist per job. When an offer is declined or expires,
-- the opening is offered to the next waitlisted candidate automatically. Purely additive.

ALTER TABLE applications
    ADD COLUMN waitlist_rank INT NULL;

ALTER TABLE applications DROP CHECK chk_applications_status;
ALTER TABLE applications
    ADD CONSTRAINT chk_applications_status
        CHECK (status IN ('APPLIED', 'SHORTLISTED', 'INTERVIEW', 'WAITLISTED', 'OFFERED', 'ACCEPTED', 'DECLINED',
                          'EXPIRED', 'REJECTED'));

-- Promotion reads a job's waitlist in rank order.
CREATE INDEX idx_applications_waitlist ON applications (job_id, status, waitlist_rank);
