package com.meepleday.event

import java.time.Duration
import java.time.Instant

/** Kind of board game event surfaced in the feed. */
enum class EventType {
    FUNDING,        // 크라우드펀딩 (텀블벅, 이노펀딩, Kickstarter, Gamefound ...)
    PREORDER,       // 선주문
    SALE,           // 특가/할인
    OFFLINE_EVENT,  // 오프라인 행사 (컨벤션, 게임 페스타 ...)
}

/** Domestic (Korea) vs overseas source, so users can filter by relevance. */
enum class Region {
    DOMESTIC,
    OVERSEAS,
}

/** Curation state. User submissions land as PENDING; operators PUBLISH or REJECT. */
enum class ModerationStatus {
    PENDING,
    PUBLISHED,
    REJECTED,
}

/**
 * Lifecycle state derived from start/end timestamps — never persisted, always computed
 * so we don't have to keep a stored column in sync with wall-clock time.
 */
enum class EventStatus {
    ANNOUNCED,     // startAt and endAt are both unset — schedule not yet fixed (see Event.scheduleNote)
    UPCOMING,      // startAt is in the future
    ONGOING,       // between startAt and endAt
    ENDING_SOON,   // ONGOING and endAt within ENDING_SOON_THRESHOLD
    ENDED;         // endAt is in the past

    companion object {
        val ENDING_SOON_THRESHOLD: Duration = Duration.ofHours(48)

        fun of(startAt: Instant?, endAt: Instant?, now: Instant): EventStatus {
            if (startAt == null && endAt == null) return ANNOUNCED
            if (endAt != null && endAt.isBefore(now)) return ENDED
            if (startAt != null && startAt.isAfter(now)) return UPCOMING
            if (endAt != null && Duration.between(now, endAt) <= ENDING_SOON_THRESHOLD) return ENDING_SOON
            return ONGOING
        }
    }
}
