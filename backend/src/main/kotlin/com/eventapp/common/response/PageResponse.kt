package com.eventapp.common.response

// TODO: PageResponse - khớp contract Android { items, page, size, totalElements, totalPages }
data class PageResponse<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val totalPages: Int = 0
)