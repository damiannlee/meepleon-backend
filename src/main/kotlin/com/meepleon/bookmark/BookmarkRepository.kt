package com.meepleon.bookmark

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface BookmarkRepository : JpaRepository<Bookmark, Long> {

    fun findByUserIdAndEventId(userId: Long, eventId: Long): Bookmark?

    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Bookmark>

    fun deleteByUserIdAndEventId(userId: Long, eventId: Long)
}
