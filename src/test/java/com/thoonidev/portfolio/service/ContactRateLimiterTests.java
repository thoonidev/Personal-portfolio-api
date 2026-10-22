package com.thoonidev.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.thoonidev.portfolio.config.ContactProperties;

class ContactRateLimiterTests {
    @Test
    void separatesClientsAndResetsAfterTheWindowExpires() {
        MutableClock clock = new MutableClock();
        ContactRateLimiter limiter = new ContactRateLimiter(properties(2, 10), clock);
        assertThat(limiter.retryAfter("a")).isZero();
        assertThat(limiter.retryAfter("a")).isZero();
        assertThat(limiter.retryAfter("a")).isEqualTo(60);
        assertThat(limiter.retryAfter("b")).isZero();
        clock.instant = clock.instant.plusSeconds(60);
        assertThat(limiter.retryAfter("a")).isZero();
    }

    @Test
    void limitsTotalTrafficAcrossDifferentClients() {
        ContactRateLimiter limiter = new ContactRateLimiter(properties(5, 2), new MutableClock());
        assertThat(limiter.retryAfter("a")).isZero();
        assertThat(limiter.retryAfter("b")).isZero();
        assertThat(limiter.retryAfter("c")).isEqualTo(60);
    }

    private ContactProperties properties(int perClient, int global) {
        return new ContactProperties(false, "", "", List.of("http://localhost:5173"), perClient, global, Duration.ofMinutes(1));
    }

    private static class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
