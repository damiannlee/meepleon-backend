package com.meepleday.user

data class UserResponse(
    val id: Long,
    val provider: AuthProvider,
    val email: String?,
    val displayName: String,
    val role: UserRole,
) {
    companion object {
        fun of(user: User): UserResponse = UserResponse(
            id = user.id ?: error("persisted user must have an id"),
            provider = user.provider,
            email = user.email,
            displayName = user.displayName,
            role = user.role,
        )
    }
}
