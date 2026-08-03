package com.meepleon.bookmark

import com.fasterxml.jackson.databind.ObjectMapper
import com.meepleon.event.EventModerationRequest
import com.meepleon.event.EventRepository
import com.meepleon.event.EventSubmissionRequest
import com.meepleon.event.EventType
import com.meepleon.event.ModerationAction
import com.meepleon.event.Region
import com.meepleon.user.AppOAuth2User
import com.meepleon.user.AuthProvider
import com.meepleon.user.User
import com.meepleon.user.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookmarkApiIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val bookmarkRepository: BookmarkRepository,
) {

    private fun asUser(user: User): RequestPostProcessor {
        val principal = AppOAuth2User(user.id ?: error("persisted user must have an id"), user.role, user.displayName, emptyMap())
        return authentication(UsernamePasswordAuthenticationToken(principal, null, principal.authorities))
    }

    private fun newUser(providerId: String): User = userRepository.save(
        User(provider = AuthProvider.KAKAO, providerId = providerId, displayName = "user-$providerId"),
    )

    private fun publishedEvent(originalUrl: String): Long {
        val event = eventRepository.save(
            EventSubmissionRequest(
                title = "테스트 이벤트",
                eventType = EventType.FUNDING,
                region = Region.DOMESTIC,
                platform = "텀블벅",
                originalUrl = originalUrl,
            ).toEntity().apply { publish() },
        )
        return event.id ?: error("persisted event must have an id")
    }

    @Test
    @Transactional
    fun `bookmark then unbookmark round trip`() {
        val bookmarker = newUser("bm-1")
        val eventId = publishedEvent("https://tumblbug.com/bookmark-1")

        mockMvc.perform(post("/api/events/$eventId/bookmark").with(csrf()).with(asUser(bookmarker)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.event.id").value(eventId))

        mockMvc.perform(get("/api/me/bookmarks").with(asUser(bookmarker)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].event.id").value(eventId))

        mockMvc.perform(delete("/api/events/$eventId/bookmark").with(csrf()).with(asUser(bookmarker)))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/me/bookmarks").with(asUser(bookmarker)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(0))
    }

    @Test
    @Transactional
    fun `bookmarking the same event twice does not create a duplicate row`() {
        val bookmarker = newUser("bm-2")
        val eventId = publishedEvent("https://tumblbug.com/bookmark-2")

        mockMvc.perform(post("/api/events/$eventId/bookmark").with(csrf()).with(asUser(bookmarker)))
            .andExpect(status().isOk)
        mockMvc.perform(post("/api/events/$eventId/bookmark").with(csrf()).with(asUser(bookmarker)))
            .andExpect(status().isOk)

        assert(bookmarkRepository.count() == 1L) { "expected exactly one bookmark row, was ${bookmarkRepository.count()}" }
    }

    @Test
    @Transactional
    fun `unbookmarking twice is idempotent`() {
        val bookmarker = newUser("bm-3")
        val eventId = publishedEvent("https://tumblbug.com/bookmark-3")

        mockMvc.perform(delete("/api/events/$eventId/bookmark").with(csrf()).with(asUser(bookmarker)))
            .andExpect(status().isNoContent)
        mockMvc.perform(delete("/api/events/$eventId/bookmark").with(csrf()).with(asUser(bookmarker)))
            .andExpect(status().isNoContent)
    }

    @Test
    @Transactional
    fun `bookmarking a non-existent event is 404`() {
        val bookmarker = newUser("bm-4")

        mockMvc.perform(post("/api/events/999999/bookmark").with(csrf()).with(asUser(bookmarker)))
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun `bookmarking a pending (unpublished) event is 404`() {
        val bookmarker = newUser("bm-5")
        val pending = eventRepository.save(
            EventSubmissionRequest(
                title = "미검수 이벤트",
                eventType = EventType.FUNDING,
                region = Region.DOMESTIC,
                platform = "텀블벅",
                originalUrl = "https://tumblbug.com/pending",
            ).toEntity(),
        )

        mockMvc.perform(post("/api/events/${pending.id}/bookmark").with(csrf()).with(asUser(bookmarker)))
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun `one user cannot see or remove another user's bookmark`() {
        val owner = newUser("bm-owner")
        val stranger = newUser("bm-stranger")
        val eventId = publishedEvent("https://tumblbug.com/bookmark-isolated")

        mockMvc.perform(post("/api/events/$eventId/bookmark").with(csrf()).with(asUser(owner)))
            .andExpect(status().isOk)

        // Stranger's own list is empty — bookmarks are not shared.
        mockMvc.perform(get("/api/me/bookmarks").with(asUser(stranger)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(0))

        // Stranger "deleting" the bookmark is a no-op (idempotent DELETE, scoped to caller) — owner keeps it.
        mockMvc.perform(delete("/api/events/$eventId/bookmark").with(csrf()).with(asUser(stranger)))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/me/bookmarks").with(asUser(owner)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
    }

    @Test
    @Transactional
    fun `a bookmarked event that later gets rejected still shows in the list as REJECTED`() {
        val bookmarker = newUser("bm-6")
        val eventId = publishedEvent("https://tumblbug.com/bookmark-cancelled")

        mockMvc.perform(post("/api/events/$eventId/bookmark").with(csrf()).with(asUser(bookmarker)))
            .andExpect(status().isOk)

        mockMvc.perform(
            patch("/api/admin/events/$eventId/moderation")
                .with(csrf())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(EventModerationRequest(ModerationAction.REJECT, "무산됨")),
                ),
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/me/bookmarks").with(asUser(bookmarker)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].event.moderationStatus").value("REJECTED"))
    }

    @Test
    @Transactional
    fun `bookmark endpoints require authentication`() {
        val eventId = publishedEvent("https://tumblbug.com/bookmark-anon")

        mockMvc.perform(post("/api/events/$eventId/bookmark").with(csrf()))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(delete("/api/events/$eventId/bookmark").with(csrf()))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/me/bookmarks"))
            .andExpect(status().isUnauthorized)
    }
}
