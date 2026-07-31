package com.meepleon.prefill

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-IP fixed-window limiter for the URL prefill endpoint.
 * Separate bucket from EventService's SubmissionRateLimiter — prefill can be called several
 * times before a single submission, so it needs its own, higher limit (ADR-0006 2026-07-23 revision).
 */
@Component
class PrefillRateLimiter(
    private val clock: Clock,
    private val properties: PrefillProperties,
) {
    private val requestsByIp = ConcurrentHashMap<String, MutableList<Instant>>()

    @Synchronized
    fun tryConsume(clientIp: String): Boolean {
        val now = clock.instant()
        val windowStart = now.minus(Duration.ofHours(properties.rateLimitWindowHours))
        val timestamps = requestsByIp.computeIfAbsent(clientIp) { mutableListOf() }
        timestamps.removeAll { it.isBefore(windowStart) }
        if (timestamps.size >= properties.rateLimitMaxRequestsPerWindow) return false
        timestamps.add(now)
        return true
    }
}
