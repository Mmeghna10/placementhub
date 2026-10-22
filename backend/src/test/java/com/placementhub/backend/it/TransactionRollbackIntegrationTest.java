package com.placementhub.backend.it;

import com.placementhub.backend.dto.RegisterRequest;
import com.placementhub.backend.enums.Role;
import com.placementhub.backend.repository.UserRepository;
import com.placementhub.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Registration writes two rows (users, then students). This test makes the second insert
 * fail for real in MySQL and checks the first one was rolled back.
 *
 * Not @Transactional on purpose: the service's own transaction must commit or roll back.
 */
class TransactionRollbackIntegrationTest extends MySqlIntegrationTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    @Test
    void failedProfileInsertRollsBackTheUserRow() {
        String email = "rollback-" + UUID.randomUUID() + "@example.com";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("secret123");
        request.setRole(Role.STUDENT);
        // students.full_name is varchar(150). Calling the service directly skips the DTO
        // validation, so MySQL (strict mode) rejects the student insert.
        request.setFullName("x".repeat(200));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Before Milestone 3 (no @Transactional) this user row was left behind, orphaned.
        assertThat(userRepository.existsByEmail(email)).isFalse();
    }
}
