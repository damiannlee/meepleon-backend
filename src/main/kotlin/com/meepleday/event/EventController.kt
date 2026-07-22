package com.meepleday.event

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventService: EventService,
) {

    /** Feed sorted by soonest deadline first (마감임박순), nulls last. */
    @GetMapping
    fun getFeed(
        @RequestParam(required = false) region: Region?,
        @RequestParam(required = false) eventType: EventType?,
        @RequestParam(required = false) platform: String?,
        @RequestParam(required = false) status: EventStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<EventResponse> {
        val query = EventFeedQuery(region, eventType, platform, status)
        val pageable = PageRequest.of(page, size.coerceIn(1, 100), DEADLINE_SORT)
        return eventService.getFeed(query, pageable)
    }

    @GetMapping("/{id}")
    fun getEvent(@PathVariable id: Long): EventResponse = eventService.getPublishedEvent(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun submit(@Valid @RequestBody request: EventSubmissionRequest): EventResponse =
        eventService.submit(request)

    companion object {
        // Nulls-last handled globally via hibernate.order_by.default_null_ordering
        // (JPA Criteria queries don't support per-order null precedence).
        private val DEADLINE_SORT: Sort = Sort.by(Sort.Direction.ASC, "endAt")
    }
}
