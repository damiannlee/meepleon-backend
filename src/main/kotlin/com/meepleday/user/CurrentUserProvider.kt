package com.meepleday.user

/** Abstraction so services never touch SecurityContext directly (project convention). */
interface CurrentUserProvider {
    /** Local user id of the authenticated caller, or null when anonymous. */
    fun currentUserId(): Long?
}
