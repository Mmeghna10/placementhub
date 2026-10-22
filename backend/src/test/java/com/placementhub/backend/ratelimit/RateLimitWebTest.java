package com.placementhub.backend.ratelimit;

import com.placementhub.backend.config.RestTemplateConfig;
import com.placementhub.backend.config.SecurityConfig;
import com.placementhub.backend.controller.AuthController;
import com.placementhub.backend.controller.ChatController;
import com.placementhub.backend.dto.AuthResponse;
import com.placementhub.backend.exception.GlobalExceptionHandler;
import com.placementhub.backend.security.JwtFilter;
import com.placementhub.backend.security.JwtUtil;
import com.placementhub.backend.security.SecurityErrorWriter;
import com.placementhub.backend.security.UserDetailsServiceImpl;
import com.placementhub.backend.service.AuthService;
import com.placementhub.backend.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The rate-limit filter in the real security chain, with small limits so tests are quick. */
@WebMvcTest(controllers = {AuthController.class, ChatController.class})
@Import({SecurityConfig.class, JwtFilter.class, JwtUtil.class, SecurityErrorWriter.class,
        GlobalExceptionHandler.class, RestTemplateConfig.class})
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-that-is-definitely-at-least-32-bytes-long",
        "app.jwt.expiration-ms=3600000",
        "app.rate-limit.login-per-minute=3",
        "app.rate-limit.chat-per-10-minutes=2"
})
class RateLimitWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;

    @MockitoBean UserDetailsServiceImpl userDetailsService;
    @MockitoBean AuthService authService;
    @MockitoBean ChatService chatService;

    private MockHttpServletRequestBuilder login(String ip) {
        return post("/api/auth/login")
                .with(request -> { request.setRemoteAddr(ip); return request; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@example.com\",\"password\":\"secret123\"}");
    }

    @Test
    void fourthLoginInAMinuteFromOneIpIsRefusedBeforeReachingTheService() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("t", "a@example.com", "STUDENT"));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(login("198.51.100.7")).andExpect(status().isOk());
        }
        mockMvc.perform(login("198.51.100.7"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        // The refused attempt never got as far as checking the password.
        verify(authService, times(3)).login(any());

        // Another IP has its own allowance.
        mockMvc.perform(login("198.51.100.8")).andExpect(status().isOk());
    }

    @Test
    void chatIsLimitedPerLoggedInUser() throws Exception {
        String user = "chatty@example.com";
        when(userDetailsService.loadUserByUsername(user)).thenReturn(
                User.withUsername(user).password("x").authorities("ROLE_STUDENT").build());
        when(chatService.getReply(anyList())).thenReturn("hi");
        String token = "Bearer " + jwtUtil.generateToken(user, "STUDENT");
        String body = "{\"messages\":[{\"role\":\"user\",\"content\":\"hello\"}]}";

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/chat").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/chat").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());

        // Two Gemini calls, not three.
        verify(chatService, times(2)).getReply(anyList());
    }
}
