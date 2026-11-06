package com.eventapp.ai.gateway

/**
 * Cổng tới nhà cung cấp AI. Bean được chọn trong [AiProviderConfiguration] theo
 * `app.ai.provider`; phần còn lại của hệ thống không phụ thuộc provider cụ thể.
 *
 * Triển khai hiện có: Gemini (native REST), DeepSeek/OpenRouter/OpenAI (chuẩn
 * `/chat/completions`) và provider giả lập cho dev/test.
 *
 * CF-NFR-01: API key chỉ nằm ở backend, không xuất hiện trong prompt, log hay DTO.
 */
interface AiProviderClient {
    val providerCode: String
    val modelCode: String

    fun complete(prompt: AiPrompt): AiCompletion
}
