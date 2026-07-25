package com.meepleday.event

import com.meepleday.common.toContainsLikePattern
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
     * Matches [Event.title]/[Event.publisher] directly, plus any event whose [Event.gameId] is in
     * [matchedGameIds] (pre-resolved by a separate Game-title lookup — see spec/search.md's fixed 2-query rule,
     * since `gameId` is a plain column rather than a JPA association we can join on here).
     */
    fun keyword(q: String?, matchedGameIds: List<Long>): Specification<Event>? {
        val trimmed = q?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val pattern = toContainsLikePattern(trimmed).lowercase()
        return Specification { root, _, cb ->
            val titleMatch = cb.like(cb.lower(root.get("title")), pattern, '\\')
            val publisherMatch = cb.like(cb.lower(root.get<String>("publisher")), pattern, '\\')
            if (matchedGameIds.isEmpty()) {
                cb.or(titleMatch, publisherMatch)
            } else {
                cb.or(titleMatch, publisherMatch, root.get<Long>("gameId").`in`(matchedGameIds))
            }
        }
    }

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
                EventStatus.ANNOUNCED -> cb.and(cb.isNull(startAt), cb.isNull(endAt))
                EventStatus.ENDED -> cb.and(cb.isNotNull(endAt), cb.lessThan(endAt, now))
                EventStatus.UPCOMING -> cb.and(cb.isNotNull(startAt), cb.greaterThan(startAt, now))
                EventStatus.ONGOING -> cb.and(
                    ongoingPredicate(cb, startAt, endAt, now),
                    cb.or(cb.isNotNull(startAt), cb.isNotNull(endAt)),
                )
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
