package com.meepleon.event

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MySubmissionsApiIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val userRepository: UserRepository,
) {

    private fun asUser(user: User): RequestPostProcessor {
        val principal = AppOAuth2User(user.id ?: error("persisted user must have an id"), user.role, user.displayName, emptyMap())
        return authentication(UsernamePasswordAuthenticationToken(principal, null, principal.authorities))
    }

    private fun newUser(providerId: String): User = userRepository.save(
        User(provider = AuthProvider.KAKAO, providerId = providerId, displayName = "user-$providerId"),
    )

    private fun submit(mockMvc: MockMvc, request: EventSubmissionRequest, submitter: RequestPostProcessor) {
        mockMvc.perform(
            post("/api/events")
                .with(csrf())
                .with(submitter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
    }

    @Test
    @Transactional
    fun `my submissions shows own PENDING and REJECTED entries, not another user's`() {
        val me = newUser("submitter-1")
        val someoneElse = newUser("submitter-2")

        submit(
            mockMvc,
            EventSubmissionRequest(
                title = "내 제보",
                eventType = EventType.SALE,
                region = Region.DOMESTIC,
                platform = "보드엠",
                originalUrl = "https://boardm.co.kr/mine",
            ),
            asUser(me),
        )
        submit(
            mockMvc,
            EventSubmissionRequest(
                title = "남의 제보",
                eventType = EventType.SALE,
                region = Region.DOMESTIC,
                platform = "보드엠",
                originalUrl = "https://boardm.co.kr/not-mine",
            ),
            asUser(someoneElse),
        )

        mockMvc.perform(get("/api/me/submissions").with(asUser(me)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("내 제보"))
    }

    @Test
    @Transactional
    fun `my submissions requires authentication`() {
        mockMvc.perform(get("/api/me/submissions")).andExpect(status().isUnauthorized)
    }
}
