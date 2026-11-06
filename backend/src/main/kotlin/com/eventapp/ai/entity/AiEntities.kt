package com.eventapp.ai.entity

import com.eventapp.common.enums.AiConversationStatus
import com.eventapp.common.enums.AiFeatureCode
import com.eventapp.common.enums.AiRequestStatus
import com.eventapp.common.enums.AiSenderType
import com.eventapp.common.enums.IntegrationResultStatus
import com.eventapp.common.enums.IntegrationServiceCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "ai_requests")
class AiRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_request_id")
    var aiRequestId: Long? = null,

    @Column(name = "user_id")
    var userId: Long? = null,

    @Column(name = "event_id")
    var eventId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_code", nullable = false)
    var featureCode: AiFeatureCode = AiFeatureCode.OTHER,

    @Column(name = "provider_code")
    var providerCode: String? = null,

    @Column(name = "model_code")
    var modelCode: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: AiRequestStatus = AiRequestStatus.PENDING,

    /** Chỉ lưu tóm tắt đã khử dữ liệu nhận dạng, KHÔNG lưu prompt đầy đủ. */
    @Column(name = "input_summary", columnDefinition = "TEXT")
    var inputSummary: String? = null,

    @Column(name = "output_summary", columnDefinition = "TEXT")
    var outputSummary: String? = null,

    @Column(name = "input_tokens")
    var inputTokens: Int? = null,

    @Column(name = "output_tokens")
    var outputTokens: Int? = null,

    @Column(name = "latency_ms")
    var latencyMs: Int? = null,

    @Column(name = "error_code")
    var errorCode: String? = null,

    @Column(name = "correlation_id")
    var correlationId: String? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "completed_at")
    var completedAt: Instant? = null
)

@Entity
@Table(name = "ai_conversations")
class AiConversation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_conversation_id")
    var aiConversationId: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0L,

    @Column(name = "title")
    var title: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: AiConversationStatus = AiConversationStatus.ACTIVE,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null
)

@Entity
@Table(name = "ai_messages")
class AiMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_message_id")
    var aiMessageId: Long? = null,

    @Column(name = "ai_conversation_id", nullable = false)
    var aiConversationId: Long = 0L,

    @Column(name = "ai_request_id")
    var aiRequestId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    var senderType: AiSenderType = AiSenderType.USER,

    @Column(name = "message_text", columnDefinition = "MEDIUMTEXT", nullable = false)
    var messageText: String = "",

    @Column(name = "related_event_id")
    var relatedEventId: Long? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null
)

/** Dùng làm cache gợi ý + lưu thao tác "Không quan tâm" (SRS U07). */
@Entity
@Table(name = "event_recommendations")
class EventRecommendation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_recommendation_id")
    var eventRecommendationId: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0L,

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0L,

    @Column(name = "ai_request_id")
    var aiRequestId: Long? = null,

    @Column(name = "recommendation_reason")
    var recommendationReason: String? = null,

    @Column(name = "recommendation_score")
    var recommendationScore: BigDecimal? = null,

    @Column(name = "position_no")
    var positionNo: Short? = null,

    @Column(name = "shown_at", insertable = false, updatable = false)
    var shownAt: Instant? = null,

    @Column(name = "clicked_at")
    var clickedAt: Instant? = null,

    @Column(name = "dismissed_at")
    var dismissedAt: Instant? = null
)

@Entity
@Table(name = "integration_health_logs")
class IntegrationHealthLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_health_log_id")
    var integrationHealthLogId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "service_code", nullable = false)
    var serviceCode: IntegrationServiceCode = IntegrationServiceCode.AI,

    @Column(name = "operation_code", nullable = false)
    var operationCode: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false)
    var resultStatus: IntegrationResultStatus = IntegrationResultStatus.SUCCESS,

    @Column(name = "error_code")
    var errorCode: String? = null,

    @Column(name = "message")
    var message: String? = null,

    @Column(name = "latency_ms")
    var latencyMs: Int? = null,

    @Column(name = "correlation_id")
    var correlationId: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_redacted")
    var metadataRedacted: Map<String, Any?>? = null,

    @Column(name = "actor_user_id")
    var actorUserId: Long? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null
)

@Entity
@Table(name = "platform_settings")
class PlatformSetting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "platform_setting_id")
    var platformSettingId: Long? = null,

    @Column(name = "setting_key", nullable = false)
    var settingKey: String = "",

    @Column(name = "version_no", nullable = false)
    var versionNo: Int = 1,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "setting_value", nullable = false)
    var settingValue: Map<String, Any?> = emptyMap(),

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "effective_from", insertable = false, updatable = false)
    var effectiveFrom: Instant? = null,

    @Column(name = "changed_by_user_id")
    var changedByUserId: Long? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null
)

@Entity
@Table(name = "audit_logs")
class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    var auditLogId: Long? = null,

    @Column(name = "actor_user_id")
    var actorUserId: Long? = null,

    @Column(name = "action_code", nullable = false)
    var actionCode: String = "",

    @Column(name = "entity_type", nullable = false)
    var entityType: String = "",

    @Column(name = "entity_id")
    var entityId: String? = null,

    @Column(name = "reason", columnDefinition = "TEXT")
    var reason: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values_json")
    var oldValues: Map<String, Any?>? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values_json")
    var newValues: Map<String, Any?>? = null,

    @Column(name = "result_code")
    var resultCode: String? = null,

    @Column(name = "ip_address")
    var ipAddress: String? = null,

    @Column(name = "correlation_id")
    var correlationId: String? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null
)
