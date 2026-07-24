package com.meepleday.event

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class EventStatusTest {

    private val now: Instant = Instant.parse("2026-07-22T00:00:00Z")

    @Test
    fun `ended when endAt is in the past`() {
        val status = EventStatus.of(
            startAt = now.minus(10, ChronoUnit.DAYS),
            endAt = now.minus(1, ChronoUnit.DAYS),
            now = now,
        )
        assertEquals(EventStatus.ENDED, status)
    }

    @Test
    fun `upcoming when startAt is in the future`() {
        val status = EventStatus.of(
            startAt = now.plus(2, ChronoUnit.DAYS),
            endAt = now.plus(10, ChronoUnit.DAYS),
            now = now,
        )
        assertEquals(EventStatus.UPCOMING, status)
    }

    @Test
    fun `ending soon when endAt is within threshold`() {
        val status = EventStatus.of(
            startAt = now.minus(5, ChronoUnit.DAYS),
            endAt = now.plus(10, ChronoUnit.HOURS),
            now = now,
        )
        assertEquals(EventStatus.ENDING_SOON, status)
    }

    @Test
    fun `ongoing when past start and far from end`() {
        val status = EventStatus.of(
            startAt = now.minus(5, ChronoUnit.DAYS),
            endAt = now.plus(10, ChronoUnit.DAYS),
            now = now,
        )
        assertEquals(EventStatus.ONGOING, status)
    }

    @Test
    fun `announced when both timestamps are absent`() {
        val status = EventStatus.of(startAt = null, endAt = null, now = now)
        assertEquals(EventStatus.ANNOUNCED, status)
    }
}
