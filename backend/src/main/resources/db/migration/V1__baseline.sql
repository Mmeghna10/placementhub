-- Baseline: the schema as it existed before Flyway was introduced (Milestone 3).
--
-- Existing databases (local dev, Azure) already contain these tables. On them Flyway
-- does NOT run this file: spring.flyway.baseline-on-migrate marks version 1 as applied
-- and only later migrations run. Fresh databases (Docker, CI, new developers) run it.
--
-- Captured with `mysqldump --no-data` from MySQL 8.0 on 2026-10-21; only AUTO_INCREMENT
-- counters were removed. Do not edit: change the schema in a new V<n>__*.sql file.

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(150) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` varchar(20) NOT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `companies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `description` text,
  `website` varchar(255) DEFAULT NULL,
  `industry` varchar(100) DEFAULT NULL,
  `location` varchar(150) DEFAULT NULL,
  `is_verified` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `students` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `full_name` varchar(150) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `college` varchar(150) DEFAULT NULL,
  `degree` varchar(100) DEFAULT NULL,
  `branch` varchar(100) DEFAULT NULL,
  `graduation_year` int DEFAULT NULL,
  `cgpa` decimal(3,2) DEFAULT NULL,
  `resume_url` varchar(255) DEFAULT NULL,
  `skills` text,
  `linkedin_url` varchar(255) DEFAULT NULL,
  `github_url` varchar(255) DEFAULT NULL,
  `profile_completed` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `students_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `recruiters` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `full_name` varchar(150) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `designation` varchar(100) DEFAULT NULL,
  `company_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `company_id` (`company_id`),
  CONSTRAINT `recruiters_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `recruiters_ibfk_2` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `jobs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recruiter_id` bigint NOT NULL,
  `company_id` bigint NOT NULL,
  `title` varchar(150) NOT NULL,
  `description` text,
  `job_type` varchar(20) DEFAULT NULL,
  `location` varchar(150) DEFAULT NULL,
  `salary_min` int DEFAULT NULL,
  `salary_max` int DEFAULT NULL,
  `required_skills` text,
  `min_cgpa` decimal(3,2) DEFAULT NULL,
  `eligible_branches` text,
  `application_deadline` date DEFAULT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `posted_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `recruiter_id` (`recruiter_id`),
  KEY `company_id` (`company_id`),
  CONSTRAINT `jobs_ibfk_1` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiters` (`id`),
  CONSTRAINT `jobs_ibfk_2` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `job_id` bigint NOT NULL,
  `status` varchar(20) DEFAULT 'APPLIED',
  `applied_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `student_id` (`student_id`),
  KEY `job_id` (`job_id`),
  CONSTRAINT `applications_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `applications_ibfk_2` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `application_timeline` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `application_id` bigint NOT NULL,
  `status` varchar(20) DEFAULT NULL,
  `remarks` varchar(255) DEFAULT NULL,
  `changed_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `application_id` (`application_id`),
  CONSTRAINT `application_timeline_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
