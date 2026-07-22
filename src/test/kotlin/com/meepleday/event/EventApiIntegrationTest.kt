package com.meepleday.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventApiIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val eventRepository: EventRepository,
) {

    @Test
    fun `submitted event is hidden from feed until published, then visible`() {
        val request = EventSubmissionRequest(
            title = "테스트 펀딩",
            eventType = EventType.FUNDING,
            region = Region.DOMESTIC,
            platform = "텀블벅",
            originalUrl = "https://tumblbug.com/test",
        )

        // Submit -> lands as PENDING (201)
        val submitResult = mockMvc.perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.moderationStatus").value("PENDING"))
            .andReturn()

        val id = objectMapper.readTree(submitResult.response.contentAsString).get("id").asLong()

        // Not in public feed yet
        mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(0))

        // Detail 404 while pending
        mockMvc.perform(get("/api/events/$id")).andExpect(status().isNotFound)

        // Operator publishes
        mockMvc.perform(
            patch("/api/admin/events/$id/moderation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(EventModerationRequest(ModerationAction.PUBLISH))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.moderationStatus").value("PUBLISHED"))

        // Now visible in feed
        mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(id))
    }

    @Test
    fun `reject without reason is a 400`() {
        val event = eventRepository.save(
            EventSubmissionRequest(
                title = "반려 대상",
                eventType = EventType.SALE,
                region = Region.DOMESTIC,
                platform = "보드엠",
                originalUrl = "https://boardm.co.kr/test",
            ).toEntity(),
        )

        mockMvc.perform(
            patch("/api/admin/events/${event.id}/moderation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(EventModerationRequest(ModerationAction.REJECT))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `invalid submission is rejected by validation`() {
        val invalid = mapOf(
            "title" to "",
            "eventType" to "FUNDING",
            "region" to "DOMESTIC",
            "platform" to "텀블벅",
            "originalUrl" to "https://x.co",
        )

        mockMvc.perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)),
        )
            .andExpect(status().isBadRequest)
    }
}
