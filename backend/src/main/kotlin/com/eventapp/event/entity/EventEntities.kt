package com.eventapp.event.entity

import com.eventapp.common.enums.EventFormat
import com.eventapp.common.enums.EventLifecycleStatus
import com.eventapp.common.enums.EventReviewStatus
import com.eventapp.common.enums.EventVisibility
import com.eventapp.common.enums.MediaType
import com.eventapp.common.enums.TicketKind
import com.eventapp.common.enums.TicketTypeStatus
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
@Table(name = "events")
class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    var eventId: Long? = null,

    @Column(name = "event_code", nullable = false, unique = true)
    var eventCode: String = "",

    @Column(name = "organizer_profile_id", nullable = false)
    var organizerProfileId: Long = 0L,

    @Column(name = "created_by_user_id", nullable = false)
    var createdByUserId: Long = 0L,

    @Column(name = "title", nullable = false)
    var title: String = "",

    @Column(name = "slug", nullable = false, unique = true)
    var slug: String = "",

    @Column(name = "short_description")
    var shortDescription: String? = null,

    @Column(name = "description", columnDefinition = "MEDIUMTEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_format", nullable = false)
    var eventFormat: EventFormat = EventFormat.OFFLINE,

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    var reviewStatus: EventReviewStatus = EventReviewStatus.DRAFT,

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false)
    var lifecycleStatus: EventLifecycleStatus = EventLifecycleStatus.UNPUBLISHED,

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    var visibility: EventVisibility = EventVisibility.PUBLIC,

    @Column(name = "start_at", nullable = false)
    var startAt: Instant = Instant.EPOCH,

    @Column(name = "end_at", nullable = false)
    var endAt: Instant = Instant.EPOCH,

    @Column(name = "timezone", nullable = false)
    var timezone: String = "Asia/Ho_Chi_Minh",

    @Column(name = "registration_start_at")
    var registrationStartAt: Instant? = null,

    @Column(name = "registration_end_at")
    var registrationEndAt: Instant? = null,

    @Column(name = "venue_name")
    var venueName: String? = null,

    @Column(name = "address_line")
    var addressLine: String? = null,

    @Column(name = "ward")
    var ward: String? = null,

    @Column(name = "district")
    var district: String? = null,

    @Column(name = "city")
    var city: String? = null,

    @Column(name = "province")
    var province: String? = null,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", nullable = false)
    var countryCode: String = "VN",

    @Column(name = "postal_code")
    var postalCode: String? = null,

    @Column(name = "latitude")
    var latitude: BigDecimal? = null,

    @Column(name = "longitude")
    var longitude: BigDecimal? = null,

    @Column(name = "online_url")
    var onlineUrl: String? = null,

    @Column(name = "capacity_limit")
    var capacityLimit: Int? = null,

    @Column(name = "participation_conditions", columnDefinition = "TEXT")
    var participationConditions: String? = null,

    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    var cancellationPolicy: String? = null,

    @Column(name = "published_at")
    var publishedAt: Instant? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
)

@Entity
@Table(name = "event_media")
class EventMedia(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_media_id")
    var eventMediaId: Long? = null,

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0L,

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    var mediaType: MediaType = MediaType.IMAGE,

    @Column(name = "storage_url", nullable = false)
    var storageUrl: String = "",

    @Column(name = "original_file_name")
    var originalFileName: String? = null,

    @Column(name = "mime_type")
    var mimeType: String? = null,

    @Column(name = "file_size_bytes")
    var fileSizeBytes: Long? = null,

    @Column(name = "alt_text")
    var altText: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "is_cover", nullable = false)
    var isCover: Boolean = false,

    @Column(name = "uploaded_by_user_id", nullable = false)
    var uploadedByUserId: Long = 0L,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null
)

@Entity
@Table(name = "ticket_types")
class TicketType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_type_id")
    var ticketTypeId: Long? = null,

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0L,

    @Column(name = "ticket_type_name", nullable = false)
    var ticketTypeName: String = "",

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_kind", nullable = false)
    var ticketKind: TicketKind = TicketKind.FREE,

    @Column(name = "price", nullable = false)
    var price: BigDecimal = BigDecimal.ZERO,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false)
    var currency: String = "VND",

    @Column(name = "quantity_total")
    var quantityTotal: Int? = null,

    @Column(name = "quantity_sold", nullable = false)
    var quantitySold: Int = 0,

    @Column(name = "quantity_reserved", nullable = false)
    var quantityReserved: Int = 0,

    @Column(name = "min_per_order", nullable = false)
    var minPerOrder: Short = 1,

    @Column(name = "max_per_user", nullable = false)
    var maxPerUser: Short = 10,

    @Column(name = "sale_start_at")
    var saleStartAt: Instant? = null,

    @Column(name = "sale_end_at")
    var saleEndAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: TicketTypeStatus = TicketTypeStatus.ACTIVE,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null
) {
    /** Số vé còn lại; null = không giới hạn. */
    fun remaining(): Int? = quantityTotal?.let { it - quantitySold - quantityReserved }
}
