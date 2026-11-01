package com.eventapp.ai.gateway

import com.eventapp.ai.entity.AiRequest
import com.eventapp.ai.entity.IntegrationHealthLog
import com.eventapp.ai.repository.AiRequestRepository
import com.eventapp.ai.repository.IntegrationHealthLogRepository
import com.eventapp.common.enums.AiRequestStatus
import com.eventapp.common.enums.IntegrationResultStatus
import com.eventapp.common.enums.IntegrationServiceCode
import com.eventapp.common.web.CorrelationId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Ghi `ai_requests` cho **mọi** lời gọi AI, kể cả khi lỗi (U07-R10, A20-R02).
 *
 * Không lưu prompt đầy đủ — chỉ tóm tắt đã khử dữ liệu nhận dạng (CF-NFR-01, A20-R01).
 * Đồng thời ghi `integration_health_logs` để màn hình giám sát A20 có dữ liệu.
 */
@Component
class AiRequestLogger(
    private val aiRequestRepository: AiRequestRepository,
    private val integrationHealthLogRepository: IntegrationHealthLogRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun success(
        task: AiTask,
        userId: Long?,
        eventId: Long?,
        providerCode: String,
        modelCode: String,
        inputSummary: String?,
        outputSummary: String?,
        inputTokens: Int?,
        outputTokens: Int?,
        latencyMs: Long
    ): AiRequest = persist(
        task, userId, eventId, providerCode, modelCode, inputSummary, outputSummary,
        inputTokens, outputTokens, latencyMs, AiRequestStatus.SUCCEEDED, null
    )

    fun failure(
        task: AiTask,
        userId: Long?,
        eventId: Long?,
        providerCode: String?,
        modelCode: String?,
        inputSummary: String?,
        latencyMs: Long,
        errorCode: String
    ): AiRequest = persist(
        task, userId, eventId, providerCode, modelCode, inputSummary, null,
        null, null, latencyMs, AiRequestStatus.FAILED, errorCode
    )

    private fun persist(
        task: AiTask,
        userId: Long?,
        eventId: Long?,
        providerCode: String?,
        modelCode: String?,
        inputSummary: String?,
        outputSummary: String?,
        inputTokens: Int?,
        outputTokens: Int?,
        latencyMs: Long,
        status: AiRequestStatus,
        errorCode: String?
    ): AiRequest {
        val correlationId = CorrelationId.current()
        val saved = try {
            aiRequestRepository.save(
                AiRequest(
                    userId = userId,
                    eventId = eventId,
                    featureCode = task.featureCode,
                    providerCode = providerCode,
                    modelCode = modelCode,
                    status = status,
                    inputSummary = inputSummary?.take(1000),
                    outputSummary = outputSummary?.take(1000),
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    latencyMs = latencyMs.toInt(),
                    errorCode = errorCode,
                    correlationId = correlationId,
                    completedAt = Instant.now()
                )
            )
        } catch (ex: Exception) {
            log.warn("Không ghi được ai_requests cho {}", task.code, ex)
            AiRequest()
        }

        try {
            integrationHealthLogRepository.save(
                IntegrationHealthLog(
                    serviceCode = IntegrationServiceCode.AI,
                    operationCode = task.code,
                    resultStatus = if (status == AiRequestStatus.SUCCEEDED) {
                        IntegrationResultStatus.SUCCESS
                    } else {
                        IntegrationResultStatus.ERROR
                    },
                    errorCode = errorCode,
                    message = if (status == AiRequestStatus.SUCCEEDED) "OK" else errorCode,
                    latencyMs = latencyMs.toInt(),
                    correlationId = correlationId,
                    metadataRedacted = mapOf(
                        "model" to modelCode,
                        "inputTokens" to inputTokens,
                        "outputTokens" to outputTokens
                    ),
                    actorUserId = userId
                )
            )
        } catch (ex: Exception) {
            log.warn("Không ghi được integration_health_logs", ex)
        }
        return saved
    }
}
