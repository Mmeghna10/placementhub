package com.placementhub.backend.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Keeps one TokenBucket per (rule, client) in memory.
 *
 * In-memory means the limits apply per backend instance: with two instances behind a
 * load balancer a client could get up to twice the limit. That is acceptable for one
 * App Service instance; with several, the buckets would move to Redis (shared state).
 *
 * Idle buckets are dropped periodically, so the map does not grow without bound as new
 * IP addresses appear.
 */
public class RateLimiter {

    private static final long IDLE_EVICTION_NANOS = TimeUnit.MINUTES.toNanos(30);
    private static final long CLEANUP_EVERY_NANOS = TimeUnit.MINUTES.toNanos(5);

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final LongSupplier nanoClock;
    private volatile long nextCleanupNanos;

    public RateLimiter() {
        this(System::nanoTime);
    }

    RateLimiter(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
        this.nextCleanupNanos = nanoClock.getAsLong() + CLEANUP_EVERY_NANOS;
    }

    /**
     * Takes one request from the client's allowance for this rule.
     *
     * @return Duration.ZERO if allowed, otherwise how long the client should wait.
     */
    public Duration tryAcquire(RateLimitRule rule, String clientKey) {
        long now = nanoClock.getAsLong();
        evictIdleBuckets(now);
        TokenBucket bucket = buckets.computeIfAbsent(rule.name() + "|" + clientKey,
                k -> new TokenBucket(rule.capacity(), rule.refillPeriod().toNanos(), now));
        return Duration.ofNanos(bucket.tryConsume(now));
    }

    int bucketCount() {
        return buckets.size();
    }

    private void evictIdleBuckets(long now) {
        if (now < nextCleanupNanos) {
            return;
        }
        nextCleanupNanos = now + CLEANUP_EVERY_NANOS;
        long cutoff = now - IDLE_EVICTION_NANOS;
        buckets.values().removeIf(bucket -> bucket.isIdleSince(cutoff));
    }
}
