package com.eventapp.organizer.crowd

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** Phòng dự phòng organizer khai báo thêm ngoài phòng chính (FR-O28). */
@Entity
@Table(name = "event_overflow_rooms")
class EventOverflowRoom(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "overflow_room_id")
    var overflowRoomId: Long? = null,

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0L,

    @Column(name = "room_name", nullable = false, length = 120)
    var roomName: String = "",

    @Column(name = "capacity", nullable = false)
    var capacity: Int = 0,

    @Column(name = "note", length = 500)
    var note: String? = null,

    /** ACTIVE | CLOSED */
    @Column(name = "status", nullable = false, length = 20)
    var status: String = STATUS_ACTIVE,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_CLOSED = "CLOSED"
    }
}

/** Một lần gửi thông báo cho người tham dự của sự kiện (FR-O28). */
@Entity
@Table(name = "event_notifications")
class EventNotification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    var notificationId: Long? = null,

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0L,

    @Column(name = "created_by_user_id", nullable = false)
    var createdByUserId: Long = 0L,

    @Column(name = "title", nullable = false, length = 200)
    var title: String = "",

    @Column(name = "body", nullable = false, length = 2000)
    var body: String = "",

    /** REGISTERED_NOT_CHECKED_IN | ALL_REGISTERED */
    @Column(name = "audience", nullable = false, length = 40)
    var audience: String = AUDIENCE_NOT_CHECKED_IN,

    /** AI | MANUAL */
    @Column(name = "source", nullable = false, length = 20)
    var source: String = SOURCE_MANUAL,

    @Column(name = "recipient_count", nullable = false)
    var recipientCount: Int = 0,

    @Column(name = "sent_at", nullable = false)
    var sentAt: Instant = Instant.now()
) {
    companion object {
        const val AUDIENCE_NOT_CHECKED_IN = "REGISTERED_NOT_CHECKED_IN"
        const val AUDIENCE_ALL_REGISTERED = "ALL_REGISTERED"
        const val SOURCE_AI = "AI"
        const val SOURCE_MANUAL = "MANUAL"
    }
}

/** Từng người nhận — cho phép user đọc lại thông báo của mình. */
@Entity
@Table(name = "event_notification_recipients")
class EventNotificationRecipient(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipient_id")
    var recipientId: Long? = null,

    @Column(name = "notification_id", nullable = false)
    var notificationId: Long = 0L,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0L,

    @Column(name = "delivered_at", nullable = false)
    var deliveredAt: Instant = Instant.now()
)
