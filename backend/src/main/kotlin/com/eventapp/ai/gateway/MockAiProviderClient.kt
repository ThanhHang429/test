package com.eventapp.ai.gateway

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

/**
 * Nhà cung cấp giả lập — chạy hoàn toàn cục bộ, không cần API key, kết quả **xác định**
 * (cùng đầu vào → cùng đầu ra) nên test lặp lại được.
 *
 * Cách hoạt động: prompt `user` do [PromptBuilder] dựng luôn là JSON có `task` và dữ liệu
 * thật của hệ thống. Mock đọc đúng JSON đó rồi sinh đầu ra theo khuôn schema của từng tác vụ,
 * nhờ vậy toàn bộ luồng hậu kiểm/ghép dữ liệu vẫn được kiểm thử thật.
 *
 * Bean được tạo trong [AiProviderConfiguration] khi `app.ai.provider=mock`.
 */
class MockAiProviderClient(
    private val objectMapper: ObjectMapper
) : AiProviderClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override val providerCode: String = "mock"
    override val modelCode: String = "mock-rules-v1"

    override fun complete(prompt: AiPrompt): AiCompletion {
        val started = System.currentTimeMillis()
        val payload = objectMapper.readTree(prompt.user)
        val task = payload.path("task").asText(prompt.task.code)

        val output: JsonNode = when (task) {
            AiTask.RECOMMEND.code -> recommend(payload)
            AiTask.DESCRIPTION.code -> describe(payload)
            AiTask.ANNOUNCEMENT.code -> announce(payload)
            AiTask.ANALYTICS.code -> analyze(payload)
            else -> objectMapper.createObjectNode().put("unsupported", true)
        }

        val text = objectMapper.writeValueAsString(output)
        val latency = System.currentTimeMillis() - started
        log.debug("Mock AI {} sinh {} ký tự trong {} ms", task, text.length, latency)

        return AiCompletion(
            text = text,
            inputTokens = estimateTokens(prompt.system.length + prompt.user.length),
            outputTokens = estimateTokens(text.length),
            latencyMs = latency
        )
    }

    // ------------------------------------------------------------------
    // RECOMMEND
    // ------------------------------------------------------------------

    private fun recommend(payload: JsonNode): ObjectNode {
        val preferredCategories = payload.path("preferences").path("categories")
            .mapNotNull { it.asText().takeIf(String::isNotBlank) }
            .toSet()
        val city = payload.path("preferences").path("city").asText("")
        val candidates = payload.path("candidates")

        val items = objectMapper.createArrayNode()
        var taken = 0
        for (candidate in candidates) {
            if (taken >= MAX_RECOMMENDATIONS) break
            val categories = candidate.path("categories").map { it.asText() }
            val matched = categories.firstOrNull { preferredCategories.contains(it) }
            val reason = when {
                matched != null -> "Phù hợp với sở thích $matched của bạn"
                city.isNotBlank() && candidate.path("city").asText("") == city ->
                    "Diễn ra tại $city, nơi bạn quan tâm"
                candidate.path("free").asBoolean(false) -> "Miễn phí và đang được quan tâm"
                else -> "Đang được nhiều người quan tâm"
            }
            items.add(
                objectMapper.createObjectNode()
                    .put("id", candidate.path("id").asLong())
                    .put("reason", reason.take(REASON_MAX_LENGTH))
            )
            taken++
        }

        return objectMapper.createObjectNode().set<ObjectNode>("items", items)
    }

    // ------------------------------------------------------------------
    // DESCRIPTION
    // ------------------------------------------------------------------

    private fun describe(payload: JsonNode): ObjectNode {
        val title = payload.path("title").asText("Sự kiện")
        val category = payload.path("category").asText("")
        val format = payload.path("format").asText("")
        val startTime = payload.path("startTime").asText("")
        val location = payload.path("location").asText("")
        val audience = payload.path("audience").asText("")
        val tone = payload.path("tone").asText("PROFESSIONAL")
        val length = payload.path("length").asText("MEDIUM")
        val variants = payload.path("variants").asInt(1).coerceIn(1, 3)
        val keyPoints = payload.path("keyPoints").map { it.asText() }.filter { it.isNotBlank() }

        val opening = when (tone) {
            "FRIENDLY" -> "$title là điểm hẹn bạn không nên bỏ lỡ."
            "FORMAL" -> "Ban tổ chức trân trọng giới thiệu $title."
            else -> "$title — sự kiện đáng chú ý dành cho cộng đồng quan tâm."
        }

        val infoLine = buildString {
            if (format.isNotBlank()) append("Hình thức: ").append(formatVi(format)).append(". ")
            if (startTime.isNotBlank()) append("Thời gian: ").append(startTime).append(". ")
            if (location.isNotBlank()) append("Địa điểm: ").append(location).append(". ")
            if (audience.isNotBlank()) append("Dành cho: ").append(audience).append(". ")
        }.trim()

        val body = if (keyPoints.isEmpty()) {
            "[Cần bổ sung: nội dung chính của sự kiện]"
        } else {
            keyPoints.joinToString(separator = "\n") { "• $it" }
        }

        val closing = when (length) {
            "SHORT" -> "Thông tin chi tiết sẽ được cập nhật trên trang sự kiện."
            "LONG" -> "Đừng bỏ lỡ cơ hội tham gia. Thông tin chi tiết và lịch trình được cập nhật " +
                "liên tục trên trang sự kiện; vui lòng theo dõi để không bỏ sót thay đổi."
            else -> "Theo dõi trang sự kiện để cập nhật thông tin mới nhất."
        }

        val categoryLine = if (category.isNotBlank()) "Lĩnh vực: $category." else "[Cần bổ sung: danh mục]"

        val descriptions = objectMapper.createArrayNode()
        repeat(variants) { index ->
            val header = if (index == 0) opening else "$opening (phương án ${index + 1})"
            val text = listOf(header, categoryLine, infoLine, body, closing)
                .filter { it.isNotBlank() }
                .joinToString(separator = "\n\n")
            val shortSummary = "$title" + if (category.isNotBlank()) " — $category" else ""
            descriptions.add(
                objectMapper.createObjectNode()
                    .put("text", text)
                    .put("shortSummary", shortSummary.take(SHORT_SUMMARY_MAX))
            )
        }

        return objectMapper.createObjectNode().set<ObjectNode>("descriptions", descriptions)
    }

    // ------------------------------------------------------------------
    // ANNOUNCEMENT
    // ------------------------------------------------------------------

    private fun announce(payload: JsonNode): ObjectNode {
        val topic = payload.path("topic").asText("Thông báo").trim()
        val eventTitle = payload.path("eventTitle").asText("").trim()
        val audience = payload.path("audience").asText("").trim()
        val tone = payload.path("tone").asText("PROFESSIONAL")
        val length = payload.path("length").asText("MEDIUM")
        val variants = payload.path("variants").asInt(1).coerceIn(1, 3)
        val keyPoints = payload.path("keyPoints").map { it.asText() }.filter { it.isNotBlank() }

        val opening = when (tone) {
            "FRIENDLY" -> "Xin chào, Ban tổ chức có một cập nhật dành cho bạn."
            "FORMAL" -> "Ban tổ chức trân trọng thông báo tới quý vị."
            else -> "Thông báo từ Ban tổ chức."
        }
        val heading = if (eventTitle.isNotBlank()) "$eventTitle — $topic" else topic
        val body = if (keyPoints.isEmpty()) {
            "[Cần bổ sung: chi tiết cụ thể của thông báo]"
        } else {
            keyPoints.joinToString(separator = "\n") { "• $it" }
        }
        val closing = when (length) {
            "SHORT" -> "Trân trọng."
            "LONG" -> "Vui lòng theo dõi trang sự kiện để cập nhật các thay đổi tiếp theo. " +
                "Ban tổ chức trân trọng cảm ơn."
            else -> "Vui lòng theo dõi trang sự kiện để cập nhật thông tin mới nhất."
        }
        val audienceLine = if (audience.isNotBlank()) "Kính gửi: $audience." else ""

        val items = objectMapper.createArrayNode()
        repeat(variants) { index ->
            val header = if (index == 0) heading else "$heading (phương án ${index + 1})"
            val text = listOf(opening, audienceLine, header, body, closing)
                .filter { it.isNotBlank() }
                .joinToString(separator = "\n\n")
            items.add(
                objectMapper.createObjectNode()
                    .put("text", text)
                    .put("shortSummary", heading.take(SHORT_SUMMARY_MAX))
            )
        }

        return objectMapper.createObjectNode().set<ObjectNode>("variants", items)
    }

    // ------------------------------------------------------------------
    // ANALYTICS
    // ------------------------------------------------------------------

    private fun analyze(payload: JsonNode): ObjectNode {
        val title = payload.path("eventTitle").asText("Sự kiện")
        val metrics = payload.path("metrics")
        val registrations = metrics.path("registrations").asInt(0)
        val capacity = metrics.path("capacity").asInt(0)
        val fillRate = metrics.path("fillRate").asDouble(0.0)
        val daysToEvent = metrics.path("daysToEvent").asInt(0)
        val avgRating = metrics.path("avgRating").asDouble(0.0)
        val revenue = metrics.path("revenue").asDouble(0.0)

        val summary = objectMapper.createArrayNode()
        summary.add(
            "Tính đến thời điểm hiện tại, $title ghi nhận $registrations đăng ký trên tổng " +
                "sức chứa $capacity, tương đương tỷ lệ lấp đầy ${(fillRate * 100).round1()}%."
        )
        if (daysToEvent > 0) {
            summary.add("Sự kiện còn $daysToEvent ngày nữa sẽ diễn ra.")
        } else {
            summary.add("Sự kiện đã diễn ra; số liệu phản ánh kết quả sau sự kiện.")
        }
        if (avgRating > 0) {
            summary.add("Điểm đánh giá trung bình hiện tại là ${avgRating.round1()}/5 từ người tham dự.")
        }

        val insights = objectMapper.createArrayNode()
        if (fillRate < 0.3 && daysToEvent in 1..14) {
            insights.add(insight("SUGGESTION", "Tỷ lệ lấp đầy dưới 30% trong khi chỉ còn $daysToEvent ngày: " +
                "nên đẩy mạnh truyền thông hoặc cân nhắc ưu đãi giá sớm.", "fillRate", "MEDIUM"))
        }
        if (registrations == 0) {
            insights.add(insight("OBSERVATION", "Chưa có đăng ký nào được ghi nhận.", "registrations", "HIGH"))
        }
        if (fillRate >= 0.8) {
            insights.add(insight("OBSERVATION", "Tỷ lệ lấp đầy đã đạt ${(fillRate * 100).round1()}%, " +
                "gần mức tối đa.", "fillRate", "HIGH"))
        }
        if (avgRating >= 4.0) {
            insights.add(insight("OBSERVATION", "Điểm đánh giá ${avgRating.round1()}/5 cho thấy mức độ hài lòng cao.",
                "avgRating", "MEDIUM"))
        } else if (avgRating > 0) {
            insights.add(insight("SUGGESTION", "Điểm đánh giá ${avgRating.round1()}/5 còn thấp; " +
                "nên xem lại phản hồi của người tham dự.", "avgRating", "LOW"))
        }
        if (revenue > 0) {
            insights.add(insight("OBSERVATION", "Doanh thu đã xác nhận được tổng hợp trong khối số liệu.",
                "revenue", "HIGH"))
        }

        val result = objectMapper.createObjectNode()
        result.set<ObjectNode>("summary", summary)
        result.set<ObjectNode>("insights", insights)
        return result
    }

    private fun insight(type: String, text: String, metric: String, confidence: String): ObjectNode =
        objectMapper.createObjectNode()
            .put("type", type)
            .put("text", text)
            .put("relatedMetric", metric)
            .put("confidence", confidence)

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun formatVi(format: String): String = when (format.uppercase()) {
        "OFFLINE", "ONSITE" -> "trực tiếp"
        "ONLINE" -> "trực tuyến"
        "HYBRID" -> "kết hợp trực tiếp và trực tuyến"
        else -> format
    }

    private fun estimateTokens(chars: Int): Int = (chars / 4).coerceAtLeast(1)

    private fun Double.round1(): String = String.format("%.1f", this)

    companion object {
        private const val MAX_RECOMMENDATIONS = 10
        private const val REASON_MAX_LENGTH = 140
        private const val SHORT_SUMMARY_MAX = 160
    }
}
