package com.eventapp.organizer.ai

import com.eventapp.ai.gateway.AiGateway
import com.eventapp.ai.gateway.AiQuotaExceededException
import com.eventapp.ai.gateway.AiTask
import com.eventapp.ai.gateway.PromptBuilder
import com.eventapp.catalog.repository.EventCategoryRepository
import com.eventapp.common.enums.EventLifecycleStatus
import com.eventapp.common.enums.OrganizerStatus
import com.eventapp.common.enums.OrderStatus
import com.eventapp.common.enums.RegistrationStatus
import com.eventapp.common.enums.ReviewStatus
import com.eventapp.common.enums.TicketStatus
import com.eventapp.common.exception.BusinessException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.entity.Event
import com.eventapp.event.repository.EventRepository
import com.eventapp.event.repository.TicketTypeRepository
import com.eventapp.identity.entity.OrganizerProfile
import com.eventapp.identity.repository.OrganizerProfileRepository
import com.eventapp.interaction.repository.ReviewRepository
import com.eventapp.ticketing.repository.OrderRepository
import com.eventapp.ticketing.repository.RegistrationRepository
import com.eventapp.ticketing.repository.TicketRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

/**
 * FR-O06 (AI viết mô tả) và FR-O27 (AI phân tích vận hành).
 *
 * Nguyên tắc bất biến:
 *  - AI chỉ tạo **bản nháp**; không có đường nào để AI đổi trạng thái hay dữ liệu sự kiện (O06, BR-017).
 *  - Mọi con số hiển thị lấy từ `metrics` do backend tính, văn bản AI chỉ tham chiếu (O27-R01).
 *  - AI lỗi/tắt → vẫn trả kết quả dựa trên luật (NFR-03).
 */
@Service
class OrganizerAiService(
    private val organizerProfileRepository: OrganizerProfileRepository,
    private val eventRepository: EventRepository,
    private val categoryRepository: EventCategoryRepository,
    private val registrationRepository: RegistrationRepository,
    private val orderRepository: OrderRepository,
    private val ticketRepository: TicketRepository,
    private val reviewRepository: ReviewRepository,
    private val ticketTypeRepository: TicketTypeRepository,
    private val aiGateway: AiGateway
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // ------------------------------------------------------------------
    // FR-O06
    // ------------------------------------------------------------------

    /** Không readOnly: bên trong ghi `ai_requests` + `integration_health_logs`. */
    @Transactional
    fun describe(userId: Long, request: EventDescriptionRequest): EventDescriptionResponse {
        requireApprovedOrganizer(userId)
        val category = categoryRepository.findById(request.categoryId!!).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy danh mục")
        if (!category.isActive) throw ForbiddenException(ErrorCode.CATEGORY_INACTIVE)

        request.eventId?.let { eventId ->
            val event = requireOwnedEvent(userId, eventId)
            if (event.lifecycleStatus == EventLifecycleStatus.CANCELLED) {
                throw ForbiddenException(ErrorCode.CONFLICT, "Sự kiện đã huỷ, không thể tạo mô tả")
            }
        }

        val tone = (request.tone ?: "PROFESSIONAL").uppercase()
        if (tone !in TONES) throw validationError("tone", "tone phải là PROFESSIONAL | FRIENDLY | FORMAL")
        val length = (request.length ?: "MEDIUM").uppercase()
        if (length !in LENGTHS) throw validationError("length", "length phải là SHORT | MEDIUM | LONG")
        val variants = (request.variants ?: 1).coerceIn(1, 3)

        val keyPoints = request.keyPoints.map { it.trim() }.filter { it.isNotEmpty() }
        if (keyPoints.size < 3) throw validationError("keyPoints", "Cần ít nhất 3 ý chính")

        val fields = mapOf(
            "title" to request.title.trim(),
            "category" to category.categoryName,
            "format" to (request.format ?: ""),
            "startTime" to (request.startTime ?: ""),
            "location" to (request.location ?: request.onlineInfo ?: ""),
            "audience" to (request.audience ?: ""),
            "keyPoints" to keyPoints,
            "tone" to tone,
            "length" to length,
            "variants" to variants
        )

        val missingInfo = buildMissingInfo(request)
        return try {
            val result = aiGateway.execute(
                task = AiTask.DESCRIPTION,
                userId = userId,
                eventId = request.eventId,
                fields = fields,
                inputSummary = "description:${request.title.take(60)}"
            )
            val descriptions = result.data.path("descriptions").map {
                DescriptionVariantDto(
                    text = it.path("text").asText(""),
                    shortSummary = it.path("shortSummary").asText("")
                )
            }
            val warnings = postCheckDescriptions(descriptions, fields)
            EventDescriptionResponse(
                descriptions = descriptions,
                missingInfo = missingInfo,
                warnings = warnings,
                source = "AI",
                aiRequestId = result.aiRequestId,
                correlationId = result.correlationId
            )
        } catch (ex: AiQuotaExceededException) {
            // O06: vượt quota là lỗi 429 có thời gian chờ, KHÔNG phải fallback.
            throw BusinessException(
                ErrorCode.RATE_LIMITED,
                ex.message ?: ErrorCode.RATE_LIMITED.defaultMessage,
                emptyList(),
                mapOf("retryAfterSeconds" to ex.retryAfterSeconds)
            )
        } catch (ex: Exception) {
            log.info("AI mô tả không dùng được ({}), trả về thông báo để Organizer nhập tay", ex.message)
            EventDescriptionResponse(
                descriptions = emptyList(),
                missingInfo = missingInfo,
                warnings = listOf("Không tạo được mô tả bằng AI, bạn vẫn có thể nhập tay bình thường."),
                source = "RULE_UNAVAILABLE",
                aiRequestId = null,
                correlationId = null
            )
        }
    }

    /**
     * Hậu kiểm O06: bất kỳ số tiền / ngày / SĐT / email / URL nào trong văn bản mà không có
     * trong dữ liệu nhập đều bị cảnh báo (AI không được tự bịa).
     */
    private fun postCheckDescriptions(
        descriptions: List<DescriptionVariantDto>,
        fields: Map<String, Any?>
    ): List<String> {
        val sourceText = fields.values.joinToString(" ") { value ->
            when (value) {
                is List<*> -> value.joinToString(" ") { it?.toString().orEmpty() }
                else -> value?.toString().orEmpty()
            }
        }.lowercase()

        val warnings = LinkedHashSet<String>()
        descriptions.forEach { variant ->
            SUSPICIOUS_PATTERNS.forEach { (label, regex) ->
                regex.findAll(variant.text).forEach { match ->
                    val token = match.value.lowercase()
                    if (!sourceText.contains(token)) {
                        warnings += "Văn bản có $label không nằm trong dữ liệu bạn nhập: ${match.value}"
                    }
                }
            }
        }
        return warnings.take(10)
    }

    private fun buildMissingInfo(request: EventDescriptionRequest): List<String> {
        val missing = mutableListOf<String>()
        if (request.format.isNullOrBlank()) missing += "Hình thức tổ chức (trực tiếp / trực tuyến)"
        if (request.startTime.isNullOrBlank()) missing += "Thời gian bắt đầu"
        if (request.location.isNullOrBlank() && request.onlineInfo.isNullOrBlank()) {
            missing += "Địa điểm hoặc thông tin tham dự trực tuyến"
        }
        if (request.audience.isNullOrBlank()) missing += "Đối tượng tham dự"
        return missing
    }

    // ------------------------------------------------------------------
    // FR-O27
    // ------------------------------------------------------------------

    @Transactional
    fun analyze(userId: Long, eventId: Long): EventAnalysisResponse {
        requireApprovedOrganizer(userId)
        val event = requireOwnedEvent(userId, eventId)
        val metrics = buildMetrics(event)
        val insufficient = metrics.getValue("insufficientData") as Boolean

        if (insufficient) {
            return EventAnalysisResponse(
                eventId = eventId,
                metrics = metrics,
                summary = listOf(
                    "Chưa đủ dữ liệu để phân tích. Cần tối thiểu 7 ngày kể từ lúc mở bán " +
                        "hoặc 20 đăng ký/đơn hợp lệ."
                ),
                insights = emptyList(),
                disclaimer = DISCLAIMER,
                dataLimitations = listOf("Số liệu hiện có quá ít để đưa ra nhận xét đáng tin cậy."),
                insufficientData = true,
                source = "RULE",
                generatedAt = Instant.now(),
                aiRequestId = null,
                correlationId = null
            )
        }

        return try {
            val result = aiGateway.execute(
                task = AiTask.ANALYTICS,
                userId = userId,
                eventId = eventId,
                fields = mapOf(
                    "eventTitle" to PromptBuilder.clip(event.title, 120),
                    "metrics" to metrics.filterKeys { it != "insufficientData" }
                ),
                inputSummary = "analytics:event=$eventId"
            )
            val summary = result.data.path("summary").map { it.asText() }
            val insights = result.data.path("insights").map {
                InsightDto(
                    type = it.path("type").asText("OBSERVATION"),
                    text = it.path("text").asText(""),
                    relatedMetric = it.path("relatedMetric").asText("").takeIf(String::isNotBlank),
                    confidence = it.path("confidence").asText("MEDIUM")
                )
            }
            EventAnalysisResponse(
                eventId = eventId,
                metrics = metrics,
                summary = summary,
                insights = insights,
                disclaimer = DISCLAIMER,
                dataLimitations = limitations(metrics),
                insufficientData = false,
                source = "AI",
                generatedAt = Instant.now(),
                aiRequestId = result.aiRequestId,
                correlationId = result.correlationId
            )
        } catch (ex: AiQuotaExceededException) {
            // O27: hết quota thì trả bản phân tích theo luật kèm ghi chú giới hạn.
            log.info("Hết quota phân tích AI cho sự kiện {}", eventId)
            val ruleInsights = ruleInsights(metrics)
            return EventAnalysisResponse(
                eventId = eventId,
                metrics = metrics,
                summary = ruleSummary(metrics),
                insights = ruleInsights,
                disclaimer = DISCLAIMER,
                dataLimitations = limitations(metrics) +
                    "Đã hết lượt phân tích AI trong ngày, kết quả dưới đây được tạo bằng luật.",
                insufficientData = false,
                source = "RULE",
                generatedAt = Instant.now(),
                aiRequestId = null,
                correlationId = null
            )
        } catch (ex: Exception) {
            log.info("AI phân tích không dùng được ({}), dùng nhận xét theo luật", ex.message)
            val ruleInsights = ruleInsights(metrics)
            EventAnalysisResponse(
                eventId = eventId,
                metrics = metrics,
                summary = ruleSummary(metrics),
                insights = ruleInsights,
                disclaimer = DISCLAIMER,
                dataLimitations = limitations(metrics) + "Nhận xét được tạo bằng luật do AI không khả dụng.",
                insufficientData = false,
                source = "RULE",
                generatedAt = Instant.now(),
                aiRequestId = null,
                correlationId = null
            )
        }
    }

    private fun buildMetrics(event: Event): Map<String, Any?> {
        val eventId = event.eventId!!
        val registrations = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED)
        val capacity = event.capacityLimit?.toLong()
            ?: ticketTypeCapacity(eventId)
        val fillRate = if (capacity != null && capacity > 0) {
            (registrations.toDouble() / capacity).coerceIn(0.0, 1.0)
        } else 0.0
        val revenue = orderRepository.sumTotalByEventAndStatus(eventId, OrderStatus.PAID)
        val checkedIn = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.CHECKED_IN)
        val checkinRate = if (registrations > 0) checkedIn.toDouble() / registrations else 0.0
        val avgRating = reviewRepository.averageRating(eventId) ?: 0.0
        val reviewCount = reviewRepository.countByEventIdAndStatus(eventId, ReviewStatus.VISIBLE)
        val daysToEvent = Duration.between(Instant.now(), event.startAt).toDays().toInt()
        val saleOpenedAt = event.registrationStartAt ?: event.createdAt
        val daysSinceSale = saleOpenedAt?.let { Duration.between(it, Instant.now()).toDays().toInt() } ?: 0

        return linkedMapOf(
            "registrations" to registrations,
            "capacity" to (capacity ?: 0L),
            "fillRate" to round4(fillRate),
            "revenue" to revenue.toDouble(),
            "checkedIn" to checkedIn,
            "checkinRate" to round4(checkinRate),
            "avgRating" to round4(avgRating),
            "reviewCount" to reviewCount,
            "daysToEvent" to daysToEvent,
            "daysSinceSaleOpened" to daysSinceSale,
            "insufficientData" to (daysSinceSale < 7 && registrations < MIN_REGISTRATIONS)
        )
    }

    /** Sức chứa suy ra từ tổng số lượng vé của các loại vé đang bán. */
    private fun ticketTypeCapacity(eventId: Long): Long? {
        val total = ticketTypeRepository.findByEventIdOrderByPriceAsc(eventId)
            .sumOf { (it.quantityTotal ?: 0).toLong() }
        return total.takeIf { it > 0 }
    }

    private fun ruleSummary(metrics: Map<String, Any?>): List<String> {
        val registrations = (metrics["registrations"] as? Long) ?: 0L
        val capacity = (metrics["capacity"] as? Long) ?: 0L
        val fillRate = (metrics["fillRate"] as? Double) ?: 0.0
        val daysToEvent = (metrics["daysToEvent"] as? Int) ?: 0
        return listOf(
            "Ghi nhận $registrations đăng ký trên sức chứa $capacity " +
                "(tỷ lệ lấp đầy ${(fillRate * 100).round1()}%).",
            if (daysToEvent > 0) "Sự kiện còn $daysToEvent ngày." else "Sự kiện đã diễn ra."
        )
    }

    private fun ruleInsights(metrics: Map<String, Any?>): List<InsightDto> {
        val insights = mutableListOf<InsightDto>()
        val fillRate = (metrics["fillRate"] as? Double) ?: 0.0
        val daysToEvent = (metrics["daysToEvent"] as? Int) ?: 0
        val registrations = (metrics["registrations"] as? Long) ?: 0L
        val avgRating = (metrics["avgRating"] as? Double) ?: 0.0

        if (fillRate < 0.3 && daysToEvent in 1..14) {
            insights += InsightDto(
                "SUGGESTION",
                "Tỷ lệ lấp đầy dưới 30% khi chỉ còn $daysToEvent ngày: nên tăng truyền thông.",
                "fillRate", "MEDIUM"
            )
        }
        if (registrations == 0L) {
            insights += InsightDto("OBSERVATION", "Chưa có đăng ký nào.", "registrations", "HIGH")
        }
        if (avgRating > 0) {
            insights += InsightDto(
                "OBSERVATION",
                "Điểm đánh giá trung bình ${avgRating.round1()}/5.",
                "avgRating", "MEDIUM"
            )
        }
        return insights
    }

    private fun limitations(metrics: Map<String, Any?>): List<String> {
        val items = mutableListOf<String>()
        val capacity = (metrics["capacity"] as? Long) ?: 0L
        if (capacity <= 0) items += "Chưa cấu hình sức chứa nên tỷ lệ lấp đầy chỉ mang tính tham khảo."
        val reviewCount = (metrics["reviewCount"] as? Long) ?: 0L
        if (reviewCount < 3) items += "Số đánh giá còn ít, điểm trung bình chưa ổn định."
        return items
    }

    // ------------------------------------------------------------------
    // Quyền
    // ------------------------------------------------------------------

    private fun requireApprovedOrganizer(userId: Long): OrganizerProfile {
        val profile = organizerProfileRepository.findByUserId(userId)
            ?: throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn chưa có hồ sơ Organizer")
        if (profile.status != OrganizerStatus.APPROVED) {
            throw ForbiddenException(
                ErrorCode.FORBIDDEN,
                "Hồ sơ Organizer đang ở trạng thái ${profile.status.name}, chưa dùng được tính năng AI"
            )
        }
        return profile
    }

    private fun requireOwnedEvent(userId: Long, eventId: Long): Event {
        val profile = requireApprovedOrganizer(userId)
        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.organizerProfileId != profile.organizerProfileId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Sự kiện không thuộc quyền quản lý của bạn")
        }
        return event
    }

    private fun round4(value: Double): Double =
        BigDecimal(value).setScale(4, RoundingMode.HALF_UP).toDouble()

    private fun Double.round1(): String = String.format("%.1f", this)

    companion object {
        const val DISCLAIMER = "Đây là ước tính từ dữ liệu hiện có, không phải cam kết kết quả."
        const val MIN_REGISTRATIONS = 20L

        private val TONES = setOf("PROFESSIONAL", "FRIENDLY", "FORMAL")
        private val LENGTHS = setOf("SHORT", "MEDIUM", "LONG")

        private val SUSPICIOUS_PATTERNS = listOf(
            "số tiền" to Regex("""\d{1,3}([.,]\d{3})+\s*(đ|vnđ|vnd|₫)?""", RegexOption.IGNORE_CASE),
            "ngày" to Regex("""\b\d{1,2}[/-]\d{1,2}([/-]\d{2,4})?\b"""),
            "số điện thoại" to Regex("""(?<!\d)0\d{9,10}(?!\d)"""),
            "email" to Regex("""[\w.+-]+@[\w-]+\.[\w.]+"""),
            "liên kết" to Regex("""(https?://|www\.)\S+""", RegexOption.IGNORE_CASE)
        )
    }
}
