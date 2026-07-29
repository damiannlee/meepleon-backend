package com.meepleon.user

import org.springframework.boot.context.properties.ConfigurationProperties

/** Manual allowlist for ADMIN promotion — self-registration to ADMIN is never allowed. */
@ConfigurationProperties(prefix = "meepleon")
class AdminAllowlistProperties(
    val adminProviderIds: List<String> = emptyList(),
) {
    /** Entries are "PROVIDER:providerId", e.g. "KAKAO:12345". */
    fun isAdmin(provider: AuthProvider, providerId: String): Boolean =
        adminProviderIds.contains("$provider:$providerId")
}
