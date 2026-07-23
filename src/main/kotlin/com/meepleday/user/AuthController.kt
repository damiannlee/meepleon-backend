package com.meepleday.user

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** Logout is handled by Spring Security's logout filter (see SecurityConfig, POST /api/auth/logout) — no controller code needed. */
@RestController
class AuthController(
    private val authService: AuthService,
) {

    @GetMapping("/api/me")
    fun me(): UserResponse = authService.me()
}
