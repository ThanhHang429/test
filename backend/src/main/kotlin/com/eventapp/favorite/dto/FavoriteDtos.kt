package com.eventapp.favorite.dto

import com.eventapp.discovery.dto.EventCardDto
import java.time.Instant

/**
 * Một mục trong danh sách "Đã lưu".
 * Khi sự kiện bị ẩn/đình chỉ/chưa công khai: [available] = false và [event] = null
 * để không lộ tên/ảnh (SRS FR-U06).
 */
data class FavoriteItemDto(
    val eventId: Long,
    val available: Boolean,
    /** Đã huỷ / Đã kết thúc / Không còn khả dụng / nhãn bán vé */
    val statusLabel: String,
    val favoritedAt: Instant?,
    val event: EventCardDto?
)
