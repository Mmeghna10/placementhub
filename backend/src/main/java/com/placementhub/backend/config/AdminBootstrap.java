package com.placementhub.backend.config;

import com.placementhub.backend.entity.User;
import com.placementhub.backend.enums.Role;
import com.placementhub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first admin account on startup when APP_BOOTSTRAP_ADMIN_EMAIL and
 * APP_BOOTSTRAP_ADMIN_PASSWORD are set. Public sign-up can never create admins, so without
 * this a fresh database (Docker, a new environment) would need a manual SQL update.
 *
 * Safe to leave configured: if the email already exists nothing is changed (an existing
 * non-admin account is never promoted), and the password is only used when creating.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()) {
            return;
        }
        String normalized = email.trim();

        userRepository.findByEmail(normalized).ifPresentOrElse(existing -> {
            if (existing.getRole() != Role.ADMIN) {
                log.warn("Bootstrap admin {} already exists as {}; leaving it unchanged", normalized, existing.getRole());
            }
        }, () -> {
            if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
                throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD must be at least "
                        + MIN_PASSWORD_LENGTH + " characters to create the bootstrap admin");
            }
            User admin = new User();
            admin.setEmail(normalized);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info("Created bootstrap admin account {}", normalized);
        });
    }
}
