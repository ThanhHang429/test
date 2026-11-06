package com.eventapp.common.response

/**
 * Thông tin phân trang theo SRS mục 3.3: page (bắt đầu từ 0), size (mặc định 20, tối đa 50),
 * totalItems, hasNext.
 */
data class PageMeta(
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val hasNext: Boolean
) {
    companion object {
        fun of(page: Int, size: Int, totalItems: Long): PageMeta =
            PageMeta(page = page, size = size, totalItems = totalItems, hasNext = (page + 1).toLong() * size < totalItems)
    }
}

/** Giữ lại cho tương thích với contract Android cũ: { items, page, size, totalElements, totalPages }. */
data class PageResponse<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val totalPages: Int = 0
) {
    companion object {
        fun <T> of(items: List<T>, page: Int, size: Int, totalElements: Long): PageResponse<T> =
            PageResponse(
                items = items,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = if (size <= 0) 0 else ((totalElements + size - 1) / size).toInt()
            )
    }
}
