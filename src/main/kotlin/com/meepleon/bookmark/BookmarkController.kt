package com.meepleon.bookmark

import com.meepleon.common.NotFoundException
import com.meepleon.user.CurrentUserProvider
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** SecurityConfig requires authentication for every path here (/api/events/{id}/bookmark and everything under /api/me). */
@RestController
class BookmarkController(
    private val bookmarkService: BookmarkService,
    private val currentUserProvider: CurrentUserProvider,
) {

    @PostMapping("/api/events/{eventId}/bookmark")
    fun bookmark(@PathVariable eventId: Long): BookmarkResponse =
        bookmarkService.bookmark(requireCurrentUserId(), eventId)

    @DeleteMapping("/api/events/{eventId}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unbookmark(@PathVariable eventId: Long) {
        bookmarkService.unbookmark(requireCurrentUserId(), eventId)
    }

    @GetMapping("/api/me/bookmarks")
    fun getMyBookmarks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<BookmarkResponse> {
        val pageable = PageRequest.of(page, size.coerceIn(1, 100))
        return bookmarkService.getMyBookmarks(requireCurrentUserId(), pageable)
    }

    private fun requireCurrentUserId(): Long =
        currentUserProvider.currentUserId() ?: throw NotFoundException("No authenticated user")
}
