package com.meepleday.user

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SecurityContextCurrentUserProvider : CurrentUserProvider {

    override fun currentUserId(): Long? {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return (principal as? AppOAuth2User)?.userId
    }
}
