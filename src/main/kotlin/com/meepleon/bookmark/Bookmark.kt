package com.meepleon.bookmark

import com.meepleon.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * A user tracking a single Event's deadline (spec/tracking-model.md §1). Plain FK columns rather than
 * @ManyToOne associations, matching Event.gameId's style (ADR-0007) — the relation is enforced at the DB
 * level (see V4 migration) without coupling the entities.
 */
@Entity
@Table(
    name = "bookmarks",
    uniqueConstraints = [UniqueConstraint(name = "uk_bookmarks_user_event", columnNames = ["user_id", "event_id"])],
)
class Bookmark(

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "event_id", nullable = false)
    var eventId: Long,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
