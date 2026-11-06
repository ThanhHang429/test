package com.eventapp.ai.gateway

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

/**
 * Provider theo chuẩn OpenAI `/chat/completions` — dùng cho DeepSeek, OpenRouter,
 * OpenAI và mọi endpoint tương thích khác.
 *
 * `response_format = json_object` giữ đầu ra đúng JSON để [OutputValidator] kiểm tra.
 */
class OpenAiCompatibleAiProviderClient(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    override val providerCode: String,
    private val apiKey: String,
    private val baseUrl: String,
    override val modelCode: String
) : AiProviderClient {

    override fun complete(prompt: AiPrompt): AiCompletion {
        val started = System.currentTimeMillis()
        val body = requestBody(prompt)

        val raw = AiHttp.withRetry {
            try {
                restClient.post()
                    .uri("${baseUrl.trimEnd('/')}/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String::class.java)
            } catch (ex: RestClientResponseException) {
                throw AiProviderException(
                    ex.statusCode.value(),
                    "$providerCode trả mã ${ex.statusCode.value()}"
                )
            }
        } ?: throw AiProviderException(null, "$providerCode trả về nội dung rỗng")

        val root = objectMapper.readTree(raw)
        val choice = root.path("choices").firstOrNull()
            ?: throw AiProviderException(null, "$providerCode không trả về lựa chọn nào")
        val text = choice.path("message").path("content").asText("").trim()
        if (text.isEmpty()) throw AiProviderException(null, "$providerCode trả về nội dung rỗng")

        val usage = root.path("usage")
        return AiCompletion(
            text = text,
            inputTokens = usage.path("prompt_tokens").asInt(0).takeIf { it > 0 },
            outputTokens = usage.path("completion_tokens").asInt(0).takeIf { it > 0 },
            latencyMs = System.currentTimeMillis() - started
        )
    }

    private fun requestBody(prompt: AiPrompt): ObjectNode = objectMapper.createObjectNode().apply {
        put("model", modelCode)
        set<ArrayNode>(
            "messages",
            objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode().put("role", "system").put("content", prompt.system))
                .add(objectMapper.createObjectNode().put("role", "user").put("content", prompt.user))
        )
        put("temperature", prompt.temperature)
        put("max_tokens", prompt.maxOutputTokens)
        set<ObjectNode>("response_format", objectMapper.createObjectNode().put("type", "json_object"))
    }
}
