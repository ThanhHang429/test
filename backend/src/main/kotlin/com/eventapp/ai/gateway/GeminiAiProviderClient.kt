package com.eventapp.ai.gateway

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

/**
 * Provider Google Gemini qua REST `generateContent`.
 *
 * Dùng `responseMimeType = application/json` để mô hình trả JSON đúng schema mà
 * [OutputValidator] yêu cầu, giảm số lần phải thử lại.
 */
class GeminiAiProviderClient(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val apiKey: String,
    private val baseUrl: String,
    override val modelCode: String
) : AiProviderClient {

    override val providerCode: String = "gemini"

    override fun complete(prompt: AiPrompt): AiCompletion {
        val started = System.currentTimeMillis()
        val body = requestBody(prompt)

        val raw = AiHttp.withRetry {
            try {
                restClient.post()
                    .uri("${baseUrl.trimEnd('/')}/v1beta/models/{model}:generateContent", modelCode)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String::class.java)
            } catch (ex: RestClientResponseException) {
                throw AiProviderException(ex.statusCode.value(), "Gemini trả mã ${ex.statusCode.value()}")
            }
        } ?: throw AiProviderException(null, "Gemini trả về nội dung rỗng")

        val root = objectMapper.readTree(raw)
        val blocked = root.path("promptFeedback").path("blockReason").asText("")
        if (blocked.isNotBlank()) {
            throw AiProviderException(null, "Gemini từ chối nội dung đầu vào ($blocked)")
        }

        val candidate = root.path("candidates").firstOrNull()
            ?: throw AiProviderException(null, "Gemini không trả về candidate nào")
        val text = candidate.path("content").path("parts")
            .joinToString(separator = "") { it.path("text").asText("") }
            .trim()
        if (text.isEmpty()) throw AiProviderException(null, "Gemini trả về nội dung rỗng")

        val usage = root.path("usageMetadata")
        return AiCompletion(
            text = text,
            inputTokens = usage.path("promptTokenCount").asInt(0).takeIf { it > 0 },
            outputTokens = usage.path("candidatesTokenCount").asInt(0).takeIf { it > 0 },
            latencyMs = System.currentTimeMillis() - started
        )
    }

    private fun requestBody(prompt: AiPrompt): ObjectNode = objectMapper.createObjectNode().apply {
        set<ObjectNode>(
            "systemInstruction",
            objectMapper.createObjectNode().set<ArrayNode>(
                "parts",
                objectMapper.createArrayNode().add(
                    objectMapper.createObjectNode().put("text", prompt.system)
                )
            )
        )
        set<ArrayNode>(
            "contents",
            objectMapper.createArrayNode().add(
                objectMapper.createObjectNode()
                    .put("role", "user")
                    .set<ArrayNode>(
                        "parts",
                        objectMapper.createArrayNode().add(
                            objectMapper.createObjectNode().put("text", prompt.user)
                        )
                    )
            )
        )
        set<ObjectNode>(
            "generationConfig",
            objectMapper.createObjectNode().apply {
                put("temperature", prompt.temperature)
                put("maxOutputTokens", prompt.maxOutputTokens)
                put("responseMimeType", "application/json")
            }
        )
    }
}
