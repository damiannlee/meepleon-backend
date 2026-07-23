package com.meepleday.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Covers ADR-0006: honeypot trips and rate-limit trips both look like success but are never persisted. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubmissionAbusePreventionTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val eventRepository: EventRepository,
) {

    @Test
    fun `honeypot trip returns a convincing response but is never persisted`() {
        val request = mapOf(
            "title" to "봇 제보",
            "eventType" to "FUNDING",
            "region" to "DOMESTIC",
            "platform" to "텀블벅",
            "originalUrl" to "https://tumblbug.com/bot",
            "website" to "https://spam.example",
        )
        val before = eventRepository.count()

        mockMvc.perform(
            post("/api/events")
                .with(fromIp("203.0.113.201"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.moderationStatus").value("PENDING"))

        assertEquals(before, eventRepository.count())
    }

    @Test
    fun `exceeding the per-IP rate limit fakes success without persisting further`() {
        val ip = "203.0.113.202"
        val before = eventRepository.count()

        repeat(SubmissionRateLimiter.MAX_REQUESTS_PER_WINDOW) { i ->
            val request = EventSubmissionRequest(
                title = "정상 제보 $i",
                eventType = EventType.SALE,
                region = Region.DOMESTIC,
                platform = "보드엠",
                originalUrl = "https://boardm.co.kr/rate-limit-$i",
            )
            mockMvc.perform(
                post("/api/events")
                    .with(fromIp(ip))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
        }
        assertEquals(before + SubmissionRateLimiter.MAX_REQUESTS_PER_WINDOW, eventRepository.count())

        val oneTooMany = EventSubmissionRequest(
            title = "한도 초과 제보",
            eventType = EventType.SALE,
            region = Region.DOMESTIC,
            platform = "보드엠",
            originalUrl = "https://boardm.co.kr/rate-limit-overflow",
        )
        mockMvc.perform(
            post("/api/events")
                .with(fromIp(ip))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(oneTooMany)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.moderationStatus").value("PENDING"))

        assertEquals(before + SubmissionRateLimiter.MAX_REQUESTS_PER_WINDOW, eventRepository.count())
    }

    private fun fromIp(ip: String): RequestPostProcessor =
        RequestPostProcessor { request: MockHttpServletRequest -> request.apply { remoteAddr = ip } }
}
