package com.placementhub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;

@Entity
@Table(name = "students")
@Data

public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "full_name")
    private String fullName;

    private String phone;
    private String college;
    private String degree;
    private String branch;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    private BigDecimal cgpa;

     @Column(name = "resume_url")
    private String resumeUrl;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "profile_completed")
    private Boolean profileCompleted = false;

    /**
     * The application with this student's one pending offer, and the one they accepted.
     * Read-only here: claimed and released only by the conditional UPDATEs in
     * StudentRepository, so two simultaneous offers or acceptances cannot both win.
     */
    @Column(name = "pending_offer_application_id", insertable = false, updatable = false)
    private Long pendingOfferApplicationId;

    @Column(name = "placed_application_id", insertable = false, updatable = false)
    private Long placedApplicationId;

    /** Rank of the current placement's tier (0 = not placed); changed with placedApplicationId. */
    @Column(name = "placed_tier", nullable = false, insertable = false, updatable = false)
    private Integer placedTier = 0;

    /** Offers accepted so far, including released ones. */
    @Column(name = "accepted_count", nullable = false, insertable = false, updatable = false)
    private Integer acceptedCount = 0;
}
