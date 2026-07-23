package com.meepleday.user

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Attributes read off an OAuth2 provider's user-info response, already normalized per-provider. */
data class OAuth2Profile(
    val provider: AuthProvider,
    val providerId: String,
    val email: String?,
    val displayName: String,
)

/**
 * Finds-or-creates the local User for an OAuth2 login. Kept separate from Spring Security's
 * OAuth2UserService so the provisioning/ADMIN-promotion rule is unit-testable without mocking
 * OAuth2UserRequest.
 */
@Service
class UserProvisioningService(
    private val userRepository: UserRepository,
    private val adminAllowlistProperties: AdminAllowlistProperties,
) {

    @Transactional
    fun provision(profile: OAuth2Profile): User {
        val existing = userRepository.findByProviderAndProviderId(profile.provider, profile.providerId)
        if (existing != null) {
            existing.email = profile.email
            existing.displayName = profile.displayName
            return existing
        }
        val role = if (adminAllowlistProperties.isAdmin(profile.provider, profile.providerId)) {
            UserRole.ADMIN
        } else {
            UserRole.USER
        }
        return userRepository.save(
            User(
                provider = profile.provider,
                providerId = profile.providerId,
                email = profile.email,
                displayName = profile.displayName,
                role = role,
            ),
        )
    }
}
