package com.placementhub.backend.service;

import com.placementhub.backend.dto.ApplicationResponse;
import com.placementhub.backend.entity.Application;
import com.placementhub.backend.entity.ApplicationTimeline;
import com.placementhub.backend.entity.Job;
import com.placementhub.backend.entity.Student;
import com.placementhub.backend.enums.ApplicationStatus;
import com.placementhub.backend.exception.BusinessRuleException;
import com.placementhub.backend.exception.ConflictException;
import com.placementhub.backend.mapper.ApplicationMapper;
import com.placementhub.backend.repository.ApplicationRepository;
import com.placementhub.backend.repository.ApplicationTimelineRepository;
import com.placementhub.backend.repository.JobRepository;
import com.placementhub.backend.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Offer Desk: offers with openings and deadlines, accepted or declined by the student.
 *
 * Invariants, each held by the database rather than by a check in Java:
 * - pending + accepted offers never exceed a job's openings (atomic seat counter + CHECK);
 * - a student holds at most one pending offer and is placed at most once (conditional
 *   UPDATE on the student row + unique keys);
 * - an offer ends exactly once: accepted, declined or expired (the application's @Version).
 *
 * Every method changes the application first and flushes, so the @Version check decides
 * who wins a race before any counter is touched; a losing request rolls back as a whole.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfferService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationTimelineRepository timelineRepository;
    private final JobRepository jobRepository;
    private final StudentRepository studentRepository;
    private final StudentService studentService;
    private final ApplicationMapper applicationMapper;
    private final WaitlistService waitlistService;
    private final Clock clock;

    /**
     * Recruiter side, called by RecruiterService inside its status-change transaction just
     * before the application is saved as OFFERED: takes an opening and the student's
     * single pending-offer place, and sets the deadline.
     */
    public void prepareOffer(Application application) {
        Job job = application.getJob();
        Student student = application.getStudent();

        if (jobRepository.reserveOfferSeat(job.getId()) == 0) {
            throw new ConflictException("NO_OPENINGS_LEFT",
                    "All " + job.getOpenings() + " opening" + (job.getOpenings() == 1 ? " is" : "s are")
                            + " already filled or offered. Increase the openings to make another offer.");
        }
        if (studentRepository.claimPendingOffer(student.getId(), application.getId()) == 0) {
            // Rolled back with the seat above. Tell the recruiter which rule refused it.
            Student current = studentRepository.findById(student.getId()).orElseThrow();
            if (current.getPlacedApplicationId() != null) {
                throw new BusinessRuleException("STUDENT_ALREADY_PLACED",
                        "This student has already accepted another offer.");
            }
            throw new ConflictException("STUDENT_HAS_PENDING_OFFER",
                    "This student is deciding on another offer. You can make an offer once they reply or it expires.");
        }
        application.setOfferExpiresAt(LocalDateTime.now(clock).plusHours(job.getOfferDeadlineHours()));
    }

    @Transactional
    public ApplicationResponse accept(Long applicationId) {
        Application application = requirePendingOffer(applicationId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!application.getOfferExpiresAt().isAfter(now)) {
            throw new BusinessRuleException("OFFER_EXPIRED", "This offer expired on " + application.getOfferExpiresAt() + ".");
        }

        decide(application, ApplicationStatus.ACCEPTED, "Offer accepted", now);
        if (studentRepository.placeWithPendingOffer(application.getStudent().getId(), application.getId()) == 0
                || jobRepository.convertOfferSeat(application.getJob().getId()) == 0) {
            // Cannot happen while the invariants hold; refuse rather than corrupt the counters.
            throw new ConflictException("OFFER_NO_LONGER_OPEN", "This offer is no longer open. Refresh the page.");
        }
        return applicationMapper.toResponse(application);
    }

    @Transactional
    public ApplicationResponse decline(Long applicationId) {
        Application application = requirePendingOffer(applicationId);
        decide(application, ApplicationStatus.DECLINED, "Offer declined", LocalDateTime.now(clock));
        releaseSeatAndStudent(application);
        return applicationMapper.toResponse(application);
    }

    /**
     * Expires one overdue offer (called by OfferExpiryJob, one transaction per offer).
     * Returns false if it is no longer pending or not yet due. Safe to run on several
     * servers at once: of two attempts on the same offer, the second fails the @Version
     * check and changes nothing, so no distributed lock is needed.
     */
    @Transactional
    public boolean expire(Long applicationId) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);
        if (application == null || application.getStatus() != ApplicationStatus.OFFERED
                || application.getOfferExpiresAt().isAfter(now)) {
            return false;
        }
        decide(application, ApplicationStatus.EXPIRED, "Offer expired: no reply before the deadline", now);
        releaseSeatAndStudent(application);
        return true;
    }

    private Application requirePendingOffer(Long applicationId) {
        Application application = studentService.requireOwnApplication(applicationId);
        if (application.getStatus() != ApplicationStatus.OFFERED) {
            throw new BusinessRuleException("OFFER_NOT_PENDING",
                    "There is no open offer on this application (it is " + application.getStatus() + ").");
        }
        return application;
    }

    /** Moves the offer to its final status. The flush is where a concurrent decision loses. */
    private void decide(Application application, ApplicationStatus outcome, String remarks, LocalDateTime now) {
        application.setStatus(outcome);
        application.setUpdatedAt(now);
        applicationRepository.saveAndFlush(application);

        ApplicationTimeline entry = new ApplicationTimeline();
        entry.setApplication(application);
        entry.setStatus(outcome.name());
        entry.setRemarks(remarks);
        timelineRepository.save(entry);
    }

    /**
     * Frees the opening and the student's pending-offer place, then (phase 2) offers the
     * opening to the job's waitlist and retries jobs that are waiting for this student.
     */
    private void releaseSeatAndStudent(Application application) {
        jobRepository.releaseOfferSeat(application.getJob().getId());
        studentRepository.releasePendingOffer(application.getStudent().getId(), application.getId());
        waitlistService.fillOpenings(application.getJob());
        waitlistService.retryJobsWaitlisting(application.getStudent().getId());
    }
}
