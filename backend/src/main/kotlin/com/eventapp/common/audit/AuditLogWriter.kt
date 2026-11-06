package com.eventapp.common.audit

import com.eventapp.ai.entity.AuditLog
import com.eventapp.ai.repository.AuditLogRepository
import com.eventapp.common.web.CorrelationId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Ghi vết thao tác quản trị (A07-R08, A14-R07, A09).
 * Bảng `audit_logs` là nơi duy nhất trong schema thật lưu được vết kiểu này
 * (không có `moderation_flags` / `review_reports`).
 */
@Component
class AuditLogWriter(
    private val auditLogRepository: AuditLogRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun write(
        actorUserId: Long?,
        actionCode: String,
        entityType: String,
        entityId: Any?,
        reason: String? = null,
        oldValues: Map<String, Any?>? = null,
        newValues: Map<String, Any?>? = null,
        resultCode: String? = null,
        ipAddress: String? = null
    ) {
        try {
            auditLogRepository.save(
                AuditLog(
                    actorUserId = actorUserId,
                    actionCode = actionCode.take(120),
                    entityType = entityType.take(80),
                    entityId = entityId?.toString()?.take(100),
                    reason = reason,
                    oldValues = oldValues,
                    newValues = newValues,
                    resultCode = resultCode?.take(60),
                    ipAddress = ipAddress?.take(45),
                    correlationId = CorrelationId.current()
                )
            )
        } catch (ex: Exception) {
            // Ghi vết không được phép làm hỏng nghiệp vụ chính.
            log.warn("Không ghi được audit_logs action={} entity={}#{}", actionCode, entityType, entityId, ex)
        }
    }
}
