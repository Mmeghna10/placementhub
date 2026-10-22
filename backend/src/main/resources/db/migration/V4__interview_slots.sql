-- Milestone 4: interview slot booking.
--
-- A recruiter publishes slots for one of their jobs; a student whose application is in
-- INTERVIEW books one seat. Overbooking is prevented in three layers:
--   1. the service reserves a seat with an atomic conditional UPDATE
--      (booked_count = booked_count + 1 WHERE booked_count < capacity),
--   2. chk_interview_slots_booked rejects any write that would exceed capacity,
--   3. uk_interview_bookings_application allows one booking per application.
-- starts_at is local time in the app's time zone (app.timezone, default Asia/Kolkata).

CREATE TABLE interview_slots (
  id bigint NOT NULL AUTO_INCREMENT,
  job_id bigint NOT NULL,
  starts_at datetime NOT NULL,
  duration_minutes int NOT NULL,
  capacity int NOT NULL,
  booked_count int NOT NULL DEFAULT 0,
  location varchar(255) DEFAULT NULL,
  created_at timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_interview_slots_job_starts (job_id, starts_at),
  CONSTRAINT fk_interview_slots_job FOREIGN KEY (job_id) REFERENCES jobs (id),
  CONSTRAINT chk_interview_slots_capacity CHECK (capacity BETWEEN 1 AND 100),
  CONSTRAINT chk_interview_slots_duration CHECK (duration_minutes BETWEEN 5 AND 480),
  CONSTRAINT chk_interview_slots_booked CHECK (booked_count >= 0 AND booked_count <= capacity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE interview_bookings (
  id bigint NOT NULL AUTO_INCREMENT,
  slot_id bigint NOT NULL,
  application_id bigint NOT NULL,
  booked_at timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_interview_bookings_application (application_id),
  KEY idx_interview_bookings_slot (slot_id),
  CONSTRAINT fk_interview_bookings_slot FOREIGN KEY (slot_id) REFERENCES interview_slots (id),
  CONSTRAINT fk_interview_bookings_application FOREIGN KEY (application_id) REFERENCES applications (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
