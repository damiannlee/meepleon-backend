package com.meepleday.event

import com.meepleday.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "events",
    indexes = [
        Index(name = "idx_events_moderation_end", columnList = "moderation_status, end_at"),
        Index(name = "idx_events_region", columnList = "region"),
        Index(name = "idx_events_type", columnList = "event_type"),
    ],
)
class Event(

    @Column(nullable = false)
    var title: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    var eventType: EventType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var region: Region,

    /** Source platform, kept as free text since new marketplaces appear often (텀블벅, Gamefound, 보드엠 ...). */
    @Column(nullable = false)
    var platform: String,

    @Column(name = "original_url", nullable = false, length = 1000)
    var originalUrl: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "cover_image_url", length = 1000)
    var coverImageUrl: String? = null,

    /** Publisher / maker of the game (제작사). */
    @Column
    var publisher: String? = null,

    @Column(name = "start_at")
    var startAt: Instant? = null,

    @Column(name = "end_at")
    var endAt: Instant? = null,

    /** Funding goal — only meaningful for EventType.FUNDING. */
    @Column(name = "goal_amount", precision = 15, scale = 2)
    var goalAmount: BigDecimal? = null,

    @Column(name = "current_amount", precision = 15, scale = 2)
    var currentAmount: BigDecimal? = null,

    /** ISO currency code (KRW, USD ...) so overseas amounts render correctly. */
    @Column(length = 3)
    var currency: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    var moderationStatus: ModerationStatus = ModerationStatus.PENDING,

    @Column(name = "rejection_reason", length = 500)
    var rejectionReason: String? = null,

    /** Free-text submitter identity until real accounts land (auth is the final phase). */
    @Column(name = "submitter_name")
    var submitterName: String? = null,

    @Column(name = "submitter_email")
    var submitterEmail: String? = null,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    fun statusAt(now: Instant): EventStatus = EventStatus.of(startAt, endAt, now)

    /** Funding progress as a percentage (0-based, may exceed 100), or null when not a funding event. */
    fun fundingProgressPercent(): Int? {
        val goal = goalAmount ?: return null
        val current = currentAmount ?: return null
        if (goal.signum() <= 0) return null
        return current.multiply(BigDecimal(100)).divide(goal, 0, java.math.RoundingMode.DOWN).toInt()
    }

    fun publish() {
        moderationStatus = ModerationStatus.PUBLISHED
        rejectionReason = null
    }

    fun reject(reason: String) {
        moderationStatus = ModerationStatus.REJECTED
        rejectionReason = reason
    }
}
