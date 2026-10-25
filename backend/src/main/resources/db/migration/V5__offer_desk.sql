-- Offer Desk, phase 1: a job has a number of openings; an offer has a deadline and the
-- student accepts or declines it; a student holds at most one pending offer and is placed
-- at most once.
--
-- Additive except for one data change agreed with the owner: offers made before this
-- release could not be accepted in the app, so existing OFFERED applications are recorded
-- as ACCEPTED (with a timeline entry saying so).

ALTER TABLE jobs
    ADD COLUMN openings INT NOT NULL DEFAULT 1,
    ADD COLUMN offer_deadline_hours INT NOT NULL DEFAULT 72,
    -- Seat counters, changed only by atomic conditional UPDATEs (see JobRepository).
    ADD COLUMN offers_pending INT NOT NULL DEFAULT 0,
    ADD COLUMN offers_accepted INT NOT NULL DEFAULT 0;

ALTER TABLE applications
    ADD COLUMN offer_expires_at DATETIME NULL;

-- One pending offer and one placement per student. Each is a pointer to the application,
-- claimed with a conditional UPDATE (... WHERE column IS NULL), so two simultaneous offers
-- or acceptances cannot both win. The unique keys stop two students pointing at the same
-- application.
ALTER TABLE students
    ADD COLUMN pending_offer_application_id BIGINT NULL,
    ADD COLUMN placed_application_id BIGINT NULL,
    ADD CONSTRAINT uk_students_pending_offer UNIQUE (pending_offer_application_id),
    ADD CONSTRAINT uk_students_placed UNIQUE (placed_application_id),
    ADD CONSTRAINT fk_students_pending_offer FOREIGN KEY (pending_offer_application_id) REFERENCES applications (id),
    ADD CONSTRAINT fk_students_placed FOREIGN KEY (placed_application_id) REFERENCES applications (id);

-- New statuses: ACCEPTED, DECLINED, EXPIRED.
ALTER TABLE applications DROP CHECK chk_applications_status;

UPDATE applications SET status = 'ACCEPTED' WHERE status = 'OFFERED';

INSERT INTO application_timeline (application_id, status, remarks, changed_at)
SELECT id, 'ACCEPTED', 'Recorded as accepted: offered before offers could be accepted in the app', NOW()
FROM applications
WHERE status = 'ACCEPTED';

ALTER TABLE applications
    ADD CONSTRAINT chk_applications_status
        CHECK (status IN ('APPLIED', 'SHORTLISTED', 'INTERVIEW', 'OFFERED', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'REJECTED'));

-- Counters and placements for the converted offers. A job keeps at least as many
-- openings as it already filled.
UPDATE jobs j
SET j.offers_accepted = (SELECT COUNT(*) FROM applications a WHERE a.job_id = j.id AND a.status = 'ACCEPTED');

UPDATE jobs SET openings = GREATEST(openings, offers_accepted);

UPDATE students s
JOIN (SELECT student_id, MAX(id) AS application_id
      FROM applications
      WHERE status = 'ACCEPTED'
      GROUP BY student_id) placed ON placed.student_id = s.id
SET s.placed_application_id = placed.application_id;

ALTER TABLE jobs
    ADD CONSTRAINT chk_jobs_openings CHECK (openings BETWEEN 1 AND 500),
    ADD CONSTRAINT chk_jobs_offer_deadline CHECK (offer_deadline_hours BETWEEN 1 AND 720),
    -- Safety net under the atomic counters: never more offers than openings.
    ADD CONSTRAINT chk_jobs_offer_seats
        CHECK (offers_pending >= 0 AND offers_accepted >= 0 AND offers_pending + offers_accepted <= openings);

-- The expiry job looks for OFFERED applications whose deadline has passed.
CREATE INDEX idx_applications_offer_expiry ON applications (status, offer_expires_at);
