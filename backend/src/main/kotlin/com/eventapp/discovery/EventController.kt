package com.eventapp.discovery

import com.eventapp.common.exception.validationError
import com.eventapp.common.response.ApiResponse
import com.eventapp.common.response.PageMeta
import com.eventapp.common.security.CurrentUser
import com.eventapp.discovery.dto.EventCardDto
import com.eventapp.discovery.dto.EventDetailDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

@Tag(name = "Khám phá sự kiện", description = "FR-U04 danh sách/tìm kiếm, FR-U05 chi tiết")
@RestController
@RequestMapping("/api/v1/events")
class EventController(
    private val service: EventDiscoveryService
) {

    @Operation(summary = "API-03 — Danh sách, tìm kiếm và lọc sự kiện công khai")
    @GetMapping
    fun search(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) categoryIds: List<Long>?,
        @RequestParam(required = false) dateFrom: LocalDate?,
        @RequestParam(required = false) dateTo: LocalDate?,
        @RequestParam(required = false) city: String?,
        @RequestParam(required = false) format: String?,
        @RequestParam(required = false) priceType: String?,
        @RequestParam(required = false) priceMin: Long?,
        @RequestParam(required = false) priceMax: Long?,
        @RequestParam(required = false, defaultValue = "false") availableOnly: Boolean,
        @RequestParam(required = false, defaultValue = "false") includePast: Boolean,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ApiResponse<List<EventCardDto>> {
        val safePage = page.coerceAtLeast(0)
        // U04-R08: size > 50 bị hạ về 50, không báo lỗi
        val safeSize = size.coerceIn(1, MAX_PAGE_SIZE)

        if (priceMin != null && priceMax != null && priceMin > priceMax) {
            throw validationError("priceMin", "priceMin không được lớn hơn priceMax")
        }
        if (priceMin != null && priceMin < 0) throw validationError("priceMin", "priceMin phải >= 0")
        if (priceMax != null && priceMax < 0) throw validationError("priceMax", "priceMax phải >= 0")

        val from = dateFrom?.atStartOfDay(ZoneId.of(TZ))?.toInstant()
        val to = dateTo?.plusDays(1)?.atStartOfDay(ZoneId.of(TZ))?.toInstant()
        if (from != null && to != null) {
            if (!from.isBefore(to)) throw validationError("dateFrom", "dateFrom phải nhỏ hơn hoặc bằng dateTo")
            if (Duration.between(from, to).toDays() > MAX_DATE_RANGE_DAYS) {
                throw validationError("dateTo", "Khoảng thời gian tối đa $MAX_DATE_RANGE_DAYS ngày")
            }
        }
        val categories = categoryIds?.filter { it > 0 }?.distinct() ?: emptyList()
        if (categories.size > MAX_CATEGORIES) {
            throw validationError("categoryIds", "Chọn tối đa $MAX_CATEGORIES danh mục")
        }

        val sortKey = when (sort?.uppercase()) {
            null, "" -> if (q.isNullOrBlank()) "START_TIME_ASC" else "RELEVANCE"
            "RELEVANCE", "START_TIME_ASC", "NEWEST", "PRICE_ASC", "PRICE_DESC", "POPULAR" -> sort.uppercase()
            else -> throw validationError("sort", "Giá trị sort không hợp lệ")
        }

        val criteria = EventSearchRepository.Criteria(
            q = q,
            categoryIds = categories,
            dateFrom = from,
            dateTo = to,
            city = city,
            format = format,
            priceType = priceType,
            priceMin = priceMin,
            priceMax = priceMax,
            availableOnly = availableOnly,
            includePast = includePast,
            sort = sortKey,
            page = safePage,
            size = safeSize
        )

        val (items, total) = service.search(criteria, CurrentUser.orNull()?.userId)
        return ApiResponse.ok(items, PageMeta.of(safePage, safeSize, total))
    }

    @Operation(summary = "API-04 — Chi tiết sự kiện (token tùy chọn để biết yêu thích/đăng ký)")
    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<EventDetailDto> =
        ApiResponse.ok(service.detail(id, CurrentUser.orNull()?.userId))

    companion object {
        const val MAX_PAGE_SIZE = 50
        const val MAX_CATEGORIES = 10
        const val MAX_DATE_RANGE_DAYS = 366L
        const val TZ = "Asia/Ho_Chi_Minh"
    }
}
