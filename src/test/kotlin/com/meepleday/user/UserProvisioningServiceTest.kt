package com.meepleday.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UserProvisioningServiceTest {

    private val userRepository: UserRepository = mock()

    @Test
    fun `creates a new USER when no allowlist entry matches`() {
        whenever(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "1001")).thenReturn(null)
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }
        val service = UserProvisioningService(userRepository, AdminAllowlistProperties(adminProviderIds = emptyList()))

        val profile = OAuth2Profile(AuthProvider.KAKAO, "1001", "user@example.com", "테스터")
        val user = service.provision(profile)

        assertEquals(UserRole.USER, user.role)
        assertEquals("1001", user.providerId)
    }

    @Test
    fun `creates an ADMIN when providerId is on the allowlist`() {
        whenever(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "9001")).thenReturn(null)
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }
        val service = UserProvisioningService(
            userRepository,
            AdminAllowlistProperties(adminProviderIds = listOf("KAKAO:9001")),
        )

        val profile = OAuth2Profile(AuthProvider.KAKAO, "9001", null, "운영자")
        val user = service.provision(profile)

        assertEquals(UserRole.ADMIN, user.role)
    }

    @Test
    fun `reuses the existing user and never re-promotes on subsequent logins`() {
        val existing = User(
            provider = AuthProvider.GOOGLE,
            providerId = "g-1",
            email = "old@example.com",
            displayName = "구버전 이름",
            role = UserRole.USER,
        )
        whenever(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "g-1")).thenReturn(existing)
        val service = UserProvisioningService(
            userRepository,
            AdminAllowlistProperties(adminProviderIds = listOf("GOOGLE:g-1")),
        )

        val profile = OAuth2Profile(AuthProvider.GOOGLE, "g-1", "new@example.com", "새 이름")
        val user = service.provision(profile)

        assertSame(existing, user)
        assertEquals("new@example.com", user.email)
        assertEquals("새 이름", user.displayName)
        assertEquals(UserRole.USER, user.role, "existing role must not be silently upgraded by re-login")
        Mockito.verify(userRepository, Mockito.never()).save(any())
    }
}
