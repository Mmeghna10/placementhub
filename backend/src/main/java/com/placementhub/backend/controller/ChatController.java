package com.placementhub.backend.controller;

import com.placementhub.backend.dto.ChatRequest;
import com.placementhub.backend.dto.ChatResponse;
import com.placementhub.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String reply = chatService.getReply(request.getMessages());
        return ResponseEntity.ok(new ChatResponse(reply));
    }
}