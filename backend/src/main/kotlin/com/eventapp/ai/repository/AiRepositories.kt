package com.eventapp.ai.repository

import com.eventapp.ai.entity.AiConversation
import com.eventapp.ai.entity.AiMessage
import com.eventapp.ai.entity.AiRequest
import com.eventapp.ai.entity.AuditLog
import com.eventapp.ai.entity.EventRecommendation
import com.eventapp.ai.entity.IntegrationHealthLog
import com.eventapp.ai.entity.PlatformSetting
import com.eventapp.common.enums.AiFeatureCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface AiRequestRepository : JpaRepository<AiRequest, Long> {
    fun findByCorrelationIdOrderByCreatedAtDesc(correlationId: String): List<AiRequest>
    fun findByCreatedAtAfterOrderByCreatedAtDesc(since: Instant, pageable: Pageable): Page<AiRequest>
    fun countByUserIdAndFeatureCodeAndCreatedAtAfter(userId: Long, featureCode: AiFeatureCode, since: Instant): Long
    fun findByFeatureCodeAndCreatedAtAfter(featureCode: AiFeatureCode, since: Instant): List<AiRequest>
}

@Repository
interface AiConversationRepository : JpaRepository<AiConversation, Long>

@Repository
interface AiMessageRepository : JpaRepository<AiMessage, Long> {
    fun findByAiConversationIdOrderByCreatedAtAsc(aiConversationId: Long): List<AiMessage>
}

@Repository
interface EventRecommendationRepository : JpaRepository<EventRecommendation, Long> {
    fun findByUserIdAndShownAtAfterOrderByPositionNoAsc(userId: Long, since: Instant): List<EventRecommendation>
    fun findByUserIdAndEventIdAndDismissedAtIsNotNull(userId: Long, eventId: Long): List<EventRecommendation>
    fun findTopByUserIdAndEventIdOrderByShownAtDesc(userId: Long, eventId: Long): EventRecommendation?

    @org.springframework.data.jpa.repository.Query(
        """
        select r.eventId from EventRecommendation r
        where r.userId = :userId and r.dismissedAt is not null and r.dismissedAt > :since
        """
    )
    fun findDismissedEventIds(userId: Long, since: Instant): List<Long>
}

@Repository
interface IntegrationHealthLogRepository : JpaRepository<IntegrationHealthLog, Long> {
    fun findTop200ByOrderByCreatedAtDesc(): List<IntegrationHealthLog>
}

@Repository
interface PlatformSettingRepository : JpaRepository<PlatformSetting, Long> {
    fun findBySettingKeyOrderByVersionNoDesc(settingKey: String): List<PlatformSetting>
    fun findFirstBySettingKeyOrderByVersionNoDesc(settingKey: String): PlatformSetting?
}

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {
    fun findByActionCodeAndEntityTypeAndEntityId(
        actionCode: String,
        entityType: String,
        entityId: String
    ): List<AuditLog>

    fun existsByActionCodeAndEntityTypeAndEntityIdAndActorUserId(
        actionCode: String,
        entityType: String,
        entityId: String,
        actorUserId: Long
    ): Boolean

    fun countByActorUserIdAndActionCodeAndCreatedAtAfter(
        actorUserId: Long,
        actionCode: String,
        since: Instant
    ): Long

    /** Số tài khoản khác nhau đã báo cáo từng đánh giá (A14: ngưỡng 3 báo cáo). */
    @org.springframework.data.jpa.repository.Query(
        """
        select a.entityId, count(distinct a.actorUserId) from AuditLog a
        where a.actionCode = :actionCode and a.entityType = :entityType and a.entityId in :entityIds
        group by a.entityId
        """
    )
    fun countDistinctActorsByEntity(
        actionCode: String,
        entityType: String,
        entityIds: Collection<String>
    ): List<Array<Any>>
}
