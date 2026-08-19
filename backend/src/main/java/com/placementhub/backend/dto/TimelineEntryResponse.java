package com.placementhub.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TimelineEntryResponse {
    private String status;
    private String remarks;
    private LocalDateTime changedAt;
}