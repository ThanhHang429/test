package com.eventapp.common.idempotency

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Idempotent

interface IdempotencyRepository
data class IdempotencyRecord(val id: Long? = null)
class IdempotencyKeyService
class IdempotencyFilter