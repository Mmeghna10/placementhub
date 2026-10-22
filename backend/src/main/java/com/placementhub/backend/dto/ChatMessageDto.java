package com.placementhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageDto {
    @NotBlank(message = "Message role is required")
    @Pattern(regexp = "user|assistant", message = "Message role must be user or assistant")
    private String role;

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000, message = "Message must be at most 2000 characters")
    private String content;
}
