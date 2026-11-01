package com.eventapp.catalog.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

@Entity
@Table(name = "event_categories")
class EventCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    var categoryId: Long? = null,

    @Column(name = "parent_category_id")
    var parentCategoryId: Long? = null,

    @Column(name = "category_code", nullable = false, unique = true)
    var categoryCode: String = "",

    @Column(name = "category_name", nullable = false)
    var categoryName: String = "",

    @Column(name = "slug", nullable = false, unique = true)
    var slug: String = "",

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null
)

@Entity
@Table(name = "event_tags")
class EventTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    var tagId: Long? = null,

    @Column(name = "tag_code", nullable = false, unique = true)
    var tagCode: String = "",

    @Column(name = "tag_name", nullable = false)
    var tagName: String = "",

    @Column(name = "slug", nullable = false, unique = true)
    var slug: String = "",

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_by_user_id")
    var createdByUserId: Long? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null
)

@Embeddable
class EventCategoryMapId(
    @Column(name = "event_id") var eventId: Long = 0L,
    @Column(name = "category_id") var categoryId: Long = 0L
) : Serializable

@Entity
@Table(name = "event_category_map")
class EventCategoryMap(
    @EmbeddedId
    var id: EventCategoryMapId = EventCategoryMapId(),

    @Column(name = "assigned_at", insertable = false, updatable = false)
    var assignedAt: Instant? = null
)

@Embeddable
class EventTagMapId(
    @Column(name = "event_id") var eventId: Long = 0L,
    @Column(name = "tag_id") var tagId: Long = 0L
) : Serializable

@Entity
@Table(name = "event_tag_map")
class EventTagMap(
    @EmbeddedId
    var id: EventTagMapId = EventTagMapId(),

    @Column(name = "assigned_by_user_id", nullable = false)
    var assignedByUserId: Long = 0L,

    @Column(name = "assigned_at", insertable = false, updatable = false)
    var assignedAt: Instant? = null
)
