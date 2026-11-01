package com.eventapp.ai.gateway

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Cổng AI duy nhất của hệ thống (SRS 5.1). Mọi tác vụ AI đều đi qua đúng 6 bước:
 *
 *  1. Kiểm tra công tắc khẩn theo taskType (A20-R04).
 *  2. Kiểm tra quota người dùng (U07-R11, O06, O27).
 *  3. Kiểm tra circuit breaker.
 *  4. Dựng prompt có phiên bản, đã khử dữ liệu nhận dạng.
 *  5. Gọi nhà cung cấp với timeout cứng, hậu kiểm schema, thử lại 1 lần.
 *  6. Ghi `ai_requests` (kể cả khi lỗi) + `integration_health_logs`.
 *
 * Nơi gọi **luôn** phải có fallback: AI lỗi không được làm hỏng chức năng chính (NFR-03).
 */
@Service
class AiGateway(
    private val featureFlags: AiFeatureFlags,
    private val quotaService: AiQuotaService,
    private val circuitBreaker: AiCircuitBreaker,
    private val promptBuilder: PromptBuilder,
    private val outputValidator: OutputValidator,
    private val requestLogger: AiRequestLogger,
    private val provider: AiProviderClient,
    @Value("\${app.ai.timeout-seconds:5}") private val timeoutSeconds: Long
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "ai-gateway").apply { isDaemon = true }
    }

    fun execute(
        task: AiTask,
        userId: Long?,
        eventId: Long?,
        fields: Map<String, Any?>,
        inputSummary: String?
    ): AiGatewayResult {
        if (!featureFlags.isEnabled(task)) {
            throw AiDisabledException("Tính năng AI '${task.code}' đang tạm tắt")
        }
        quotaService.ensureWithinQuota(task, userId)

        if (circuitBreaker.isOpen()) {
            requestLogger.failure(task, userId, eventId, provider.providerCode, provider.modelCode, inputSummary, 0, "AI_CIRCUIT_OPEN")
            throw AiUnavailableException("Dịch vụ AI đang tạm ngưng do lỗi liên tiếp")
        }

        val prompt = promptBuilder.build(task, fields)
        val started = System.currentTimeMillis()

        val completion = try {
            callWithTimeout(prompt)
        } catch (ex: Exception) {
            val latency = System.currentTimeMillis() - started
            circuitBreaker.recordFailure()
            requestLogger.failure(
                task, userId, eventId, provider.providerCode, provider.modelCode, inputSummary, latency,
                if (ex is java.util.concurrent.TimeoutException) "AI_TIMEOUT" else "AI_UNAVAILABLE"
            )
            log.warn("Gọi AI {} thất bại sau {} ms: {}", task.code, latency, ex.message)
            throw AiUnavailableException("Không gọi được dịch vụ AI: ${ex.javaClass.simpleName}")
        }

        val validated = try {
            validateWithRetry(task, prompt)
        } catch (ex: AiInvalidOutputException) {
            val latency = System.currentTimeMillis() - started
            circuitBreaker.recordSuccess() // nhà cung cấp vẫn sống, chỉ sai định dạng
            requestLogger.failure(
                task, userId, eventId, provider.providerCode, provider.modelCode, inputSummary, latency,
                "AI_INVALID_OUTPUT"
            )
            log.warn("Đầu ra AI {} không hợp lệ: {}", task.code, ex.message)
            throw ex
        }

        val latency = System.currentTimeMillis() - started
        circuitBreaker.recordSuccess()
        val saved = requestLogger.success(
            task = task,
            userId = userId,
            eventId = eventId,
            providerCode = provider.providerCode,
            modelCode = provider.modelCode,
            inputSummary = inputSummary,
            outputSummary = summarize(validated),
            inputTokens = completion.inputTokens,
            outputTokens = completion.outputTokens,
            latencyMs = latency
        )

        return AiGatewayResult(
            aiRequestId = saved.aiRequestId,
            task = task,
            data = validated,
            latencyMs = latency,
            correlationId = saved.correlationId,
            modelCode = provider.modelCode
        )
    }

    /** true nếu tác vụ đang bật và breaker đóng — dùng để quyết định gọi AI hay đi thẳng luật. */
    fun isAvailable(task: AiTask): Boolean = featureFlags.isEnabled(task) && !circuitBreaker.isOpen()

    private fun callWithTimeout(prompt: AiPrompt): AiCompletion =
        CompletableFuture
            .supplyAsync({ provider.complete(prompt) }, executor)
            .get(timeoutSeconds, TimeUnit.SECONDS)

    private fun validateWithRetry(task: AiTask, prompt: AiPrompt): JsonNode {
        val first = callWithTimeout(prompt)
        return try {
            outputValidator.validate(task, first.text)
        } catch (ex: AiInvalidOutputException) {
            log.info("Đầu ra AI {} sai schema, thử lại 1 lần", task.code)
            val second = callWithTimeout(prompt)
            outputValidator.validate(task, second.text)
        }
    }

    private fun summarize(node: JsonNode): String = node.toString().take(1000)
}
