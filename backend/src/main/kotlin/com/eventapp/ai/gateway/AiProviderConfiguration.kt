package com.eventapp.ai.gateway

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

/**
 * Chọn nhà cung cấp AI theo `app.ai.provider`.
 *
 * Hỗ trợ mô hình bên thứ ba:
 *  - `gemini`  — Google Generative Language API (native REST).
 *  - `deepseek` / `openrouter` / `openai` / `openai-compatible` — chuẩn OpenAI `/chat/completions`.
 *  - `mock` — provider giả lập tất định, không cần API key (mặc định cho dev/test).
 *
 * `AI_BASE_URL` và `AI_MODEL` để trống thì dùng mặc định của từng provider; đặt giá trị
 * khác khi cần trỏ sang endpoint/proxy riêng. API key chỉ đọc từ biến môi trường (CF-NFR-01).
 */
@Configuration
class AiProviderConfiguration {

    @Bean
    fun aiProviderClient(
        @Value("\${app.ai.provider:mock}") provider: String,
        @Value("\${app.ai.api-key:}") apiKey: String,
        @Value("\${app.ai.base-url:}") baseUrl: String,
        @Value("\${app.ai.model:}") model: String,
        @Value("\${app.ai.connect-timeout-seconds:3}") connectTimeoutSeconds: Int,
        @Value("\${app.ai.read-timeout-seconds:30}") readTimeoutSeconds: Int,
        objectMapper: ObjectMapper
    ): AiProviderClient {
        val code = provider.trim().lowercase()
        if (code.isEmpty() || code == MOCK) return MockAiProviderClient(objectMapper)

        if (apiKey.isBlank()) {
            throw IllegalStateException(
                "app.ai.provider=$code nhưng chưa cấu hình AI_API_KEY (chỉ đặt trong biến môi trường)"
            )
        }

        val restClient = AiHttp.restClient(connectTimeoutSeconds, readTimeoutSeconds)
        val customBaseUrl = baseUrl.trim().trimEnd('/')
        val customModel = model.trim()

        return when (code) {
            "gemini", "google" -> GeminiAiProviderClient(
                restClient = restClient,
                objectMapper = objectMapper,
                apiKey = apiKey,
                baseUrl = customBaseUrl.ifBlank { GEMINI_BASE_URL },
                modelCode = customModel.ifBlank { GEMINI_MODEL }
            )

            "deepseek" -> openAiCompatible(
                restClient, objectMapper, code, apiKey,
                customBaseUrl.ifBlank { DEEPSEEK_BASE_URL },
                customModel.ifBlank { DEEPSEEK_MODEL }
            )

            "openrouter" -> openAiCompatible(
                restClient, objectMapper, code, apiKey,
                customBaseUrl.ifBlank { OPENROUTER_BASE_URL },
                customModel.ifBlank { OPENROUTER_MODEL }
            )

            "openai" -> openAiCompatible(
                restClient, objectMapper, code, apiKey,
                customBaseUrl.ifBlank { OPENAI_BASE_URL },
                customModel.ifBlank { OPENAI_MODEL }
            )

            "openai-compatible" -> {
                if (customBaseUrl.isBlank()) {
                    throw IllegalStateException("app.ai.provider=openai-compatible cần AI_BASE_URL")
                }
                if (customModel.isBlank()) {
                    throw IllegalStateException("app.ai.provider=openai-compatible cần AI_MODEL")
                }
                openAiCompatible(restClient, objectMapper, code, apiKey, customBaseUrl, customModel)
            }

            else -> throw IllegalStateException(
                "app.ai.provider không hợp lệ: $provider. " +
                    "Chọn một trong: mock | gemini | deepseek | openrouter | openai | openai-compatible"
            )
        }
    }

    private fun openAiCompatible(
        restClient: RestClient,
        objectMapper: ObjectMapper,
        providerCode: String,
        apiKey: String,
        baseUrl: String,
        model: String
    ): AiProviderClient = OpenAiCompatibleAiProviderClient(
        restClient = restClient,
        objectMapper = objectMapper,
        providerCode = providerCode,
        apiKey = apiKey,
        baseUrl = baseUrl,
        modelCode = model
    )

    companion object {
        private const val MOCK = "mock"

        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com"
        private const val GEMINI_MODEL = "gemini-2.0-flash"

        private const val DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1"
        private const val DEEPSEEK_MODEL = "deepseek-chat"

        private const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"
        private const val OPENROUTER_MODEL = "deepseek/deepseek-chat"

        private const val OPENAI_BASE_URL = "https://api.openai.com/v1"
        private const val OPENAI_MODEL = "gpt-4o-mini"
    }
}
