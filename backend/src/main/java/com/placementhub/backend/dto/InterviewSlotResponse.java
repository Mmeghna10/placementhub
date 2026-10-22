package com.placementhub.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InterviewSlotResponse {
    private Long id;
    private Long jobId;
    private LocalDateTime startsAt;
    private Integer durationMinutes;
    private String location;
    private Integer capacity;
    private Integer bookedCount;
    private Integer seatsLeft;

    /** Student view only: whether this is the slot the student has booked. */
    private Boolean bookedByMe;

    /** Recruiter view only: who booked a seat. */
    private List<BookedApplicant> bookings;

    @Data
    public static class BookedApplicant {
        private Long applicationId;
        private String fullName;
        private String applicationStatus;
    }
}
