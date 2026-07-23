package com.meepleday.user

import com.meepleday.common.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val currentUserProvider: CurrentUserProvider,
) {

    /** SecurityConfig requires authentication for this path, so a null id here means the session's user was deleted. */
    @Transactional(readOnly = true)
    fun me(): UserResponse {
        val userId = currentUserProvider.currentUserId()
            ?: throw NotFoundException("No authenticated user")
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User $userId not found") }
        return UserResponse.of(user)
    }
}
