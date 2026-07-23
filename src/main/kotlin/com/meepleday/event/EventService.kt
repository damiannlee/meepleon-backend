package com.meepleday.event

import com.meepleday.common.BadRequestException
import com.meepleday.common.NotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

/** Optional feed filters; any null field is ignored. */
data class EventFeedQuery(
    val region: Region? = null,
    val eventType: EventType? = null,
    val platform: String? = null,
    val status: EventStatus? = null,
)

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val rateLimiter: SubmissionRateLimiter,
    private val clock: Clock,
) {

    /** Public feed: PUBLISHED events only, filtered and paged by the caller's Pageable (default sort applied upstream). */
    @Transactional(readOnly = true)
    fun getFeed(query: EventFeedQuery, pageable: Pageable): Page<EventResponse> {
        val now = clock.instant()
        val spec = buildFeedSpecification(query, now)
        return eventRepository.findAll(spec, pageable).map { EventResponse.of(it, now) }
    }

    @Transactional(readOnly = true)
    fun getPublishedEvent(id: Long): EventResponse {
        val now = clock.instant()
        val event = eventRepository.findById(id)
            .filter { it.moderationStatus == ModerationStatus.PUBLISHED }
            .orElseThrow { NotFoundException("Event $id not found") }
        return EventResponse.of(event, now)
    }

    /**
     * Bot traffic (honeypot trip or rate-limit exceeded) gets a convincing response but is never persisted (ADR-0006).
     * Submission stays anonymous-friendly by design (Phase 2 auth doesn't gate it) — `submittedByUserId` is only
     * attribution for logged-in submitters, not a requirement.
     */
    @Transactional
    fun submit(request: EventSubmissionRequest, clientIp: String, submittedByUserId: Long?): EventResponse {
        val now = clock.instant()
        if (request.isHoneypotTripped() || !rateLimiter.tryConsume(clientIp)) {
            return request.toFakeResponse(now)
        }
        val saved = eventRepository.save(request.toEntity(submittedByUserId))
        return EventResponse.of(saved, now)
    }

    @Transactional(readOnly = true)
    fun listByModerationStatus(status: ModerationStatus, pageable: Pageable): Page<EventResponse> {
        val now = clock.instant()
        val spec = EventSpecifications.moderationStatus(status)
        return eventRepository.findAll(spec, pageable).map { EventResponse.of(it, now) }
    }

    @Transactional
    fun moderate(id: Long, request: EventModerationRequest): EventResponse {
        val event = eventRepository.findById(id)
            .orElseThrow { NotFoundException("Event $id not found") }
        when (request.action) {
            ModerationAction.PUBLISH -> event.publish()
            ModerationAction.REJECT -> {
                val reason = request.reason?.takeIf { it.isNotBlank() }
                    ?: throw BadRequestException("Rejection requires a reason")
                event.reject(reason)
            }
        }
        return EventResponse.of(event, clock.instant())
    }

    private fun buildFeedSpecification(query: EventFeedQuery, now: Instant): Specification<Event> {
        val specs = listOfNotNull(
            EventSpecifications.moderationStatus(ModerationStatus.PUBLISHED),
            EventSpecifications.region(query.region),
            EventSpecifications.eventType(query.eventType),
            EventSpecifications.platform(query.platform),
            EventSpecifications.status(query.status, now),
        )
        return specs.reduce { acc, spec -> acc.and(spec) }
    }
}
