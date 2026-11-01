package com.eventapp.ai.gateway

/**
 * Cổng tới nhà cung cấp AI. Triển khai thật (Gemini/OpenAI) chỉ cần thêm một bean và
 * đổi `app.ai.provider`, phần còn lại của hệ thống không phải sửa.
 *
 * CF-NFR-01: API key chỉ nằm ở backend, không xuất hiện trong prompt, log hay DTO.
 */
interface AiProviderClient {
    val providerCode: String
    val modelCode: String

    fun complete(prompt: AiPrompt): AiCompletion
}
