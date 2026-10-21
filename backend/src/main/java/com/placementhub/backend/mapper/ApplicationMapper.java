package com.placementhub.backend.mapper;

import com.placementhub.backend.dto.ApplicationResponse;
import com.placementhub.backend.dto.TimelineEntryResponse;
import com.placementhub.backend.entity.Application;
import com.placementhub.backend.entity.ApplicationTimeline;
import com.placementhub.backend.repository.ApplicationTimelineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application entity → API response, including its status timeline.
 *
 * Timelines for a whole list of applications are loaded with a single IN query rather
 * than one query per application (the previous N+1). Reads application.job and
 * job.company, so callers should fetch those with the applications.
 */
@Component
@RequiredArgsConstructor
public class ApplicationMapper {

    private final ApplicationTimelineRepository timelineRepository;

    public ApplicationResponse toResponse(Application application) {
        return toResponses(List.of(application)).get(0);
    }

    public List<ApplicationResponse> toResponses(List<Application> applications) {
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

        return applications.stream()
                .map(application -> toResponse(application, timelines.getOrDefault(application.getId(), List.of())))
                .toList();
    }

    private ApplicationResponse toResponse(Application application, List<TimelineEntryResponse> timeline) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId());
        response.setJobId(application.getJob().getId());
        response.setJobTitle(application.getJob().getTitle());
        response.setCompanyName(application.getJob().getCompany().getName());
        response.setStatus(application.getStatus());
        response.setAppliedAt(application.getAppliedAt());
        response.setUpdatedAt(application.getUpdatedAt());
        response.setTimeline(timeline);
        return response;
    }

    private TimelineEntryResponse toTimelineEntry(ApplicationTimeline entry) {
        TimelineEntryResponse response = new TimelineEntryResponse();
        response.setStatus(entry.getStatus());
        response.setRemarks(entry.getRemarks());
        response.setChangedAt(entry.getChangedAt());
        return response;
    }
}
