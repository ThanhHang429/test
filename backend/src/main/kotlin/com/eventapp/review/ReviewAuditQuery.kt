package com.eventapp.review

import com.eventapp.ai.repository.AuditLogRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Truy vấn vết kiểm duyệt đánh giá trong `audit_logs`.
 *
 * Schema thật không có `review_reports` / `moderation_flags` / cột `edit_count`,
 * nên các thông tin đó được suy ra từ audit:
 *  - số lần sửa   = số dòng action REVIEW_UPDATED
 *  - số báo cáo   = số tài khoản khác nhau có action REVIEW_REPORTED trên cùng review
 */
@Component
class ReviewAuditQuery(
    private val auditLogRepository: AuditLogRepository
) {

    fun editCount(reviewId: Long): Int =
        auditLogRepository
            .findByActionCodeAndEntityTypeAndEntityId(
                ReviewService.ACTION_UPDATED,
                ReviewService.ENTITY_TYPE,
                reviewId.toString()
            )
            .size

    fun hasReported(actorUserId: Long, reviewId: String): Boolean =
        auditLogRepository.existsByActionCodeAndEntityTypeAndEntityIdAndActorUserId(
            ReviewService.ACTION_REPORTED,
            ReviewService.ENTITY_TYPE,
            reviewId,
            actorUserId
        )

    fun reportsToday(actorUserId: Long, now: Instant): Long =
        auditLogRepository.countByActorUserIdAndActionCodeAndCreatedAtAfter(
            actorUserId,
            ReviewService.ACTION_REPORTED,
            now.minusSeconds(86_400)
        )

    /** Số tài khoản khác nhau đã báo cáo một đánh giá. */
    fun reporters(reviewId: String): Long {
        val rows = auditLogRepository.countDistinctActorsByEntity(
            ReviewService.ACTION_REPORTED,
            ReviewService.ENTITY_TYPE,
            listOf(reviewId)
        )
        val row = rows.firstOrNull() ?: return 0L
        return (row[1] as? Number)?.toLong() ?: 0L
    }

    /** Dùng cho hàng chờ A14: đếm báo cáo cho nhiều đánh giá trong một truy vấn. */
    fun reportCounts(reviewIds: Collection<Long>): Map<Long, Long> {
        if (reviewIds.isEmpty()) return emptyMap()
        val rows = auditLogRepository.countDistinctActorsByEntity(
            ReviewService.ACTION_REPORTED,
            ReviewService.ENTITY_TYPE,
            reviewIds.map { it.toString() }
        )
        return rows.mapNotNull { row ->
            val id = row[0]?.toString()?.toLongOrNull() ?: return@mapNotNull null
            id to ((row[1] as? Number)?.toLong() ?: 0L)
        }.toMap()
    }

    /** Lịch sử xử lý của một đánh giá (dùng cho màn chi tiết của Admin). */
    fun history(reviewId: Long): List<Map<String, Any?>> =
        auditLogRepository
            .findByActionCodeAndEntityTypeAndEntityId(ReviewService.ACTION_UPDATED, ReviewService.ENTITY_TYPE, reviewId.toString())
            .map {
                mapOf(
                    "action" to it.actionCode,
                    "actorUserId" to it.actorUserId,
                    "at" to it.createdAt,
                    "reason" to it.reason
                )
            }
}
