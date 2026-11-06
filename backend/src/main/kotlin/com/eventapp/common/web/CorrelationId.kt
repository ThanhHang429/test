package com.eventapp.common.web

import org.slf4j.MDC
import java.util.UUID

/**
 * Giữ correlation ID của request hiện tại (SRS mục 3.6).
 * Android sinh `X-Correlation-Id`; backend giữ nguyên hoặc tự sinh nếu thiếu,
 * ghi vào log, vào `ai_requests` và `audit_logs`, và trả lại trong response.
 */
object CorrelationId {
    const val HEADER = "X-Correlation-Id"
    private const val MDC_KEY = "correlationId"
    private val holder = ThreadLocal<String>()

    fun current(): String? = holder.get()

    fun getOrCreate(): String = holder.get() ?: UUID.randomUUID().toString().also { set(it) }

    fun set(value: String) {
        holder.set(value)
        MDC.put(MDC_KEY, value)
    }

    fun clear() {
        holder.remove()
        MDC.remove(MDC_KEY)
    }
}
