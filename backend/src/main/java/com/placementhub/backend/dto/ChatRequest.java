package com.placementhub.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    // Caps how much conversation history is forwarded to Gemini (cost and abuse control).
    @NotEmpty(message = "At least one message is required")
    @Size(max = 20, message = "Conversation is too long; please start a new chat")
    private List<@Valid ChatMessageDto> messages;
}
