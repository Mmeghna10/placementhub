package com.placementhub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApplicationsByStatusResponse {
    private String status;
    private long count;
}