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

    /** Free-text schedule hint for events without a fixed date yet (e.g. "2026년 4분기 예정"). */
    @Column(name = "schedule_note", length = 255)
    var scheduleNote: String? = null,

    /** Game this event belongs to; nullable since not every event maps to a single game (ADR-0007). */
    @Column(name = "game_id")
    var gameId: Long? = null,

    /** OFFLINE_EVENT-only fields — venue name, e.g. "서울 코엑스". */
    @Column(name = "location")
    var location: String? = null,

    @Column(name = "address")
    var address: String? = null,

    @Column(name = "admission_fee", precision = 15, scale = 2)
    var admissionFee: BigDecimal? = null,

    @Column(name = "reservation_url", length = 1000)
    var reservationUrl: String? = null,

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

    /** Set when the submitter was logged in at submission time; anonymous submissions leave this null. */
    @Column(name = "submitted_by_user_id")
    var submittedByUserId: Long? = null,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    fun statusAt(now: Instant): EventStatus = EventStatus.of(startAt, endAt, now)

    fun publish() {
        moderationStatus = ModerationStatus.PUBLISHED
        rejectionReason = null
    }

    fun reject(reason: String) {
        moderationStatus = ModerationStatus.REJECTED
        rejectionReason = reason
    }
}
