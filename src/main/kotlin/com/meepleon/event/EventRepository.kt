package com.meepleon.event

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface EventRepository : JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /** Events on a game's page (S3): published only, ordered chronologically (nulls-last globally, see application.yml). */
    fun findByGameIdAndModerationStatusOrderByStartAtAsc(gameId: Long, moderationStatus: ModerationStatus): List<Event>

    /** Logged-in submitter's own submissions (any moderation status) — m3-tracking.md §3 "로그인 혜택". */
    fun findBySubmittedByUserIdOrderByCreatedAtDesc(submittedByUserId: Long, pageable: Pageable): Page<Event>
}
