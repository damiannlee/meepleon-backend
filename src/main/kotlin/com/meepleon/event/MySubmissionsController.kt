package com.meepleon.event

import com.meepleon.common.NotFoundException
import com.meepleon.user.CurrentUserProvider
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** m3-tracking.md §3 "로그인 혜택" — a submitter's own submission history, any moderation status. */
@RestController
class MySubmissionsController(
    private val eventService: EventService,
    private val currentUserProvider: CurrentUserProvider,
) {

    @GetMapping("/api/me/submissions")
    fun getMySubmissions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<EventResponse> {
        val userId = currentUserProvider.currentUserId() ?: throw NotFoundException("No authenticated user")
        val pageable = PageRequest.of(page, size.coerceIn(1, 100))
        return eventService.getMySubmissions(userId, pageable)
    }
}
