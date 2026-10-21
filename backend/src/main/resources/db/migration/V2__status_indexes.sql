-- Indexes for the status filters used on every listing and dashboard request:
--   jobs:         findByStatus('APPROVED' / 'PENDING'), countByStatus(...)
--   applications: countByStatus(...) for admin analytics
--
-- Purely additive, so it is safe on existing data. The UNIQUE (student_id, job_id) key is
-- deliberately NOT here: it would fail on any database that already holds duplicate
-- applications, so it ships in Milestone 4 after production has been checked.

CREATE INDEX idx_jobs_status_posted_at ON jobs (status, posted_at);
CREATE INDEX idx_applications_status ON applications (status);
