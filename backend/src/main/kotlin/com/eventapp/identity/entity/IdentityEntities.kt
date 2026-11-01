package com.eventapp.identity.entity

import com.eventapp.common.enums.OrganizerStatus
import com.eventapp.common.enums.UserStatus
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
import java.io.Serializable
import java.time.Instant

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    var userId: Long? = null,

    @Column(name = "email")
    var email: String? = null,

    @Column(name = "phone")
    var phone: String? = null,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",

    @Column(name = "full_name", nullable = false)
    var fullName: String = "",

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "email_verified_at")
    var emailVerifiedAt: Instant? = null,

    @Column(name = "phone_verified_at")
    var phoneVerifiedAt: Instant? = null,

    @Column(name = "locale", nullable = false)
    var locale: String = "vi-VN",

    @Column(name = "timezone", nullable = false)
    var timezone: String = "Asia/Ho_Chi_Minh",

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
)

@Entity
@Table(name = "roles")
class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    var roleId: Long? = null,

    @Column(name = "role_code", nullable = false, unique = true)
    var roleCode: String = "",

    @Column(name = "role_name", nullable = false)
    var roleName: String = "",

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "is_system", nullable = false)
    var isSystem: Boolean = true,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null
)

@Embeddable
class UserRoleId(
    @Column(name = "user_id") var userId: Long = 0L,
    @Column(name = "role_id") var roleId: Long = 0L
) : Serializable

@Entity
@Table(name = "user_roles")
class UserRole(
    @EmbeddedId
    var id: UserRoleId = UserRoleId(),

    @Column(name = "assigned_by_user_id")
    var assignedByUserId: Long? = null,

    @Column(name = "assigned_at", insertable = false, updatable = false)
    var assignedAt: Instant? = null,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null
)

@Entity
@Table(name = "organizer_profiles")
class OrganizerProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organizer_profile_id")
    var organizerProfileId: Long? = null,

    @Column(name = "user_id", nullable = false, unique = true)
    var userId: Long = 0L,

    @Column(name = "organization_name", nullable = false)
    var organizationName: String = "",

    @Column(name = "public_name")
    var publicName: String? = null,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "contact_email")
    var contactEmail: String? = null,

    @Column(name = "contact_phone")
    var contactPhone: String? = null,

    @Column(name = "website_url")
    var websiteUrl: String? = null,

    @Column(name = "legal_identifier")
    var legalIdentifier: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrganizerStatus = OrganizerStatus.PENDING,

    @Column(name = "submitted_at")
    var submittedAt: Instant? = null,

    @Column(name = "reviewed_by_user_id")
    var reviewedByUserId: Long? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

    @Column(name = "review_note")
    var reviewNote: String? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null
)
