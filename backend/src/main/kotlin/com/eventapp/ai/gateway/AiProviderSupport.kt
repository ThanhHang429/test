package com.eventapp.ai.gateway

import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.io.IOException
import kotlin.random.Random

/**
 * Lỗi khi gọi nhà cung cấp AI.
 *
 * Chỉ giữ mã HTTP và mô tả ngắn — không chứa URL đầy đủ, header hay body để API key
 * không thể lọt vào log (CF-NFR-01).
 */
class AiProviderException(val statusCode: Int?, message: String) : RuntimeException(message) {
    /** SRS 5.1: chỉ retry khi timeout/lỗi mạng/429/5xx; 4xx khác là lỗi cấu hình. */
    val retryable: Boolean
        get() = statusCode == null || statusCode == 429 || statusCode >= 500
}

/** Tiện ích dùng chung cho các provider HTTP. */
internal object AiHttp {

    /** Kết nối 3 giây theo SRS 5.1; timeout đọc rộng hơn timeout cứng của Gateway. */
    fun restClient(connectTimeoutSeconds: Int, readTimeoutSeconds: Int): RestClient {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(connectTimeoutSeconds.coerceAtLeast(1) * 1000)
            setReadTimeout(readTimeoutSeconds.coerceAtLeast(1) * 1000)
        }
        return RestClient.builder().requestFactory(factory).build()
    }

    /** Retry tối đa 1 lần, giãn cách 500 ms cộng ngẫu nhiên (SRS 5.1). */
    fun <T> withRetry(block: () -> T): T = try {
        block()
    } catch (ex: AiProviderException) {
        if (!ex.retryable) throw ex
        backoff()
        block()
    } catch (ex: ResourceAccessException) {
        backoff()
        block()
    } catch (ex: IOException) {
        backoff()
        block()
    }

    private fun backoff() = Thread.sleep(500L + Random.nextLong(0, 250))
}
