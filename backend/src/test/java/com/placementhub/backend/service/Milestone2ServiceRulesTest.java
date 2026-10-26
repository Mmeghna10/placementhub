package com.placementhub.backend.service;

import com.placementhub.backend.dto.JobRequest;
import com.placementhub.backend.dto.RecruiterProfileUpdateRequest;
import com.placementhub.backend.dto.RegisterRequest;
import com.placementhub.backend.entity.Application;
import com.placementhub.backend.entity.Company;
import com.placementhub.backend.entity.Job;
import com.placementhub.backend.entity.Recruiter;
import com.placementhub.backend.entity.Student;
import com.placementhub.backend.entity.User;
import com.placementhub.backend.enums.JobStatus;
import com.placementhub.backend.enums.Role;
import com.placementhub.backend.exception.BusinessRuleException;
import com.placementhub.backend.exception.ConflictException;
import com.placementhub.backend.exception.ForbiddenException;
import com.placementhub.backend.exception.NotFoundException;
import com.placementhub.backend.mapper.ApplicationMapper;
import com.placementhub.backend.mapper.JobMapper;
import com.placementhub.backend.repository.*;
import com.placementhub.backend.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests (Mockito, no Spring context) for the business rules added in Milestone 2. */
@ExtendWith(MockitoExtension.class)
class Milestone2ServiceRulesTest {

    @Nested
    class AuthServiceRules {

        @Mock UserRepository userRepository;
        @Mock StudentRepository studentRepository;
        @Mock RecruiterRepository recruiterRepository;
        @Mock PasswordEncoder passwordEncoder;
        @Mock JwtUtil jwtUtil;
        @Mock AuthenticationManager authenticationManager;
        @InjectMocks AuthService authService;

        @Test
        void adminRegistrationIsRefusedEvenWithoutControllerValidation() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("evil@example.com");
            request.setPassword("secret123");
            request.setFullName("Evil");
            request.setRole(Role.ADMIN);

            assertThatThrownBy(() -> authService.register(request)).isInstanceOf(ForbiddenException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        void duplicateEmailIsAConflict() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("dup@example.com");
            request.setRole(Role.STUDENT);
            when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Email already registered");
        }
    }

    @Nested
    class RecruiterServiceRules {

        private static final String EMAIL = "rec@example.com";

        @Mock RecruiterRepository recruiterRepository;
        @Mock CompanyRepository companyRepository;
        @Mock JobRepository jobRepository;
        @Mock ApplicationRepository applicationRepository;
        @Mock ApplicationTimelineRepository timelineRepository;
        @Spy JobMapper jobMapper = new JobMapper();
        @Mock ApplicationMapper applicationMapper;
        @Mock InterviewSlotRepository slotRepository;
        @Mock OfferService offerService;
        @Mock WaitlistService waitlistService;
        @Mock Clock clock;
        @InjectMocks RecruiterService recruiterService;

        private Recruiter recruiter;

        @BeforeEach
        void loginAsRecruiter() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(EMAIL, null, List.of()));

            User user = new User();
            user.setEmail(EMAIL);
            recruiter = new Recruiter();
            recruiter.setId(1L);
            recruiter.setUser(user);
            when(recruiterRepository.findByUser_Email(EMAIL)).thenReturn(Optional.of(recruiter));
        }

        @AfterEach
        void clearContext() {
            SecurityContextHolder.clearContext();
        }

        private Job ownedJob(JobStatus status) {
            Company company = new Company();
            company.setId(5L);
            company.setName("Acme");
            Job job = new Job();
            job.setId(10L);
            job.setRecruiter(recruiter);
            job.setCompany(company);
            job.setStatus(status);
            return job;
        }

        @Test
        void editingAnApprovedJobSendsItBackForReview() {
            Job job = ownedJob(JobStatus.APPROVED);
            when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
            when(jobRepository.saveAndFlush(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

            JobRequest request = new JobRequest();
            request.setTitle("Edited title");

            assertThat(recruiterService.updateJob(10L, request).getStatus()).isEqualTo("PENDING");
        }

        @Test
        void anotherRecruitersJobLooksLikeItDoesNotExist() {
            Recruiter other = new Recruiter();
            other.setId(2L);
            Job job = ownedJob(JobStatus.APPROVED);
            job.setRecruiter(other);
            when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> recruiterService.updateJob(10L, new JobRequest()))
                    .isInstanceOf(NotFoundException.class);
            verify(jobRepository, never()).save(any());
        }

        @Test
        void jobWithApplicationsCannotBeDeleted() {
            when(jobRepository.findById(10L)).thenReturn(Optional.of(ownedJob(JobStatus.APPROVED)));
            when(applicationRepository.existsByJobId(10L)).thenReturn(true);

            assertThatThrownBy(() -> recruiterService.deleteJob(10L))
                    .isInstanceOf(ConflictException.class);
            verify(jobRepository, never()).delete(any());
        }

        @Test
        void savingPersonalProfileWithoutCompanyDoesNotCreateAnEmptyCompany() {
            when(recruiterRepository.save(any(Recruiter.class))).thenAnswer(inv -> inv.getArgument(0));

            RecruiterProfileUpdateRequest request = new RecruiterProfileUpdateRequest();
            request.setFullName("Rita Recruiter");

            recruiterService.updateProfile(request);

            verify(companyRepository, never()).save(any());
            assertThat(recruiter.getCompany()).isNull();
        }

        @Test
        void existingCompanyNameCannotBeBlanked() {
            Company company = new Company();
            company.setName("Acme");
            recruiter.setCompany(company);

            RecruiterProfileUpdateRequest request = new RecruiterProfileUpdateRequest();
            request.setCompanyName("  ");

            assertThatThrownBy(() -> recruiterService.updateProfile(request))
                    .isInstanceOf(BusinessRuleException.class);
            assertThat(company.getName()).isEqualTo("Acme");
        }
    }

    @Nested
    class StudentServiceRules {

        private static final String EMAIL = "stu@example.com";

        @Mock StudentRepository studentRepository;
        @Mock JobRepository jobRepository;
        @Mock ApplicationRepository applicationRepository;
        @Mock ApplicationTimelineRepository timelineRepository;
        @Mock JobMapper jobMapper;
        @Mock ApplicationMapper applicationMapper;
        @Mock EligibilityPolicy eligibilityPolicy;
        @Mock Clock clock;
        @Mock PlacementRules placementRules;
        @InjectMocks StudentService studentService;

        @BeforeEach
        void loginAsStudent() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(EMAIL, null, List.of()));

            Student me = new Student();
            me.setId(1L);
            when(studentRepository.findByUser_Email(EMAIL)).thenReturn(Optional.of(me));
        }

        @AfterEach
        void clearContext() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void anotherStudentsApplicationLooksLikeItDoesNotExist() {
            Student other = new Student();
            other.setId(2L);
            Application theirs = new Application();
            theirs.setId(50L);
            theirs.setStudent(other);
            when(applicationRepository.findById(50L)).thenReturn(Optional.of(theirs));

            // Same 404 as for an id that does not exist, so ids cannot be probed.
            assertThatThrownBy(() -> studentService.getApplicationDetails(50L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Application not found");
            verify(applicationMapper, never()).toResponse(any());
        }
    }

    @Nested
    class AdminServiceRules {

        @Mock StudentRepository studentRepository;
        @Mock RecruiterRepository recruiterRepository;
        @Mock CompanyRepository companyRepository;
        @Mock JobRepository jobRepository;
        @Mock ApplicationRepository applicationRepository;
        @Spy JobMapper jobMapper = new JobMapper();
        @InjectMocks AdminService adminService;

        @Test
        void pageSizeAndPageAreClamped() {
            when(studentRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

            adminService.getAllStudents(-3, 10_000);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(studentRepository).findAll(captor.capture());
            assertThat(captor.getValue().getPageNumber()).isZero();
            assertThat(captor.getValue().getPageSize()).isEqualTo(AdminService.MAX_PAGE_SIZE);
        }
    }

    @Nested
    class JwtUtilRules {

        @Test
        void shortSecretFailsFastAtStartup() {
            JwtUtil jwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "too-short");
            ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 1000L);

            assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(jwtUtil, "init"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least 32 bytes");
        }

        @Test
        void generatedTokenRoundTrips() {
            JwtUtil jwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "a-secret-that-is-long-enough-for-hs256-ok");
            ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 60_000L);
            ReflectionTestUtils.invokeMethod(jwtUtil, "init");

            String token = jwtUtil.generateToken("a@example.com", "STUDENT");

            assertThat(jwtUtil.parseAndValidate(token).getSubject()).isEqualTo("a@example.com");
        }
    }
}
