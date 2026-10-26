package com.placementhub.backend.security;

import com.placementhub.backend.config.RestTemplateConfig;
import com.placementhub.backend.config.SecurityConfig;
import com.placementhub.backend.controller.AdminController;
import com.placementhub.backend.controller.AuthController;
import com.placementhub.backend.controller.RecruiterController;
import com.placementhub.backend.controller.StudentController;
import com.placementhub.backend.dto.AdminDashboardResponse;
import com.placementhub.backend.dto.AuthResponse;
import com.placementhub.backend.dto.PageResponse;
import com.placementhub.backend.dto.StudentProfileResponse;
import com.placementhub.backend.exception.ConflictException;
import com.placementhub.backend.exception.GlobalExceptionHandler;
import com.placementhub.backend.exception.NotFoundException;
import com.placementhub.backend.service.AdminService;
import com.placementhub.backend.service.AuthService;
import com.placementhub.backend.service.InterviewSlotService;
import com.placementhub.backend.service.OfferService;
import com.placementhub.backend.service.RecruiterService;
import com.placementhub.backend.service.StudentService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests: the real SecurityConfig, JwtFilter, JwtUtil and GlobalExceptionHandler,
 * with services mocked. No database is needed.
 */
@WebMvcTest(controllers = {AuthController.class, StudentController.class,
        RecruiterController.class, AdminController.class})
@Import({SecurityConfig.class, JwtFilter.class, JwtUtil.class, SecurityErrorWriter.class,
        GlobalExceptionHandler.class, RestTemplateConfig.class})
@TestPropertySource(properties = {
        "app.jwt.secret=" + ApiSecurityTest.SECRET,
        "app.jwt.expiration-ms=3600000",
        // Rate limiting has its own tests (RateLimitWebTest); here it would make results
        // depend on how many requests earlier tests sent.
        "app.rate-limit.enabled=false"
})
class ApiSecurityTest {

    static final String SECRET = "test-secret-that-is-definitely-at-least-32-bytes-long";

    private static final String STUDENT = "student@example.com";
    private static final String ADMIN = "admin@example.com";
    private static final String DISABLED = "disabled@example.com";

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;

    @MockitoBean UserDetailsServiceImpl userDetailsService;
    @MockitoBean AuthService authService;
    @MockitoBean StudentService studentService;
    @MockitoBean RecruiterService recruiterService;
    @MockitoBean AdminService adminService;
    @MockitoBean InterviewSlotService interviewSlotService;
    @MockitoBean OfferService offerService;

    @BeforeEach
    void users() {
        when(userDetailsService.loadUserByUsername(STUDENT)).thenReturn(
                User.withUsername(STUDENT).password("x").authorities("ROLE_STUDENT").build());
        when(userDetailsService.loadUserByUsername(ADMIN)).thenReturn(
                User.withUsername(ADMIN).password("x").authorities("ROLE_ADMIN").build());
        when(userDetailsService.loadUserByUsername(DISABLED)).thenReturn(
                User.withUsername(DISABLED).password("x").authorities("ROLE_STUDENT").disabled(true).build());
    }

    private String bearer(String email, String role) {
        return "Bearer " + jwtUtil.generateToken(email, role);
    }

    @Nested
    class Registration {

        @Test
        void registeringAsAdminIsRejectedBeforeReachingTheService() throws Exception {
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                            {"email":"evil@example.com","password":"secret123","role":"ADMIN","fullName":"Evil"}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.error").value("Only STUDENT or RECRUITER accounts can be registered"));

            verify(authService, never()).register(any());
        }

        @Test
        void registeringAsStudentSucceeds() throws Exception {
            when(authService.register(any())).thenReturn(new AuthResponse("token", "new@example.com", "STUDENT"));

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                            {"email":"new@example.com","password":"secret123","role":"STUDENT","fullName":"New"}
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("STUDENT"));
        }

        @Test
        void unknownRoleValueIsAMalformedRequest() throws Exception {
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                            {"email":"x@example.com","password":"secret123","role":"SUPERUSER","fullName":"X"}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        }

        @Test
        void duplicateEmailIsAConflict() throws Exception {
            when(authService.register(any()))
                    .thenThrow(new ConflictException("EMAIL_ALREADY_REGISTERED", "Email already registered"));

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                            {"email":"dup@example.com","password":"secret123","role":"STUDENT","fullName":"Dup"}
                            """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Email already registered"));
        }
    }

    @Nested
    class Authentication {

        @Test
        void missingTokenIs401WithJsonBody() throws Exception {
            mockMvc.perform(get("/api/students/profile"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.path").value("/api/students/profile"));
        }

        @Test
        void expiredTokenIs401TokenExpired() throws Exception {
            String expired = Jwts.builder()
                    .subject(STUDENT)
                    .claim("role", "STUDENT")
                    .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                    .expiration(new Date(System.currentTimeMillis() - 3_600_000))
                    .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                    .compact();

            mockMvc.perform(get("/api/students/profile").header("Authorization", "Bearer " + expired))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
        }

        @Test
        void tokenSignedWithAnotherKeyIs401InvalidToken() throws Exception {
            String forged = Jwts.builder()
                    .subject(ADMIN)
                    .claim("role", "ADMIN")
                    .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                    .signWith(Keys.hmacShaKeyFor(
                            "a-completely-different-secret-of-32-plus-bytes".getBytes(StandardCharsets.UTF_8)))
                    .compact();

            mockMvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + forged))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        }

        @Test
        void garbageTokenIs401InvalidToken() throws Exception {
            mockMvc.perform(get("/api/students/profile").header("Authorization", "Bearer not.a.jwt"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        }

        @Test
        void tokenForDeletedUserIs401() throws Exception {
            when(userDetailsService.loadUserByUsername("gone@example.com"))
                    .thenThrow(new UsernameNotFoundException("User not found"));

            mockMvc.perform(get("/api/students/profile").header("Authorization", bearer("gone@example.com", "STUDENT")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        }

        @Test
        void deactivatedAccountIsRejectedEvenWithValidToken() throws Exception {
            mockMvc.perform(get("/api/students/profile").header("Authorization", bearer(DISABLED, "STUDENT")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        }

        @Test
        void staleTokenDoesNotBlockPublicLogin() throws Exception {
            when(authService.login(any())).thenReturn(new AuthResponse("fresh", STUDENT, "STUDENT"));

            mockMvc.perform(post("/api/auth/login")
                            .header("Authorization", "Bearer not.a.jwt")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"student@example.com","password":"secret123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("fresh"));
        }

        @Test
        void validTokenReachesController() throws Exception {
            StudentProfileResponse profile = new StudentProfileResponse();
            profile.setEmail(STUDENT);
            when(studentService.getProfile()).thenReturn(profile);

            mockMvc.perform(get("/api/students/profile").header("Authorization", bearer(STUDENT, "STUDENT")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(STUDENT));
        }
    }

    @Nested
    class Authorization {

        @Test
        void studentCallingAdminApiIs403NotBadRequest() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(STUDENT, "STUDENT")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

            verify(adminService, never()).getDashboard();
        }

        @Test
        void roleClaimInTokenIsIgnoredInFavourOfDatabaseRole() throws Exception {
            // The token claims ADMIN, but the database says this user is a STUDENT.
            mockMvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(STUDENT, "ADMIN")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void adminCanReachAdminApi() throws Exception {
            when(adminService.getDashboard()).thenReturn(new AdminDashboardResponse(1, 2, 3, 4, 5, 6));

            mockMvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(ADMIN, "ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalJobs").value(4));
        }
    }

    @Nested
    class InterviewSlotEndpoints {

        private static final String RECRUITER = "recruiter@example.com";

        @BeforeEach
        void recruiterUser() {
            when(userDetailsService.loadUserByUsername(RECRUITER)).thenReturn(
                    User.withUsername(RECRUITER).password("x").authorities("ROLE_RECRUITER").build());
        }

        @Test
        void studentBookingReturns201() throws Exception {
            when(interviewSlotService.bookSlot(7L, 3L)).thenReturn(new com.placementhub.backend.dto.ApplicationResponse());

            mockMvc.perform(post("/api/students/applications/7/slots/3/booking")
                            .header("Authorization", bearer(STUDENT, "STUDENT")))
                    .andExpect(status().isCreated());
        }

        @Test
        void fullSlotIs409() throws Exception {
            when(interviewSlotService.bookSlot(7L, 3L))
                    .thenThrow(new ConflictException("SLOT_FULL", "This interview slot is already full."));

            mockMvc.perform(post("/api/students/applications/7/slots/3/booking")
                            .header("Authorization", bearer(STUDENT, "STUDENT")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SLOT_FULL"));
        }

        @Test
        void studentCannotCreateSlots() throws Exception {
            mockMvc.perform(post("/api/recruiter/jobs/1/slots")
                            .header("Authorization", bearer(STUDENT, "STUDENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startsAt\":\"2030-01-01T10:00:00\",\"durationMinutes\":30,\"capacity\":2}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void slotCapacityIsValidated() throws Exception {
            mockMvc.perform(post("/api/recruiter/jobs/1/slots")
                            .header("Authorization", bearer(RECRUITER, "RECRUITER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startsAt\":\"2030-01-01T10:00:00\",\"durationMinutes\":30,\"capacity\":0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.capacity").exists());
        }

        @Test
        void recruiterCreatesSlotWith201() throws Exception {
            when(interviewSlotService.createSlot(eq(1L), any())).thenReturn(new com.placementhub.backend.dto.InterviewSlotResponse());

            mockMvc.perform(post("/api/recruiter/jobs/1/slots")
                            .header("Authorization", bearer(RECRUITER, "RECRUITER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startsAt\":\"2030-01-01T10:00:00\",\"durationMinutes\":30,\"capacity\":2,\"location\":\"Room 4\"}"))
                    .andExpect(status().isCreated());
        }

        @Test
        void closeJobEndpointIsWired() throws Exception {
            when(recruiterService.closeJob(5L)).thenReturn(new com.placementhub.backend.dto.JobResponse());

            mockMvc.perform(post("/api/recruiter/jobs/5/close").header("Authorization", bearer(RECRUITER, "RECRUITER")))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class ErrorMapping {

        @Test
        void notFoundFromServiceIs404() throws Exception {
            when(studentService.getApplicationDetails(anyLong()))
                    .thenThrow(new NotFoundException("Application not found"));

            mockMvc.perform(get("/api/students/applications/99").header("Authorization", bearer(STUDENT, "STUDENT")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Application not found"));
        }

        @Test
        void unexpectedExceptionIs500WithoutLeakingDetails() throws Exception {
            when(studentService.getProfile()).thenThrow(new IllegalStateException("SQL: secret internals"));

            mockMvc.perform(get("/api/students/profile").header("Authorization", bearer(STUDENT, "STUDENT")))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.error").value("Something went wrong. Please try again."));
        }

        @Test
        void invalidJobApprovalStatusIsRejected() throws Exception {
            mockMvc.perform(put("/api/admin/jobs/1/approve")
                            .header("Authorization", bearer(ADMIN, "ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"FOO\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.status").value("Status must be APPROVED or REJECTED"));
        }

        @Test
        void outOfRangeCgpaIsRejected() throws Exception {
            mockMvc.perform(put("/api/students/profile")
                            .header("Authorization", bearer(STUDENT, "STUDENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"S\",\"cgpa\":11}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.cgpa").exists());
        }

        @Test
        void adminListsUseStablePageShape() throws Exception {
            when(adminService.getAllStudents(anyInt(), anyInt()))
                    .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

            mockMvc.perform(get("/api/admin/students").header("Authorization", bearer(ADMIN, "ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0));
        }
    }
}
