package com.meepleon.bookmark

import com.meepleon.common.NotFoundException
import com.meepleon.event.EventRepository
import com.meepleon.event.ModerationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val eventRepository: EventRepository,
    private val clock: Clock,
) {

    /** Idempotent: bookmarking an already-bookmarked event just returns the existing row. */
    @Transactional
    fun bookmark(userId: Long, eventId: Long): BookmarkResponse {
        val event = eventRepository.findById(eventId)
            .filter { it.moderationStatus == ModerationStatus.PUBLISHED }
            .orElseThrow { NotFoundException("Event $eventId not found") }
        val existing = bookmarkRepository.findByUserIdAndEventId(userId, eventId)
        val bookmark = existing ?: bookmarkRepository.save(Bookmark(userId = userId, eventId = eventId))
        return BookmarkResponse.of(bookmark, event, clock.instant())
    }

    /** Idempotent delete — removing a bookmark that isn't there (or belongs to someone else) is a no-op. */
    @Transactional
    fun unbookmark(userId: Long, eventId: Long) {
        bookmarkRepository.deleteByUserIdAndEventId(userId, eventId)
    }

    /** N+1 guard: one query for the bookmark page, one batch query for their events. */
    @Transactional(readOnly = true)
    fun getMyBookmarks(userId: Long, pageable: Pageable): Page<BookmarkResponse> {
        val now = clock.instant()
        val page = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        val eventsById = eventRepository.findAllById(page.content.map { it.eventId }).associateBy { it.id }
        return page.map { bookmark ->
            val event = eventsById[bookmark.eventId] ?: error("bookmarked event ${bookmark.eventId} missing")
            BookmarkResponse.of(bookmark, event, now)
        }
    }
}
