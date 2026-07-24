package com.meepleday.event

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant

/** Public/detail view of an event, with lifecycle status computed at read time. */
data class EventResponse(
    val id: Long,
    val title: String,
    val eventType: EventType,
    val region: Region,
    val platform: String,
    val originalUrl: String,
    val description: String?,
    val coverImageUrl: String?,
    val publisher: String?,
    val startAt: Instant?,
    val endAt: Instant?,
    val scheduleNote: String?,
    val gameId: Long?,
    val location: String?,
    val address: String?,
    val admissionFee: BigDecimal?,
    val reservationUrl: String?,
    val status: EventStatus,
    val moderationStatus: ModerationStatus,
) {
    companion object {
        fun of(event: Event, now: Instant): EventResponse = EventResponse(
            id = event.id ?: error("persisted event must have an id"),
            title = event.title,
            eventType = event.eventType,
            region = event.region,
            platform = event.platform,
            originalUrl = event.originalUrl,
            description = event.description,
            coverImageUrl = event.coverImageUrl,
            publisher = event.publisher,
            startAt = event.startAt,
            endAt = event.endAt,
            scheduleNote = event.scheduleNote,
            gameId = event.gameId,
            location = event.location,
            address = event.address,
            admissionFee = event.admissionFee,
            reservationUrl = event.reservationUrl,
            status = event.statusAt(now),
            moderationStatus = event.moderationStatus,
        )
    }
}

/** Crowdsourced submission. Accepted from anyone; lands as PENDING for operator review. */
data class EventSubmissionRequest(
    @field:NotBlank @field:Size(max = 255)
    val title: String,

    @field:NotNull
    val eventType: EventType,

    @field:NotNull
    val region: Region,

    @field:NotBlank @field:Size(max = 255)
    val platform: String,

    @field:NotBlank @field:Size(max = 1000)
    val originalUrl: String,

    val description: String? = null,
    @field:Size(max = 1000) val coverImageUrl: String? = null,
    @field:Size(max = 255) val publisher: String? = null,
    val startAt: Instant? = null,
    val endAt: Instant? = null,
    @field:Size(max = 255) val scheduleNote: String? = null,
    val gameId: Long? = null,

    /** OFFLINE_EVENT-only fields — ignored for other event types. */
    @field:Size(max = 255) val location: String? = null,
    @field:Size(max = 500) val address: String? = null,
    val admissionFee: BigDecimal? = null,
    @field:Size(max = 1000) val reservationUrl: String? = null,

    @field:Size(max = 255) val submitterName: String? = null,
    @field:Email @field:Size(max = 255) val submitterEmail: String? = null,

    /** Honeypot decoy (ADR-0006): hidden from real users, so any value here marks the request as a bot. */
    @field:Size(max = 255) val website: String? = null,
) {
    fun isHoneypotTripped(): Boolean = !website.isNullOrBlank()

    /** Convincing but non-persisted response for bot traffic (honeypot or rate-limit trip) — see ADR-0006. */
    fun toFakeResponse(now: Instant): EventResponse = EventResponse(
        id = FAKE_SUBMISSION_ID,
        title = title,
        eventType = eventType,
        region = region,
        platform = platform,
        originalUrl = originalUrl,
        description = description,
        coverImageUrl = coverImageUrl,
        publisher = publisher,
        startAt = startAt,
        endAt = endAt,
        scheduleNote = scheduleNote,
        gameId = gameId,
        location = location,
        address = address,
        admissionFee = admissionFee,
        reservationUrl = reservationUrl,
        status = EventStatus.of(startAt, endAt, now),
        moderationStatus = ModerationStatus.PENDING,
    )

    fun toEntity(submittedByUserId: Long? = null): Event = Event(
        title = title,
        eventType = eventType,
        region = region,
        platform = platform,
        originalUrl = originalUrl,
        description = description,
        coverImageUrl = coverImageUrl,
        publisher = publisher,
        startAt = startAt,
        endAt = endAt,
        scheduleNote = scheduleNote,
        gameId = gameId,
        location = location,
        address = address,
        admissionFee = admissionFee,
        reservationUrl = reservationUrl,
        moderationStatus = ModerationStatus.PENDING,
        submitterName = submitterName,
        submitterEmail = submitterEmail,
        submittedByUserId = submittedByUserId,
    )
}

private const val FAKE_SUBMISSION_ID = 0L

enum class ModerationAction { PUBLISH, REJECT }

/** Operator decision on a submission. REJECT requires a reason. */
data class EventModerationRequest(
    @field:NotNull
    val action: ModerationAction,

    @field:Size(max = 500)
    val reason: String? = null,
)
