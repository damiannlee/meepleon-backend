package com.meepleon.game

import com.meepleon.common.NotFoundException
import com.meepleon.event.EventRepository
import com.meepleon.event.EventResponse
import com.meepleon.event.ModerationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val eventRepository: EventRepository,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getGameDetail(id: Long): GameDetailResponse {
        val game = gameRepository.findById(id)
            .orElseThrow { NotFoundException("Game $id not found") }
        val now = clock.instant()
        val events = eventRepository
            .findByGameIdAndModerationStatusOrderByStartAtAsc(id, ModerationStatus.PUBLISHED)
            .map { EventResponse.of(it, now) }
        return GameDetailResponse.of(game, events)
    }
}
