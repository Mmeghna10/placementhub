package com.placementhub.backend.repository;

import com.placementhub.backend.entity.Job;
import com.placementhub.backend.enums.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Every job listing maps the company name, so list queries fetch the company in the same
// SQL statement (to-one join, so it is also safe with pagination).
public interface JobRepository extends JpaRepository<Job, Long> {

    @EntityGraph(attributePaths = "company")
    List<Job> findByStatus(JobStatus status);

    @EntityGraph(attributePaths = "company")
    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "company")
    List<Job> findByRecruiterId(Long recruiterId);

    @Override
    @EntityGraph(attributePaths = "company")
    Page<Job> findAll(Pageable pageable);

    /** Jobs students can still apply to: in the given status and not past their deadline. */
    @EntityGraph(attributePaths = "company")
    @Query("""
            select j from Job j
            where j.status = :status
              and (j.applicationDeadline is null or j.applicationDeadline >= :today)
            order by j.postedAt desc
            """)
    List<Job> findOpenJobs(@Param("status") JobStatus status, @Param("today") LocalDate today);

    /**
     * Student job search: open jobs (in the given status, deadline not passed) filtered in
     * the database and returned one page at a time, so the cost does not grow with the
     * total number of jobs. Every filter is optional (null = not applied).
     *
     * eligibleOnly mirrors EligibilityPolicy in SQL (CGPA minimum and case-insensitive
     * branch "contains"); SearchJobsEligibilityIntegrationTest checks the two agree.
     * Patterns are passed already lower-cased and wrapped in %...% by the service.
     */
    @EntityGraph(attributePaths = "company")
    @Query(value = """
            select j from Job j
            where j.status = :status
              and (j.applicationDeadline is null or j.applicationDeadline >= :today)
              and (:text is null
                   or lower(j.title) like :text
                   or lower(j.company.name) like :text
                   or lower(j.requiredSkills) like :text)
              and (:jobType is null or j.jobType = :jobType)
              and (:location is null or lower(j.location) like :location)
              and (:eligibleOnly = false or (
                    (j.minCgpa is null or (:cgpa is not null and j.minCgpa <= :cgpa))
                    and (j.eligibleBranches is null or trim(j.eligibleBranches) = ''
                         or (:branch is not null and lower(j.eligibleBranches) like :branch))))
            """,
            countQuery = """
            select count(j) from Job j
            where j.status = :status
              and (j.applicationDeadline is null or j.applicationDeadline >= :today)
              and (:text is null
                   or lower(j.title) like :text
                   or lower(j.company.name) like :text
                   or lower(j.requiredSkills) like :text)
              and (:jobType is null or j.jobType = :jobType)
              and (:location is null or lower(j.location) like :location)
              and (:eligibleOnly = false or (
                    (j.minCgpa is null or (:cgpa is not null and j.minCgpa <= :cgpa))
                    and (j.eligibleBranches is null or trim(j.eligibleBranches) = ''
                         or (:branch is not null and lower(j.eligibleBranches) like :branch))))
            """)
    Page<Job> searchOpenJobs(@Param("status") JobStatus status,
                             @Param("today") LocalDate today,
                             @Param("text") String text,
                             @Param("jobType") String jobType,
                             @Param("location") String location,
                             @Param("eligibleOnly") boolean eligibleOnly,
                             @Param("cgpa") java.math.BigDecimal cgpa,
                             @Param("branch") String branch,
                             Pageable pageable);

    /**
     * Loads the job for an apply request with a shared row lock (SELECT ... FOR SHARE).
     *
     * Many applications to the same job can hold the shared lock at once, so applying never
     * blocks applying. Closing, editing or re-reviewing the job UPDATEs the row, which needs
     * an exclusive lock, so it waits for in-flight applications to commit; applications that
     * start after it see the new status/deadline. The job state an application was checked
     * against therefore cannot change before that application commits.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select j from Job j where j.id = :id")
    Optional<Job> findByIdForApplication(@Param("id") Long id);

    long countByStatus(JobStatus status);
}
