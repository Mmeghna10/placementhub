-- Offer Desk, phase 3: placement tiers and campus rules.
--
-- A job has a tier (REGULAR < DREAM < SUPER_DREAM) set by the placement cell on approval.
-- A placed student may only take offers from a higher tier, up to a maximum number of
-- accepted offers. Accepting a higher-tier offer releases the earlier placement, and
-- applications the rules no longer allow are withdrawn. Additive; existing jobs become
-- REGULAR and existing placements are backfilled.

ALTER TABLE jobs
    ADD COLUMN tier VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    ADD CONSTRAINT chk_jobs_tier CHECK (tier IN ('REGULAR', 'DREAM', 'SUPER_DREAM'));

-- placed_tier: rank of the student's current placement (0 = not placed, 1 = REGULAR,
-- 2 = DREAM, 3 = SUPER_DREAM). accepted_count: offers accepted so far (released ones too).
-- Both are changed only together with placed_application_id, by one conditional UPDATE.
ALTER TABLE students
    ADD COLUMN placed_tier INT NOT NULL DEFAULT 0,
    ADD COLUMN accepted_count INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_students_placed_tier CHECK (placed_tier BETWEEN 0 AND 3);

UPDATE students SET placed_tier = 1 WHERE placed_application_id IS NOT NULL;

UPDATE students s
SET s.accepted_count = (SELECT COUNT(*) FROM applications a WHERE a.student_id = s.id AND a.status = 'ACCEPTED');

-- New final statuses: WITHDRAWN (closed by the rules when the student was placed) and
-- RELEASED (an accepted offer given up for a higher-tier one).
ALTER TABLE applications DROP CHECK chk_applications_status;
ALTER TABLE applications
    ADD CONSTRAINT chk_applications_status
        CHECK (status IN ('APPLIED', 'SHORTLISTED', 'INTERVIEW', 'WAITLISTED', 'OFFERED', 'ACCEPTED', 'DECLINED',
                          'EXPIRED', 'REJECTED', 'WITHDRAWN', 'RELEASED'));

-- The campus rules: a single row the placement cell edits.
CREATE TABLE placement_policy (
    id TINYINT NOT NULL,
    allow_upgrades BOOLEAN NOT NULL DEFAULT TRUE,
    max_accepted_offers INT NOT NULL DEFAULT 2,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_placement_policy_single_row CHECK (id = 1),
    CONSTRAINT chk_placement_policy_max CHECK (max_accepted_offers BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO placement_policy (id, allow_upgrades, max_accepted_offers) VALUES (1, TRUE, 2);
