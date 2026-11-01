package com.eventapp.event.repository

import com.eventapp.event.entity.Event
import com.eventapp.event.entity.EventMedia
import com.eventapp.event.entity.TicketType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventRepository : JpaRepository<Event, Long> {
    fun findBySlug(slug: String): Event?
    fun findByEventCode(eventCode: String): Event?
    fun countByOrganizerProfileId(organizerProfileId: Long): Long
    fun findByOrganizerProfileIdOrderByStartAtDesc(organizerProfileId: Long): List<Event>
}

@Repository
interface EventMediaRepository : JpaRepository<EventMedia, Long> {
    fun findByEventIdOrderBySortOrderAsc(eventId: Long): List<EventMedia>
    fun findByEventIdInOrderBySortOrderAsc(eventIds: Collection<Long>): List<EventMedia>
}

@Repository
interface TicketTypeRepository : JpaRepository<TicketType, Long> {
    fun findByEventIdOrderByPriceAsc(eventId: Long): List<TicketType>
    fun findByEventIdIn(eventIds: Collection<Long>): List<TicketType>
}
