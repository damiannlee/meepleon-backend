package com.meepleday.event

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

private class MutableClock(private var instant: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneId.of("UTC")
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = instant
    fun advance(duration: Duration) {
        instant = instant.plus(duration)
    }
}

class SubmissionRateLimiterTest {

    private val clock = MutableClock(Instant.parse("2026-07-23T00:00:00Z"))
    private val limiter = SubmissionRateLimiter(clock)

    @Test
    fun `allows up to the per-window limit then rejects`() {
        repeat(SubmissionRateLimiter.MAX_REQUESTS_PER_WINDOW) {
            assertTrue(limiter.tryConsume("203.0.113.1"))
        }
        assertFalse(limiter.tryConsume("203.0.113.1"))
    }

    @Test
    fun `tracks each IP independently`() {
        repeat(SubmissionRateLimiter.MAX_REQUESTS_PER_WINDOW) {
            assertTrue(limiter.tryConsume("203.0.113.1"))
        }
        assertTrue(limiter.tryConsume("203.0.113.2"))
    }

    @Test
    fun `resets once the window has fully elapsed`() {
        repeat(SubmissionRateLimiter.MAX_REQUESTS_PER_WINDOW) {
            assertTrue(limiter.tryConsume("203.0.113.1"))
        }
        assertFalse(limiter.tryConsume("203.0.113.1"))

        clock.advance(Duration.ofHours(1).plusSeconds(1))

        assertTrue(limiter.tryConsume("203.0.113.1"))
    }
}
