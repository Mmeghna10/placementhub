package com.placementhub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;

/**
 * A recruiter-published interview time for one job, with a fixed number of seats.
 * bookedCount is only ever changed by the atomic UPDATEs in InterviewSlotRepository,
 * never by setting the field and saving, so concurrent bookings cannot lose increments.
 */
@Entity
@Table(name = "interview_slots")
@Data
public class InterviewSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "booked_count", nullable = false, insertable = false, updatable = false)
    private Integer bookedCount = 0;

    private String location;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
