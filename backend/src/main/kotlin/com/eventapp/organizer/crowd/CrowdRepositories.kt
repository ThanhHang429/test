package com.eventapp.organizer.crowd

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventOverflowRoomRepository : JpaRepository<EventOverflowRoom, Long> {
    fun findByEventIdOrderByCreatedAtAsc(eventId: Long): List<EventOverflowRoom>
    fun findByEventIdAndStatusOrderByCreatedAtAsc(eventId: Long, status: String): List<EventOverflowRoom>
}

@Repository
interface EventNotificationRepository : JpaRepository<EventNotification, Long> {
    fun findByEventIdOrderBySentAtDesc(eventId: Long): List<EventNotification>
}

@Repository
interface EventNotificationRecipientRepository : JpaRepository<EventNotificationRecipient, Long> {
    fun findByUserIdOrderByDeliveredAtDesc(userId: Long): List<EventNotificationRecipient>
    fun findByNotificationId(notificationId: Long): List<EventNotificationRecipient>
    fun countByNotificationId(notificationId: Long): Long
}
