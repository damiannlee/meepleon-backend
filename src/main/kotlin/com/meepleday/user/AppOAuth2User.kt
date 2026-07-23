package com.meepleday.user

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

/** Principal wrapping our local User so downstream code (SecurityConfig, CurrentUserProvider) never needs provider-specific attributes. */
class AppOAuth2User(
    val userId: Long,
    private val role: UserRole,
    private val name: String,
    private val attributes: Map<String, Any>,
) : OAuth2User {

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_$role"))

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getName(): String = name
}
