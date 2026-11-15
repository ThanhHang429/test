package com.eventapp.ticketing.payment

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Service
open class PaymentSseHub(
    private val objectMapper: ObjectMapper
) {
    private val channels = ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>()

    open fun subscribePayment(paymentId: Long, initialSnapshot: Any? = null): SseEmitter {
        return subscribe("payment:$paymentId", initialSnapshot)
    }

    open fun subscribeTopup(topupId: Long, initialSnapshot: Any? = null): SseEmitter {
        return subscribe("topup:$topupId", initialSnapshot)
    }

    open fun publishPayment(paymentId: Long, payload: Any) {
        publish("payment:$paymentId", "payment_updated", payload)
    }

    open fun publishTopup(topupId: Long, payload: Any) {
        publish("topup:$topupId", "topup_updated", payload)
    }

    private fun subscribe(key: String, initialSnapshot: Any?): SseEmitter {
        val emitter = SseEmitter(16 * 60 * 1000L) // 16 phút > thời gian giữ chỗ 15 phút
        val list = channels.computeIfAbsent(key) { CopyOnWriteArrayList() }
        list.add(emitter)

        val cleanup = {
            list.remove(emitter)
            if (list.isEmpty()) channels.remove(key, list)
        }
        emitter.onCompletion(cleanup)
        emitter.onTimeout {
            cleanup()
            emitter.complete()
        }
        emitter.onError { cleanup() }

        try {
            emitter.send(SseEmitter.event().name("ping").data("connected"))
            if (initialSnapshot != null) {
                val json = objectMapper.writeValueAsString(initialSnapshot)
                emitter.send(SseEmitter.event().name("snapshot").data(json, MediaType.TEXT_PLAIN))
            }
        } catch (_: Exception) {
            cleanup()
        }
        return emitter
    }

    private fun publish(key: String, eventName: String, payload: Any) {
        val list = channels[key] ?: return
        val json = objectMapper.writeValueAsString(payload)
        list.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().name(eventName).data(json, MediaType.TEXT_PLAIN))
            } catch (_: Exception) {
                list.remove(emitter)
            }
        }
    }

    @Scheduled(fixedRate = 15_000L)
    open fun sendHeartbeat() {
        channels.forEach { (key, list) ->
            list.forEach { emitter ->
                try {
                    emitter.send(SseEmitter.event().comment("keepalive"))
                } catch (_: Exception) {
                    list.remove(emitter)
                }
            }
            if (list.isEmpty()) channels.remove(key, list)
        }
    }
}