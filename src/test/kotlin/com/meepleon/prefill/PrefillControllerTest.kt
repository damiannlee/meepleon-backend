package com.meepleon.prefill

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.containsString
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

/** Real success-path fetching is covered separately (UrlFetcherTest/OgMetadataExtractorTest/PrefillServiceTest) —
 *  SsrfGuard legitimately blocks loopback, so a full MockMvc round trip can't reach a local test server. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrefillControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val prefillProperties: PrefillProperties,
) {

    @Test
    fun `rejects a blank URL with a validation error`() {
        mockMvc.perform(
            post("/api/events/prefill")
                .with(fromIp("203.0.113.50"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("url" to ""))),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `rejects a URL that resolves to a private address`() {
        mockMvc.perform(
            post("/api/events/prefill")
                .with(fromIp("203.0.113.51"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("url" to "http://127.0.0.1/whatever"))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("blocked address")))
    }

    @Test
    fun `stops accepting requests once the per-IP rate limit is hit`() {
        val ip = "203.0.113.52"
        val request = objectMapper.writeValueAsString(mapOf("url" to "http://127.0.0.1/x"))

        repeat(prefillProperties.rateLimitMaxRequestsPerWindow) {
            mockMvc.perform(
                post("/api/events/prefill")
                    .with(fromIp(ip))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request),
            ).andExpect(status().isBadRequest)
        }

        mockMvc.perform(
            post("/api/events/prefill")
                .with(fromIp(ip))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("Too many")))
    }

    private fun fromIp(ip: String): RequestPostProcessor =
        RequestPostProcessor { request: MockHttpServletRequest -> request.apply { remoteAddr = ip } }
}
