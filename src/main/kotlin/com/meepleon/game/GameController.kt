package com.meepleon.game

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/games")
class GameController(
    private val gameService: GameService,
) {

    @GetMapping("/{id}")
    fun getGame(@PathVariable id: Long): GameDetailResponse = gameService.getGameDetail(id)
}
