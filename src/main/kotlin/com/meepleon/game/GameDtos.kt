package com.meepleon.game

import com.meepleon.event.EventResponse

/** Game page (S3): game info plus its events' lifecycle timeline (funding -> preorder -> expansion), chronological. */
data class GameDetailResponse(
    val id: Long,
    val titleKo: String?,
    val titleOriginal: String,
    val publisher: String?,
    val bggId: Long?,
    val events: List<EventResponse>,
) {
    companion object {
        fun of(game: Game, events: List<EventResponse>): GameDetailResponse = GameDetailResponse(
            id = game.id ?: error("persisted game must have an id"),
            titleKo = game.titleKo,
            titleOriginal = game.titleOriginal,
            publisher = game.publisher,
            bggId = game.bggId,
            events = events,
        )
    }
}
