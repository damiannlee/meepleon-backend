package com.meepleon.game

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GameRepository : JpaRepository<Game, Long> {

    /** Case-insensitive partial match on either title, used to resolve [Event.gameId] candidates for keyword search. */
    @Query(
        "select g.id from Game g " +
            "where lower(g.titleKo) like lower(:pattern) escape '\\' " +
            "or lower(g.titleOriginal) like lower(:pattern) escape '\\'",
    )
    fun findIdsByTitleLike(@Param("pattern") pattern: String): List<Long>
}
