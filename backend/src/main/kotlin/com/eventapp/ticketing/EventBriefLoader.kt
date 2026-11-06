package com.eventapp.ticketing

import com.eventapp.event.repository.EventMediaRepository
import com.eventapp.event.repository.EventRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** Thông tin tối thiểu của sự kiện để gắn vào đơn/vé mà không cần join nặng. */
data class EventBrief(
    val id: Long,
    val title: String,
    val startAt: Instant,
    val endAt: Instant,
    val venueName: String?,
    val city: String?,
    val coverUrl: String?
)

/**
 * Nạp thông tin sự kiện theo lô cho danh sách đơn/vé.
 *
 * Gộp 2 truy vấn (events + event_media) thay vì N+1 khi render danh sách.
 */
@Component
class EventBriefLoader(
    private val eventRepository: EventRepository,
    private val eventMediaRepository: EventMediaRepository
) {

    @Transactional(readOnly = true)
    fun load(ids: Collection<Long>): Map<Long, EventBrief> {
        if (ids.isEmpty()) return emptyMap()
        val covers = eventMediaRepository.findByEventIdInOrderBySortOrderAsc(ids)
            .filter { it.isCover }
            .associate { it.eventId to it.storageUrl }
        return eventRepository.findAllById(ids).associate { event ->
            val id = event.eventId!!
            id to EventBrief(
                id = id,
                title = event.title,
                startAt = event.startAt,
                endAt = event.endAt,
                venueName = event.venueName,
                city = event.city,
                coverUrl = covers[id]
            )
        }
    }

    fun load(id: Long): EventBrief? = load(listOf(id))[id]
}
