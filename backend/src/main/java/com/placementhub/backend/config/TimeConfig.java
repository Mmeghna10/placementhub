package com.placementhub.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    /**
     * The placement cell's local time. Deadlines are dates and interview slots are local
     * times, so "today" and "now" must not depend on the server's zone (Azure runs in UTC).
     * Injecting a Clock also lets tests fix the time.
     */
    @Bean
    public Clock clock(@Value("${app.timezone:Asia/Kolkata}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
