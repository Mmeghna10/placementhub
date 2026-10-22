package com.placementhub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;

/** One student's seat in an interview slot. At most one per application (unique key). */
@Entity
@Table(name = "interview_bookings")
@Data
public class InterviewBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "slot_id", nullable = false)
    private InterviewSlot slot;

    @OneToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Column(name = "booked_at", updatable = false)
    private LocalDateTime bookedAt = LocalDateTime.now();
}
