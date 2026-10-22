package com.placementhub.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InterviewSlotRequest {

    // "Must be in the future" is checked in the service against the app's time zone clock;
    // @Future would use the server JVM's zone instead.
    @NotNull(message = "Start time is required")
    private LocalDateTime startsAt;

    @NotNull(message = "Duration is required")
    @Min(value = 5, message = "Duration must be between 5 and 480 minutes")
    @Max(value = 480, message = "Duration must be between 5 and 480 minutes")
    private Integer durationMinutes;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be between 1 and 100")
    @Max(value = 100, message = "Capacity must be between 1 and 100")
    private Integer capacity;

    @Size(max = 255, message = "Location must be at most 255 characters")
    private String location;
}
