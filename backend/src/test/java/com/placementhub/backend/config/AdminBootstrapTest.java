package com.placementhub.backend.config;

import com.placementhub.backend.entity.User;
import com.placementhub.backend.enums.Role;
import com.placementhub.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AdminBootstrap bootstrap;

    private void configure(String email, String password) {
        ReflectionTestUtils.setField(bootstrap, "email", email);
        ReflectionTestUtils.setField(bootstrap, "password", password);
    }

    @Test
    void doesNothingWhenNotConfigured() {
        configure("", "");
        bootstrap.run(null);
        verifyNoInteractions(userRepository);
    }

    @Test
    void createsAdminWithHashedPassword() {
        configure(" admin@placementhub.local ", "a-long-enough-password");
        when(userRepository.findByEmail("admin@placementhub.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("a-long-enough-password")).thenReturn("bcrypt-hash");

        bootstrap.run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@placementhub.local");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
    }

    @Test
    void neverChangesAnExistingAccount() {
        configure("someone@placementhub.local", "a-long-enough-password");
        User student = new User();
        student.setRole(Role.STUDENT);
        when(userRepository.findByEmail("someone@placementhub.local")).thenReturn(Optional.of(student));

        bootstrap.run(null);

        verify(userRepository, never()).save(any());
        assertThat(student.getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void refusesAShortPassword() {
        configure("admin@placementhub.local", "short");
        when(userRepository.findByEmail("admin@placementhub.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bootstrap.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12");
        verify(userRepository, never()).save(any());
    }
}
