package com.eventapp.ai.gateway

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Component

/**
 * Hậu kiểm schema đầu ra (SRS 3.6, U08-R07): sai schema → thử lại 1 lần → fallback.
 * Đồng thời chuẩn hoá những gì AI không được phép tự quyết: độ dài lý do, kiểu số, danh sách id.
 */
@Component
class OutputValidator(
    private val objectMapper: ObjectMapper
) {

    fun validate(task: AiTask, raw: String): JsonNode {
        val root = try {
            objectMapper.readTree(raw)
        } catch (_: Exception) {
            throw AiInvalidOutputException("AI trả về không phải JSON hợp lệ")
        }
        if (root == null || !root.isObject) throw AiInvalidOutputException("AI trả về không phải JSON object")

        return when (task) {
            AiTask.RECOMMEND -> validateRecommend(root)
            AiTask.DESCRIPTION -> validateDescription(root)
            AiTask.ANALYTICS -> validateAnalytics(root)
            AiTask.CHAT -> validateChat(root)
            AiTask.MODERATION -> validateModeration(root)
        }
    }

    private fun validateRecommend(root: JsonNode): JsonNode {
        val items = root.path("items")
        if (!items.isArray) throw AiInvalidOutputException("Thiếu mảng items")
        val cleaned = objectMapper.createArrayNode()
        items.forEach { item ->
            val id = item.path("id").asLong(-1)
            val reason = item.path("reason").asText("").trim()
            if (id <= 0 || reason.isEmpty()) return@forEach
            cleaned.add(
                objectMapper.createObjectNode()
                    .put("id", id)
                    .put("reason", reason.take(REASON_MAX))
            )
        }
        if (cleaned.isEmpty && !items.isEmpty) throw AiInvalidOutputException("items không có mục hợp lệ")
        return objectMapper.createObjectNode().set<ArrayNode>("items", cleaned)
    }

    private fun validateDescription(root: JsonNode): JsonNode {
        val descriptions = root.path("descriptions")
        if (!descriptions.isArray) throw AiInvalidOutputException("Thiếu mảng descriptions")
        val cleaned = objectMapper.createArrayNode()
        descriptions.take(3).forEach { item ->
            val text = item.path("text").asText("").trim()
            if (text.isEmpty()) return@forEach
            val shortSummary = item.path("shortSummary").asText("").trim().take(SHORT_SUMMARY_MAX)
            cleaned.add(
                objectMapper.createObjectNode()
                    .put("text", text)
                    .put("shortSummary", shortSummary)
            )
        }
        if (cleaned.isEmpty) throw AiInvalidOutputException("descriptions rỗng")
        return objectMapper.createObjectNode().set<ArrayNode>("descriptions", cleaned)
    }

    private fun validateAnalytics(root: JsonNode): JsonNode {
        val summary = root.path("summary")
        if (!summary.isArray) throw AiInvalidOutputException("Thiếu mảng summary")
        val summaryClean = objectMapper.createArrayNode()
        summary.take(6).forEach { node ->
            val text = node.asText("").trim()
            if (text.isNotEmpty()) summaryClean.add(text)
        }

        val insights = root.path("insights")
        val insightsClean = objectMapper.createArrayNode()
        if (insights.isArray) {
            insights.take(10).forEach { item ->
                val text = item.path("text").asText("").trim()
                if (text.isEmpty()) return@forEach
                val type = item.path("type").asText("OBSERVATION").uppercase()
                    .takeIf { it == "OBSERVATION" || it == "SUGGESTION" } ?: "OBSERVATION"
                val confidence = item.path("confidence").asText("MEDIUM").uppercase()
                    .takeIf { it in CONFIDENCE_LEVELS } ?: "MEDIUM"
                insightsClean.add(
                    objectMapper.createObjectNode()
                        .put("type", type)
                        .put("text", text)
                        .put("relatedMetric", item.path("relatedMetric").asText("").take(60))
                        .put("confidence", confidence)
                )
            }
        }
        if (summaryClean.isEmpty && insightsClean.isEmpty) {
            throw AiInvalidOutputException("Không có nội dung phân tích hợp lệ")
        }
        val result = objectMapper.createObjectNode()
        result.set<ArrayNode>("summary", summaryClean)
        result.set<ArrayNode>("insights", insightsClean)
        return result
    }

    private fun validateChat(root: JsonNode): JsonNode {
        val answer = root.path("answer").asText("").trim()
        if (answer.isEmpty()) throw AiInvalidOutputException("Thiếu trường answer")
        val intent = root.path("intent").asText("OUT_OF_SCOPE").uppercase()
            .takeIf { it in CHAT_INTENTS } ?: "OUT_OF_SCOPE"
        return objectMapper.createObjectNode()
            .put("intent", intent)
            .put("answer", answer.take(2000))
    }

    private fun validateModeration(root: JsonNode): JsonNode {
        val spam = root.path("spamScore").asDouble(0.0).coerceIn(0.0, 1.0)
        val toxicity = root.path("toxicityScore").asDouble(0.0).coerceIn(0.0, 1.0)
        val reasons = objectMapper.createArrayNode()
        root.path("reasons").takeIf { it.isArray }?.take(5)?.forEach {
            val text = it.asText("").trim()
            if (text.isNotEmpty()) reasons.add(text.take(200))
        }
        val result = objectMapper.createObjectNode()
            .put("spamScore", spam)
            .put("toxicityScore", toxicity)
        result.set<ArrayNode>("reasons", reasons)
        return result
    }

    companion object {
        const val REASON_MAX = 140
        const val SHORT_SUMMARY_MAX = 160
        private val CONFIDENCE_LEVELS = setOf("LOW", "MEDIUM", "HIGH")
        private val CHAT_INTENTS = setOf(
            "SEARCH_EVENTS", "EVENT_QA", "POLICY_FAQ", "TRANSACTION", "OUT_OF_SCOPE"
        )
    }
}
