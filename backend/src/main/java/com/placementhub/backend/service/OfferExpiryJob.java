package com.placementhub.backend.service;

import com.placementhub.backend.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Expires offers whose deadline has passed, so their openings go back to the job.
 *
 * Runs every minute (app.offers.expiry-check-ms). Each offer is expired in its own short
 * transaction by OfferService.expire. If a student accepts at the same moment, or another
 * server runs this job too, one side wins the application's @Version check and the other
 * changes nothing; that loss is expected and only logged. No distributed lock is needed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.offers.expiry-job.enabled", havingValue = "true", matchIfMissing = true)
public class OfferExpiryJob {

    /** Upper bound per run, so one run stays short; the next run picks up the rest. */
    private static final int BATCH_SIZE = 200;

    private final ApplicationRepository applicationRepository;
    private final OfferService offerService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.offers.expiry-check-ms:60000}", initialDelayString = "${app.offers.expiry-check-ms:60000}")
    public void expireOverdueOffers() {
        List<Long> due = applicationRepository.findDueOfferIds(LocalDateTime.now(clock), PageRequest.of(0, BATCH_SIZE));
        int expired = 0;
        for (Long applicationId : due) {
            try {
                if (offerService.expire(applicationId)) {
                    expired++;
                }
            } catch (ConcurrencyFailureException e) {
                log.info("Offer on application {} was decided concurrently; left as is", applicationId);
            }
        }
        if (expired > 0) {
            log.info("Expired {} overdue offer(s)", expired);
        }
    }
}
