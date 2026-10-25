package com.placementhub.backend.entity;

import com.placementhub.backend.enums.JobStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data

public class Job {
     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "job_type")
    private String jobType;

     private String location;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(name = "min_cgpa")
    private BigDecimal minCgpa;

    @Column(name = "eligible_branches", columnDefinition = "TEXT")
    private String eligibleBranches;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    /** Optimistic lock against two concurrent edits/approvals of the same job. */
    @Version
    private Long version;

    @Column(name = "posted_at", updatable = false)
    private LocalDateTime postedAt = LocalDateTime.now();

    /** How many people this job hires. Offers (pending + accepted) can never exceed it. */
    @Column(nullable = false)
    private Integer openings = 1;

    /** How long a student has to accept or decline an offer for this job. */
    @Column(name = "offer_deadline_hours", nullable = false)
    private Integer offerDeadlineHours = 72;

    /**
     * Seat counters, read-only here: they are changed only by the atomic conditional
     * UPDATEs in JobRepository, so saving a Job can never overwrite them with stale values.
     */
    @Column(name = "offers_pending", nullable = false, insertable = false, updatable = false)
    private Integer offersPending = 0;

    @Column(name = "offers_accepted", nullable = false, insertable = false, updatable = false)
    private Integer offersAccepted = 0;
}
