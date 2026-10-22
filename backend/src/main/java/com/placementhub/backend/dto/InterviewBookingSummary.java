package com.placementhub.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InterviewBookingSummary {
    private Long slotId;
    private LocalDateTime startsAt;
    private Integer durationMinutes;
    private String location;
    private LocalDateTime bookedAt;
}
