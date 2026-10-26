package com.placementhub.backend.service;

import com.placementhub.backend.entity.Application;
import com.placementhub.backend.entity.ApplicationTimeline;
import com.placementhub.backend.entity.Job;
import com.placementhub.backend.enums.ApplicationStatus;
import com.placementhub.backend.exception.ConflictException;
import com.placementhub.backend.repository.ApplicationRepository;
import com.placementhub.backend.repository.ApplicationTimelineRepository;
import com.placementhub.backend.repository.JobRepository;
import com.placementhub.backend.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * Offer Desk phase 2: a ranked waitlist per job that refills openings automatically.
 *
 * {@link #fillOpenings} runs inside the transaction that freed an opening (a decline, an
 * expiry, or the recruiter's "fill" action). For each free opening it offers the job to
 * the next waitlisted candidate who is free to take an offer. Every step is a conditional
 * UPDATE: the seat counter, the student's single pending-offer place, and the
 * WAITLISTED → OFFERED change. So two promotions racing for the same opening or the same
 * candidate cannot both win, and losing just means "try the next candidate". A losing
 * promotion never fails the decline or expiry that triggered it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WaitlistService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationTimelineRepository timelineRepository;
    private final JobRepository jobRepository;
    private final StudentRepository studentRepository;
    private final PlacementRules placementRules;
    private final Clock clock;

    /** Offers this job's free openings to waitlisted candidates in rank order. Returns how many were offered. */
    @Transactional
    public int fillOpenings(Job job) {
        int offered = 0;
        for (Application candidate : applicationRepository.findWaitlist(job.getId())) {
            if (jobRepository.reserveOfferSeat(job.getId()) == 0) {
                break; // no free opening left
            }
            if (offerTo(job, candidate)) {
                offered++;
            } else {
                jobRepository.releaseOfferSeat(job.getId());
            }
        }
        return offered;
    }

    /**
     * A student just became free (their pending offer was declined or expired). Jobs that
     * skipped them earlier, because they were holding that offer, get another chance.
     */
    @Transactional
    public void retryJobsWaitlisting(Long studentId) {
        for (Long jobId : new HashSet<>(applicationRepository.findJobIdsWaitlisting(studentId))) {
            jobRepository.findById(jobId).ifPresent(this::fillOpenings);
        }
    }

    /** Recruiter puts an interviewed candidate at the end of the waitlist. */
    void addToWaitlist(Application application) {
        application.setWaitlistRank(applicationRepository.maxWaitlistRank(application.getJob().getId()) + 1);
    }

    /**
     * Recruiter reorders the waitlist. The list must contain exactly the applications that
     * are waitlisted right now; if someone was promoted or added meanwhile, the recruiter
     * is asked to refresh instead of saving a stale order.
     */
    @Transactional
    public void reorder(Job job, List<Long> applicationIds) {
        List<Application> waitlist = applicationRepository.findWaitlist(job.getId());
        if (applicationIds.size() != waitlist.size()
                || !new HashSet<>(applicationIds).equals(new HashSet<>(waitlist.stream().map(Application::getId).toList()))) {
            throw new ConflictException("WAITLIST_CHANGED", "The waitlist changed in the meantime. Refresh and try again.");
        }
        for (Application application : waitlist) {
            application.setWaitlistRank(applicationIds.indexOf(application.getId()) + 1);
        }
        applicationRepository.saveAllAndFlush(waitlist);
    }

    /** One promotion attempt. False if the candidate is busy, placed, or was just promoted elsewhere. */
    private boolean offerTo(Job job, Application candidate) {
        Long studentId = candidate.getStudent().getId();
        if (studentRepository.claimPendingOffer(studentId, candidate.getId(), job.getTier().rank(),
                placementRules.maxAcceptedOffers()) == 0) {
            return false; // holding another offer, or the placement rules don't allow this job
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (applicationRepository.promoteFromWaitlist(candidate.getId(), now.plusHours(job.getOfferDeadlineHours()), now) == 0) {
            studentRepository.releasePendingOffer(studentId, candidate.getId());
            return false; // no longer waitlisted (a concurrent promotion or rejection won)
        }

        ApplicationTimeline entry = new ApplicationTimeline();
        entry.setApplication(candidate);
        entry.setStatus(ApplicationStatus.OFFERED.name());
        entry.setRemarks("Offered automatically from the waitlist when an opening became free");
        timelineRepository.save(entry);
        log.info("Waitlist promotion: application {} offered job {}", candidate.getId(), job.getId());
        return true;
    }
}
