package com.meepleon.bookmark

import com.meepleon.event.Event
import com.meepleon.event.EventResponse
import java.time.Instant

/**
 * A cancelled (무산된) announced event stays bookmarked rather than being deleted, so it doesn't vanish
 * silently — the nested [EventResponse.moderationStatus] already surfaces REJECTED for the UI to label
 * "취소됨" (ADR-0004 개정 2026-08-02), so no separate flag is needed here.
 */
data class BookmarkResponse(
    val id: Long,
    val event: EventResponse,
    val createdAt: Instant,
) {
    companion object {
        fun of(bookmark: Bookmark, event: Event, now: Instant): BookmarkResponse = BookmarkResponse(
            id = bookmark.id ?: error("persisted bookmark must have an id"),
            event = EventResponse.of(event, now),
            createdAt = bookmark.createdAt,
        )
    }
}
