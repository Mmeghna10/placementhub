package com.placementhub.backend.controller;

import jakarta.validation.Valid;
import com.placementhub.backend.dto.ChatRequest;
import com.placementhub.backend.dto.ChatResponse;
import com.placementhub.backend.service.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Assistant", description = "In-app AI guide (Gemini). Rate limited per user (429 RATE_LIMITED).")
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String reply = chatService.getReply(request.getMessages());
        return ResponseEntity.ok(new ChatResponse(reply));
    }
}