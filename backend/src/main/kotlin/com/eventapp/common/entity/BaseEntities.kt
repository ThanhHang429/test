
package com.eventapp.common.entity

import java.time.Instant

// TODO: Base entities
abstract class BaseEntity(
    var createdAt: Instant? = null,
    var updatedAt: Instant? = null
)

abstract class AuditableEntity(
    var createdBy: Long? = null,
    var updatedBy: Long? = null
) : BaseEntity()

abstract class SoftDeletableEntity(
    var deleted: Boolean = false,
    var deletedAt: Instant? = null
) : BaseEntity()