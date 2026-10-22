package com.placementhub.backend.ratelimit;

import java.time.Duration;

/**
 * "At most {@code capacity} requests per {@code refillPeriod}" for one method + path.
 * byUser = true keys the bucket by the logged-in user, otherwise by client IP address.
 */
public record RateLimitRule(String name, String method, String path, int capacity,
                            Duration refillPeriod, boolean byUser) {

    boolean matches(String requestMethod, String requestPath) {
        return method.equalsIgnoreCase(requestMethod) && path.equals(requestPath);
    }
}
