package com.placementhub.backend.ratelimit;

/**
 * Classic token bucket. The bucket holds up to {@code capacity} tokens and refills
 * continuously at {@code capacity / refillPeriodNanos}. Each request takes one token;
 * with none left the request is refused and the caller learns how long until the next
 * token arrives.
 *
 * Unlike a fixed "N per minute" window, a bucket allows a short burst (up to capacity)
 * and then a steady rate, and there is no window edge where 2N requests slip through.
 *
 * Thread-safe: one bucket is shared by all requests from the same client.
 */
final class TokenBucket {

    private final long capacity;
    private final double tokensPerNano;
    private double tokens;
    private long lastRefillNanos;
    private long lastUsedNanos;

    TokenBucket(long capacity, long refillPeriodNanos, long nowNanos) {
        this.capacity = capacity;
        this.tokensPerNano = (double) capacity / refillPeriodNanos;
        this.tokens = capacity;
        this.lastRefillNanos = nowNanos;
        this.lastUsedNanos = nowNanos;
    }

    /** @return 0 if a token was taken, otherwise nanoseconds until one becomes available. */
    synchronized long tryConsume(long nowNanos) {
        refill(nowNanos);
        lastUsedNanos = nowNanos;
        if (tokens >= 1) {
            tokens -= 1;
            return 0;
        }
        return (long) Math.ceil((1 - tokens) / tokensPerNano);
    }

    synchronized boolean isIdleSince(long cutoffNanos) {
        return lastUsedNanos < cutoffNanos;
    }

    private void refill(long nowNanos) {
        long elapsed = nowNanos - lastRefillNanos;
        if (elapsed > 0) {
            tokens = Math.min(capacity, tokens + elapsed * tokensPerNano);
            lastRefillNanos = nowNanos;
        }
    }
}
