package com.meepleday.user

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @Test
    fun `anonymous access to the admin queue is 401`() {
        mockMvc.perform(get("/api/admin/events")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `admin role can reach the admin queue`() {
        mockMvc.perform(get("/api/admin/events").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
    }

    @Test
    fun `logged in but non-admin is forbidden from the admin queue`() {
        mockMvc.perform(get("/api/admin/events").with(user("someone").roles("USER")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `public feed stays anonymous`() {
        mockMvc.perform(get("/api/events")).andExpect(status().isOk)
    }

    @Test
    fun `me is 401 when anonymous`() {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized)
    }
}
