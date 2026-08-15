package com.placementhub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
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

    @ManyToOne
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;

    @ManyToOne
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

    private String status = "PENDING";

    @Column(name = "posted_at", updatable = false)
    private LocalDateTime postedAt = LocalDateTime.now();
}
