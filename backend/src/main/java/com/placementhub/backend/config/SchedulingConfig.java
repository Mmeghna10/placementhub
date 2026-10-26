package com.placementhub.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on @Scheduled background jobs (currently only OfferExpiryJob). Each job can be
 * switched off on its own with a property, e.g. app.offers.expiry-job.enabled=false.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
