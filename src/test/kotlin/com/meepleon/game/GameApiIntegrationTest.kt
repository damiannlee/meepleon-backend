package com.meepleon.game

import com.meepleon.event.EventRepository
import com.meepleon.event.EventSubmissionRequest
import com.meepleon.event.EventType
import com.meepleon.event.Region
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameApiIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val gameRepository: GameRepository,
    @Autowired private val eventRepository: EventRepository,
) {

    @Test
    fun `unknown game is a 404`() {
        mockMvc.perform(get("/api/games/999999")).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun `game page lists only published events, ordered by start date`() {
        val game = gameRepository.save(Game(titleKo = "카탄", titleOriginal = "Catan", publisher = "카탄스튜디오"))

        val expansion = eventRepository.save(
            EventSubmissionRequest(
                title = "확장판 선주문",
                eventType = EventType.PREORDER,
                region = Region.DOMESTIC,
                platform = "보드엠",
                originalUrl = "https://boardm.co.kr/catan-expansion",
                gameId = game.id,
                startAt = Instant.parse("2026-06-01T00:00:00Z"),
            ).toEntity().apply { publish() },
        )
        val funding = eventRepository.save(
            EventSubmissionRequest(
                title = "해외 펀딩",
                eventType = EventType.FUNDING,
                region = Region.OVERSEAS,
                platform = "Kickstarter",
                originalUrl = "https://kickstarter.com/catan",
                gameId = game.id,
                startAt = Instant.parse("2026-01-01T00:00:00Z"),
            ).toEntity().apply { publish() },
        )
        // Pending (unpublished) events for the same game must not appear.
        eventRepository.save(
            EventSubmissionRequest(
                title = "검수 대기중",
                eventType = EventType.SALE,
                region = Region.DOMESTIC,
                platform = "보드엠",
                originalUrl = "https://boardm.co.kr/catan-pending",
                gameId = game.id,
            ).toEntity(),
        )
        // Different game's event must not appear.
        val otherGame = gameRepository.save(Game(titleOriginal = "Unrelated Game"))
        eventRepository.save(
            EventSubmissionRequest(
                title = "다른 게임 이벤트",
                eventType = EventType.SALE,
                region = Region.DOMESTIC,
                platform = "보드엠",
                originalUrl = "https://boardm.co.kr/other",
                gameId = otherGame.id,
            ).toEntity().apply { publish() },
        )

        mockMvc.perform(get("/api/games/${game.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.titleKo").value("카탄"))
            .andExpect(jsonPath("$.titleOriginal").value("Catan"))
            .andExpect(jsonPath("$.publisher").value("카탄스튜디오"))
            .andExpect(jsonPath("$.events.length()").value(2))
            .andExpect(jsonPath("$.events[0].id").value(funding.id))
            .andExpect(jsonPath("$.events[1].id").value(expansion.id))
    }
}
