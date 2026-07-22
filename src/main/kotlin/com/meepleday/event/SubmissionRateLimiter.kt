package com.meepleday.event

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-IP fixed-window limiter for the anonymous submission endpoint (ADR-0006).
 * In-memory and single-instance only; revisit with a shared store (e.g. Redis)
 * if the backend ever scales out horizontally.
 */
@Component
class SubmissionRateLimiter(
    private val clock: Clock,
) {
    private val submissionsByIp = ConcurrentHashMap<String, MutableList<Instant>>()

    @Synchronized
    fun tryConsume(clientIp: String): Boolean {
        val now = clock.instant()
        val windowStart = now.minus(WINDOW)
        val timestamps = submissionsByIp.computeIfAbsent(clientIp) { mutableListOf() }
        timestamps.removeAll { it.isBefore(windowStart) }
        if (timestamps.size >= MAX_REQUESTS_PER_WINDOW) return false
        timestamps.add(now)
        return true
    }

    companion object {
        const val MAX_REQUESTS_PER_WINDOW = 5
        private val WINDOW: Duration = Duration.ofHours(1)
    }
}
