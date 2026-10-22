package com.placementhub.backend.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/** Token bucket behaviour with a fake clock, so the tests are instant and exact. */
class RateLimiterTest {

    private final AtomicLong now = new AtomicLong(1_000_000_000L);
    private final RateLimiter limiter = new RateLimiter(now::get);
    private final RateLimitRule threePerMinute =
            new RateLimitRule("login", "POST", "/api/auth/login", 3, Duration.ofMinutes(1), false);

    private void advance(Duration d) {
        now.addAndGet(d.toNanos());
    }

    @Test
    void allowsABurstUpToCapacityThenRefuses() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire(threePerMinute, "1.2.3.4")).isZero();
        }
        Duration wait = limiter.tryAcquire(threePerMinute, "1.2.3.4");
        // One token refills every 20 seconds (3 per minute).
        assertThat(wait).isPositive().isLessThanOrEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void refillsGraduallyInsteadOfResettingAtAWindowEdge() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire(threePerMinute, "1.2.3.4");
        }
        advance(Duration.ofSeconds(19));
        assertThat(limiter.tryAcquire(threePerMinute, "1.2.3.4")).isPositive();
        advance(Duration.ofSeconds(2));
        assertThat(limiter.tryAcquire(threePerMinute, "1.2.3.4")).isZero();
        // Only one token came back, not the whole allowance.
        assertThat(limiter.tryAcquire(threePerMinute, "1.2.3.4")).isPositive();
    }

    @Test
    void neverStoresMoreThanCapacity() {
        advance(Duration.ofHours(5));
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire(threePerMinute, "1.2.3.4")).isZero();
        }
        assertThat(limiter.tryAcquire(threePerMinute, "1.2.3.4")).isPositive();
    }

    @Test
    void clientsAndRulesHaveSeparateBuckets() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire(threePerMinute, "1.2.3.4");
        }
        assertThat(limiter.tryAcquire(threePerMinute, "1.2.3.4")).isPositive();
        assertThat(limiter.tryAcquire(threePerMinute, "5.6.7.8")).isZero();

        RateLimitRule register = new RateLimitRule("register", "POST", "/api/auth/register", 1, Duration.ofMinutes(10), false);
        assertThat(limiter.tryAcquire(register, "1.2.3.4")).isZero();
    }

    @Test
    void idleBucketsAreEvictedSoMemoryDoesNotGrowForever() {
        for (int i = 0; i < 100; i++) {
            limiter.tryAcquire(threePerMinute, "10.0.0." + i);
        }
        assertThat(limiter.bucketCount()).isEqualTo(100);

        advance(Duration.ofMinutes(31));
        limiter.tryAcquire(threePerMinute, "fresh-client");

        assertThat(limiter.bucketCount()).isEqualTo(1);
    }

    @Test
    void clientIpDropsAPortButKeepsIpv6() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.5:51234");
        assertThat(RateLimitFilter.clientIp(request)).isEqualTo("203.0.113.5");

        request.setRemoteAddr("203.0.113.5");
        assertThat(RateLimitFilter.clientIp(request)).isEqualTo("203.0.113.5");

        request.setRemoteAddr("2001:db8::1");
        assertThat(RateLimitFilter.clientIp(request)).isEqualTo("2001:db8::1");
    }
}
