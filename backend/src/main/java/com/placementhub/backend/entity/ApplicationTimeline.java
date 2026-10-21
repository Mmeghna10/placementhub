package com.placementhub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_timeline")
@Data

public class ApplicationTimeline {
     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    private String status;
    private String remarks;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt = LocalDateTime.now();
}
