package com.placementhub.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    private List<ChatMessageDto> messages;
}