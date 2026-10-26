package com.placementhub.backend.mapper;

import com.placementhub.backend.dto.ApplicantSummary;
import com.placementhub.backend.dto.ApplicationResponse;
import com.placementhub.backend.dto.InterviewBookingSummary;
import com.placementhub.backend.dto.TimelineEntryResponse;
import com.placementhub.backend.entity.Application;
import com.placementhub.backend.entity.ApplicationTimeline;
import com.placementhub.backend.entity.InterviewBooking;
import com.placementhub.backend.entity.Student;
import com.placementhub.backend.repository.ApplicationTimelineRepository;
import com.placementhub.backend.repository.InterviewBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Application entity → API response, including its status timeline and interview booking.
 *
 * Timelines and bookings for a whole list of applications are each loaded with a single IN
 * query rather than one query per application. Reads application.job and job.company (and
 * application.student.user for the recruiter view), so callers should fetch those with the
 * applications.
 */
@Component
@RequiredArgsConstructor
public class ApplicationMapper {

    private final ApplicationTimelineRepository timelineRepository;
    private final InterviewBookingRepository bookingRepository;

    /** Student view: no applicant block. */
    public ApplicationResponse toResponse(Application application) {
        return toResponses(List.of(application)).get(0);
    }

    public List<ApplicationResponse> toResponses(List<Application> applications) {
        return map(applications, false);
    }

    /** Recruiter view: also includes who applied. */
    public ApplicationResponse toRecruiterResponse(Application application) {
        return toRecruiterResponses(List.of(application)).get(0);
    }

    public List<ApplicationResponse> toRecruiterResponses(List<Application> applications) {
        return map(applications, true);
    }

    private List<ApplicationResponse> map(List<Application> applications, boolean includeApplicant) {
        if (applications.isEmpty()) {
            return List.of();
        }

        List<Long> ids = applications.stream().map(Application::getId).toList();
        // Grouping keeps the query's changed_at order within each application.
        Map<Long, List<TimelineEntryResponse>> timelines = timelineRepository
                .findByApplicationIdInOrderByChangedAtAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        t -> t.getApplication().getId(),
                        Collectors.mapping(this::toTimelineEntry, Collectors.toList())));
        Map<Long, InterviewBooking> bookings = bookingRepository.findByApplicationIdIn(ids).stream()
                .collect(Collectors.toMap(b -> b.getApplication().getId(), Function.identity()));

        return applications.stream()
                .map(application -> {
                    ApplicationResponse response = toResponse(application,
                            timelines.getOrDefault(application.getId(), List.of()),
                            bookings.get(application.getId()));
                    if (includeApplicant) {
                        response.setApplicant(toApplicant(application.getStudent()));
                    }
                    return response;
                })
                .toList();
    }

    private ApplicationResponse toResponse(Application application, List<TimelineEntryResponse> timeline,
                                           InterviewBooking booking) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId());
        response.setJobId(application.getJob().getId());
        response.setJobTitle(application.getJob().getTitle());
        response.setCompanyName(application.getJob().getCompany().getName());
        response.setStatus(application.getStatus().name());
        response.setAppliedAt(application.getAppliedAt());
        response.setUpdatedAt(application.getUpdatedAt());
        response.setTimeline(timeline);
        response.setVersion(application.getVersion());
        response.setOfferExpiresAt(application.getOfferExpiresAt());
        response.setWaitlistRank(application.getWaitlistRank());
        response.setAllowedNextStatuses(application.getStatus().allowedNext().stream().map(Enum::name).toList());
        if (booking != null) {
            response.setInterview(toBooking(booking));
        }
        return response;
    }

    private ApplicantSummary toApplicant(Student student) {
        ApplicantSummary applicant = new ApplicantSummary();
        applicant.setStudentId(student.getId());
        applicant.setFullName(student.getFullName());
        applicant.setEmail(student.getUser().getEmail());
        applicant.setCollege(student.getCollege());
        applicant.setDegree(student.getDegree());
        applicant.setBranch(student.getBranch());
        applicant.setGraduationYear(student.getGraduationYear());
        applicant.setCgpa(student.getCgpa());
        applicant.setSkills(student.getSkills());
        applicant.setResumeUrl(student.getResumeUrl());
        applicant.setLinkedinUrl(student.getLinkedinUrl());
        applicant.setGithubUrl(student.getGithubUrl());
        return applicant;
    }

    private InterviewBookingSummary toBooking(InterviewBooking booking) {
        InterviewBookingSummary summary = new InterviewBookingSummary();
        summary.setSlotId(booking.getSlot().getId());
        summary.setStartsAt(booking.getSlot().getStartsAt());
        summary.setDurationMinutes(booking.getSlot().getDurationMinutes());
        summary.setLocation(booking.getSlot().getLocation());
        summary.setBookedAt(booking.getBookedAt());
        return summary;
    }

    private TimelineEntryResponse toTimelineEntry(ApplicationTimeline entry) {
        TimelineEntryResponse response = new TimelineEntryResponse();
        response.setStatus(entry.getStatus());
        response.setRemarks(entry.getRemarks());
        response.setChangedAt(entry.getChangedAt());
        return response;
    }
}
