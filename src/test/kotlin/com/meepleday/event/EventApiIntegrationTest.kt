package com.meepleday.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.meepleday.game.Game
import com.meepleday.game.GameRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventApiIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val gameRepository: GameRepository,
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
                .with(csrf())
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
                .with(csrf())
                .with(user("admin").roles("ADMIN"))
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
                .with(csrf())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(EventModerationRequest(ModerationAction.REJECT))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun `event with no dates is announced, not ongoing`() {
        val announced = eventRepository.save(
            EventSubmissionRequest(
                title = "일정 미정 예고",
                eventType = EventType.PREORDER,
                region = Region.DOMESTIC,
                platform = "보드엠",
                originalUrl = "https://boardm.co.kr/announced",
                scheduleNote = "2026년 4분기 예정",
            ).toEntity().apply { publish() },
        )
        val id = announced.id ?: error("persisted event must have an id")

        mockMvc.perform(get("/api/events").param("status", "ANNOUNCED"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[?(@.id == $id)]").exists())

        mockMvc.perform(get("/api/events").param("status", "ONGOING"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[?(@.id == $id)]").doesNotExist())
    }

    @Test
    @Transactional
    fun `keyword search matches event title, publisher, and linked game title`() {
        val game = gameRepository.save(Game(titleKo = "카탄", titleOriginal = "Catan"))
        val catanEvent = eventRepository.save(
            EventSubmissionRequest(
                title = "확장판 선주문",
                eventType = EventType.PREORDER,
                region = Region.DOMESTIC,
                platform = "보드엠",
                originalUrl = "https://boardm.co.kr/catan-expansion",
                gameId = game.id,
            ).toEntity().apply { publish() },
        )
        val publisherMatch = eventRepository.save(
            EventSubmissionRequest(
                title = "무관한 이벤트",
                eventType = EventType.SALE,
                region = Region.DOMESTIC,
                platform = "보드엠",
                originalUrl = "https://boardm.co.kr/publisher-match",
                publisher = "카탄스튜디오",
            ).toEntity().apply { publish() },
        )
        eventRepository.save(
            EventSubmissionRequest(
                title = "관계없는 게임 이벤트",
                eventType = EventType.FUNDING,
                region = Region.DOMESTIC,
                platform = "텀블벅",
                originalUrl = "https://tumblbug.com/unrelated",
            ).toEntity().apply { publish() },
        )

        // Matches via linked Game.titleKo -> gameId, and via Event.publisher, but not the unrelated event.
        mockMvc.perform(get("/api/events").param("q", "카탄"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[?(@.id == ${catanEvent.id})]").exists())
            .andExpect(jsonPath("$.content[?(@.id == ${publisherMatch.id})]").exists())

        // Case-insensitive, partial match on the game's original title too.
        mockMvc.perform(get("/api/events").param("q", "atan"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[?(@.id == ${catanEvent.id})]").exists())

        // Blank keyword is ignored, falling back to the unfiltered feed.
        mockMvc.perform(get("/api/events").param("q", "  "))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(3))

        // No match.
        mockMvc.perform(get("/api/events").param("q", "존재하지않는게임"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(0))
    }

    @Test
    @Transactional
    fun `keyword search escapes LIKE wildcards so a literal percent sign doesn't match everything`() {
        eventRepository.save(
            EventSubmissionRequest(
                title = "50% 할인전",
                eventType = EventType.SALE,
                region = Region.DOMESTIC,
                platform = "보드엠",
                originalUrl = "https://boardm.co.kr/sale-50",
            ).toEntity().apply { publish() },
        )
        eventRepository.save(
            EventSubmissionRequest(
                title = "일반 세일",
                eventType = EventType.SALE,
                region = Region.DOMESTIC,
                platform = "보드엠",
                originalUrl = "https://boardm.co.kr/sale-plain",
            ).toEntity().apply { publish() },
        )

        mockMvc.perform(get("/api/events").param("q", "50%"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("50% 할인전"))
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
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)),
        )
            .andExpect(status().isBadRequest)
    }
}
