-- Milestone 4: database-enforced invariants for applications and jobs.
--
-- Checked on production before shipping (2026-10-22): no duplicate (student_id, job_id)
-- pairs, and every existing status value is in the allowed lists below.
--
-- One ALTER per table: MySQL DDL is not transactional, so each table either gets all of
-- its changes or none, and a failure cannot leave a half-applied table behind.

-- 1. One application per student per job: the final word on duplicates, even when two
--    requests race past the service's "already applied?" check.
-- 2. Version column for optimistic locking of concurrent status updates.
-- 3. Only known statuses can be stored.
ALTER TABLE applications
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT uk_applications_student_job UNIQUE (student_id, job_id),
    ADD CONSTRAINT chk_applications_status
        CHECK (status IN ('APPLIED', 'SHORTLISTED', 'INTERVIEW', 'OFFERED', 'REJECTED'));

ALTER TABLE jobs
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_jobs_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CLOSED'));
