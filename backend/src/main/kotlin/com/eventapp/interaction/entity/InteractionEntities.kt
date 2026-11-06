package com.eventapp.interaction.entity

import com.eventapp.common.enums.ReviewStatus
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.time.Instant

@Embeddable
class FavoriteId(
    @Column(name = "user_id") var userId: Long = 0L,
    @Column(name = "event_id") var eventId: Long = 0L
) : Serializable

@Entity
@Table(name = "favorites")
class Favorite(
    @EmbeddedId
    var id: FavoriteId = FavoriteId(),

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null
)

@Entity
@Table(name = "reviews")
class Review(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    var reviewId: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0L,

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0L,

    /** tinyint unsigned, 1..5 */
    @Column(name = "rating", nullable = false)
    var rating: Byte = 5,

    @Column(name = "review_text", columnDefinition = "TEXT")
    var reviewText: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ReviewStatus = ReviewStatus.VISIBLE,

    @Column(name = "moderation_reason")
    var moderationReason: String? = null,

    @Column(name = "moderated_by_user_id")
    var moderatedByUserId: Long? = null,

    @Column(name = "moderated_at")
    var moderatedAt: Instant? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null
)

/** Giá trị của settings_json trong user_preferences. */
data class PreferenceSettings(
    val priceMode: String = "ANY",
    val maxBudgetVnd: Long? = null,
    val preferredDays: List<String> = listOf("WEEKDAY", "WEEKEND"),
    val preferredCities: List<String> = emptyList(),
    val onboardingCompleted: Boolean = false,
    val version: Int = 0
)

/**
 * Bảng thật không chuẩn hoá như SRS: sở thích danh mục/hình thức nằm trong cột JSON,
 * chỉ có một thành phố. Các trường còn lại (priceMode, ngân sách, ngày, version)
 * lưu trong settings_json.
 */
@Entity
@Table(name = "user_preferences")
class UserPreference(
    @Id
    @Column(name = "user_id")
    var userId: Long = 0L,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_categories_json")
    var preferredCategories: List<Long>? = null,

    @Column(name = "preferred_city")
    var preferredCity: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_event_formats_json")
    var preferredEventFormats: List<String>? = null,

    @Column(name = "personalization_enabled", nullable = false)
    var personalizationEnabled: Boolean = true,

    /** enum('PRIVATE','PUBLIC') */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "profile_visibility", nullable = false)
    var profileVisibility: String = "PRIVATE",

    @Column(name = "allow_product_emails", nullable = false)
    var allowProductEmails: Boolean = true,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings_json")
    var settings: PreferenceSettings? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null
)
