package com.eventapp.ai.gateway

import com.eventapp.ai.repository.PlatformSettingRepository
import com.eventapp.common.audit.AuditLogWriter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/**
 * Công tắc khẩn theo từng tác vụ AI (FR-A20 bước 4, A20-R04: có hiệu lực trong 5 giây).
 *
 * Lưu ở `platform_settings` khoá `ai.feature.flags` dạng `{ "RECOMMEND": true, ... }`
 * (schema thật không có bảng `ai_feature_flags`). Cache 5 giây để mọi instance thấy thay đổi
 * nhanh mà không truy vấn DB trên mỗi lần gọi AI.
 */
@Service
class AiFeatureFlags(
    private val platformSettingRepository: PlatformSettingRepository,
    private val auditLogWriter: AuditLogWriter
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private data class Snapshot(val flags: Map<String, Boolean>, val versionNo: Int, val loadedAt: Instant)

    @Volatile
    private var snapshot: Snapshot? = null

    fun isEnabled(task: AiTask): Boolean = flags()[task.code] ?: true

    fun flags(): Map<String, Boolean> {
        val current = snapshot
        if (current != null && Duration.between(current.loadedAt, Instant.now()) < CACHE_TTL) {
            return current.flags
        }
        val loaded = try {
            val row = platformSettingRepository.findFirstBySettingKeyOrderByVersionNoDesc(KEY)
            val flags = AiTask.entries.associate { task ->
                task.code to (row?.settingValue?.get(task.code)?.toString()?.toBooleanStrictOrNull() ?: true)
            }
            Snapshot(flags, row?.versionNo ?: 0, Instant.now())
        } catch (ex: Exception) {
            log.warn("Không đọc được {} — coi như bật tất cả", KEY, ex)
            Snapshot(AiTask.entries.associate { it.code to true }, 0, Instant.now())
        }
        snapshot = loaded
        return loaded.flags
    }

    /** Bật/tắt một tác vụ; ghi phiên bản mới để giữ lịch sử (A20-R04 phải ghi audit). */
    @Transactional
    fun setEnabled(task: AiTask, enabled: Boolean, actorUserId: Long?, reason: String): Map<String, Boolean> {
        val existing = platformSettingRepository.findFirstBySettingKeyOrderByVersionNoDesc(KEY)
        val currentFlags = existing?.settingValue.orEmpty()

        // Chỉ giữ khoá theo AiTask.code; đọc được cả khoá cũ (theo feature_code) để nâng cấp êm.
        val nextFlags = LinkedHashMap<String, Any?>()
        AiTask.entries.forEach { task ->
            val value = currentFlags[task.code]?.toString()?.toBooleanStrictOrNull()
                ?: currentFlags[task.featureCode.name]?.toString()?.toBooleanStrictOrNull()
                ?: true
            nextFlags[task.code] = value
        }
        nextFlags[task.code] = enabled

        platformSettingRepository.save(
            com.eventapp.ai.entity.PlatformSetting(
                settingKey = KEY,
                versionNo = (existing?.versionNo ?: 0) + 1,
                settingValue = nextFlags,
                description = "Bật/tắt từng tác vụ AI (kill switch)",
                changedByUserId = actorUserId
            )
        )
        snapshot = null // hiệu lực ngay lần đọc kế tiếp

        auditLogWriter.write(
            actorUserId = actorUserId,
            actionCode = if (enabled) "AI_FEATURE_ENABLED" else "AI_FEATURE_DISABLED",
            entityType = "ai_feature_flag",
            entityId = task.code,
            reason = reason,
            oldValues = mapOf("enabled" to (currentFlags[task.code] ?: true)),
            newValues = mapOf("enabled" to enabled),
            resultCode = if (enabled) "ENABLED" else "DISABLED"
        )
        log.info("Admin {} {} tác vụ AI {} (lý do: {})", actorUserId, if (enabled) "bật" else "tắt", task.code, reason)
        return nextFlags.mapValues { (it.value as? Boolean) ?: true }
    }

    fun versionNo(): Int = snapshot?.versionNo ?: 0

    companion object {
        const val KEY = "ai.feature.flags"
        private val CACHE_TTL: Duration = Duration.ofSeconds(5)
    }
}
