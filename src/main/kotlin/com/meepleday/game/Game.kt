package com.meepleday.game

import com.meepleday.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Groups the events of a single board game (overseas funding -> domestic preorder -> sale ...)
 * so they can be browsed together. See ADR-0007.
 */
@Entity
@Table(name = "games")
class Game(

    /** Korean title, when known — may lag the original release. */
    @Column(name = "title_ko")
    var titleKo: String? = null,

    @Column(name = "title_original", nullable = false)
    var titleOriginal: String,

    @Column
    var publisher: String? = null,

    /** BoardGameGeek id, reserved for a future BGG integration — unused for now. */
    @Column(name = "bgg_id")
    var bggId: Long? = null,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
