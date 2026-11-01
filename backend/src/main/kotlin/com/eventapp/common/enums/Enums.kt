package com.eventapp.common.enums

// ============================================================
// Giá trị enum phải khớp CHÍNH XÁC với cột ENUM trong MySQL.
// Server từ chối giá trị lạ do client gửi lên (SRS 1.4).
// ============================================================

// ---------- Người dùng / phân quyền ----------

/** users.status */
enum class UserStatus { PENDING_VERIFICATION, ACTIVE, SUSPENDED, DELETED }

/** roles.role_code */
enum class RoleName { USER, ORGANIZER, ADMIN }

/** organizer_profiles.status */
enum class OrganizerStatus { PENDING, APPROVED, REJECTED, MORE_INFO, SUSPENDED }

// ---------- Sự kiện ----------

/** events.event_format */
enum class EventFormat { OFFLINE, ONLINE, HYBRID }

/** events.visibility */
enum class EventVisibility { PUBLIC, UNLISTED, PRIVATE }

/** event_media.media_type */
enum class MediaType { IMAGE, VIDEO, DOCUMENT }

// ---------- Vé / đăng ký / check-in ----------

/** ticket_types.ticket_kind */
enum class TicketKind { FREE, PAID }

/** ticket_types.status */
enum class TicketTypeStatus { ACTIVE, PAUSED, ENDED, ARCHIVED }

/** tickets.status */
enum class TicketStatus { VALID, CHECKED_IN, CANCELLED, REFUNDED, REVOKED }

/** registrations.status */
enum class RegistrationStatus { CONFIRMED, CANCELLED, WAITLISTED }

/** check_ins.check_in_method */
enum class CheckinMethod { SELF_SCAN, ORGANIZER_SCAN, MANUAL }

/** payments.payment_method */
enum class PaymentMethod { BANK_TRANSFER_QR, WALLET, CARD, OTHER }

/** payments.verification_source */
enum class PaymentVerificationSource { PROVIDER_WEBHOOK, PROVIDER_API, MANUAL_RECONCILIATION, OTHER }

// ---------- Đánh giá ----------

/** reviews.status (SRS gọi HELD, DB dùng PENDING) */
enum class ReviewStatus { PENDING, VISIBLE, HIDDEN, REMOVED }

// ---------- AI ----------

/** ai_requests.feature_code */
enum class AiFeatureCode { EVENT_RECOMMENDATION, EVENT_DESCRIPTION, CHATBOT, ORGANIZER_ANALYTICS, OTHER }

/** ai_requests.status */
enum class AiRequestStatus { PENDING, SUCCEEDED, FAILED, CANCELLED }

/** ai_conversations.status */
enum class AiConversationStatus { ACTIVE, ARCHIVED, DELETED }

/** ai_messages.sender_type */
enum class AiSenderType { USER, ASSISTANT, SYSTEM }

// ---------- Tích hợp ----------

/** integration_health_logs.service_code */
enum class IntegrationServiceCode { PAYMENT, FCM, AI, EMAIL, STORAGE, OTHER }

/** integration_health_logs.result_status */
enum class IntegrationResultStatus { SUCCESS, WARNING, ERROR }

// ---------- Khác (giữ lại cho các module khác dùng) ----------

enum class AccountStatus { PENDING_VERIFICATION, ACTIVE, SUSPENDED, DELETED }
enum class SaleStatus { NOT_STARTED, ON_SALE, ENDED, SOLD_OUT }
enum class NotificationType { SYSTEM, EVENT, ORDER, TICKET, PAYMENT, SUPPORT }
enum class RefundStatus { REQUESTED, APPROVED, PROCESSING, SUCCEEDED, FAILED, REJECTED }
enum class SupportStatus { OPEN, IN_PROGRESS, WAITING_USER, RESOLVED, CLOSED }
