package com.meepleday.event

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Operator-facing moderation API. Auth is the final development phase (see docs/adr);
 * until then this path is open, kept separate so it can be locked down in one place later.
 */
@RestController
@RequestMapping("/api/admin/events")
class EventAdminController(
    private val eventService: EventService,
) {

    /** Review queue — defaults to PENDING submissions, newest first. */
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "PENDING") status: ModerationStatus,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<EventResponse> {
        val pageable = PageRequest.of(page, size.coerceIn(1, 100), CREATED_DESC)
        return eventService.listByModerationStatus(status, pageable)
    }

    @PatchMapping("/{id}/moderation")
    fun moderate(
        @PathVariable id: Long,
        @Valid @RequestBody request: EventModerationRequest,
    ): EventResponse = eventService.moderate(id, request)

    companion object {
        private val CREATED_DESC: Sort = Sort.by(Sort.Direction.DESC, "createdAt")
    }
}
