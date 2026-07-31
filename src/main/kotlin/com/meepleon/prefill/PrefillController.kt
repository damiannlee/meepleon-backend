package com.meepleon.prefill

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/events/prefill")
class PrefillController(
    private val prefillService: PrefillService,
) {
    @PostMapping
    fun prefill(
        @Valid @RequestBody request: PrefillRequest,
        servletRequest: HttpServletRequest,
    ): PrefillResponse = prefillService.prefill(request, servletRequest.remoteAddr)
}
