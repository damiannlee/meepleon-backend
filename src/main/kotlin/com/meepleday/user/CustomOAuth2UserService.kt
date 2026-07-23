package com.meepleday.user

import com.meepleday.common.BadRequestException
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val userProvisioningService: UserProvisioningService,
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oauth2User = super.loadUser(userRequest)
        val profile = resolveProfile(userRequest.clientRegistration.registrationId, oauth2User.attributes)
        val user = userProvisioningService.provision(profile)
        val userId = user.id ?: error("provisioned user must have an id")
        return AppOAuth2User(userId, user.role, profile.providerId, oauth2User.attributes)
    }

    private fun resolveProfile(registrationId: String, attributes: Map<String, Any>): OAuth2Profile = when (registrationId) {
        "kakao" -> buildKakaoProfile(attributes)
        "google" -> buildGoogleProfile(attributes)
        else -> throw OAuth2AuthenticationException("Unsupported provider: $registrationId")
    }

    private fun buildKakaoProfile(attributes: Map<String, Any>): OAuth2Profile {
        val providerId = attributes["id"]?.toString()
            ?: throw BadRequestException("Kakao profile missing id")
        val kakaoAccount = attributes["kakao_account"] as? Map<*, *>
        val nestedProfile = kakaoAccount?.get("profile") as? Map<*, *>
        return OAuth2Profile(
            provider = AuthProvider.KAKAO,
            providerId = providerId,
            email = kakaoAccount?.get("email") as? String,
            displayName = nestedProfile?.get("nickname") as? String ?: "카카오사용자",
        )
    }

    private fun buildGoogleProfile(attributes: Map<String, Any>): OAuth2Profile {
        val providerId = attributes["sub"] as? String
            ?: throw BadRequestException("Google profile missing sub")
        val email = attributes["email"] as? String
        return OAuth2Profile(
            provider = AuthProvider.GOOGLE,
            providerId = providerId,
            email = email,
            displayName = attributes["name"] as? String ?: email ?: "구글사용자",
        )
    }
}
