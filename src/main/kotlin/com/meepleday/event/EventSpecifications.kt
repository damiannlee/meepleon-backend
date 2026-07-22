package com.meepleday.event

import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import java.time.Instant

/**
 * Composable filters for the event feed. Each builder returns null when its argument
 * is absent, so callers can `and` them together and skip inactive filters.
 */
object EventSpecifications {

    fun moderationStatus(status: ModerationStatus): Specification<Event> =
        Specification { root, _, cb -> cb.equal(root.get<ModerationStatus>("moderationStatus"), status) }

    fun region(region: Region?): Specification<Event>? =
        region?.let { Specification { root, _, cb -> cb.equal(root.get<Region>("region"), it) } }

    fun eventType(type: EventType?): Specification<Event>? =
        type?.let { Specification { root, _, cb -> cb.equal(root.get<EventType>("eventType"), it) } }

    fun platform(platform: String?): Specification<Event>? =
        platform?.takeIf { it.isNotBlank() }
            ?.let { Specification { root, _, cb -> cb.equal(root.get<String>("platform"), it) } }

    /**
     * Derived lifecycle status expressed as time predicates against [now],
     * so we never persist a status column that would drift out of date.
     */
    fun status(status: EventStatus?, now: Instant): Specification<Event>? {
        if (status == null) return null
        val endAtField = "endAt"
        val startAtField = "startAt"
        val endingSoonBound = now.plus(EventStatus.ENDING_SOON_THRESHOLD)
        return Specification { root, _, cb ->
            val endAt = root.get<Instant>(endAtField)
            val startAt = root.get<Instant>(startAtField)
            when (status) {
                EventStatus.ENDED -> cb.and(cb.isNotNull(endAt), cb.lessThan(endAt, now))
                EventStatus.UPCOMING -> cb.and(cb.isNotNull(startAt), cb.greaterThan(startAt, now))
                EventStatus.ONGOING -> ongoingPredicate(cb, startAt, endAt, now)
                EventStatus.ENDING_SOON -> cb.and(
                    ongoingPredicate(cb, startAt, endAt, now),
                    cb.isNotNull(endAt),
                    cb.lessThanOrEqualTo(endAt, endingSoonBound),
                )
            }
        }
    }

    private fun ongoingPredicate(
        cb: jakarta.persistence.criteria.CriteriaBuilder,
        startAt: jakarta.persistence.criteria.Path<Instant>,
        endAt: jakarta.persistence.criteria.Path<Instant>,
        now: Instant,
    ): Predicate {
        val started = cb.or(cb.isNull(startAt), cb.lessThanOrEqualTo(startAt, now))
        val notEnded = cb.or(cb.isNull(endAt), cb.greaterThanOrEqualTo(endAt, now))
        return cb.and(started, notEnded)
    }
}
