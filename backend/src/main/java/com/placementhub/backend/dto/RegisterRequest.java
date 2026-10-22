package com.placementhub.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.placementhub.backend.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must be at most 150 characters")
    private String email;

    // BCrypt only uses the first 72 bytes of a password, so longer input is rejected.
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must be at most 150 characters")
    private String fullName;

    /** Admin accounts are provisioned by an operator, never through public sign-up. */
    @JsonIgnore
    @AssertTrue(message = "Only STUDENT or RECRUITER accounts can be registered")
    public boolean isSelfRegistrableRole() {
        return role == null || role == Role.STUDENT || role == Role.RECRUITER;
    }
}
