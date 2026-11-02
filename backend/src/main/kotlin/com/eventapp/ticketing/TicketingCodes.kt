package com.eventapp.ticketing

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Sinh mã đơn/vé/đăng ký và mã QR soát vé.
 *
 * <p>QR là HMAC-SHA256 của mã vé với khoá chỉ server biết, nên có thể sinh lại bất cứ lúc nào để
 * hiển thị mà không cần lưu bản rõ. DB chỉ lưu SHA-256 của mã QR ([hashToken]) — lộ DB cũng không
 * dựng lại được mã hợp lệ nếu thiếu khoá.</p>
 */
@Component
class TicketingCodes(
    @Value("\${app.ticketing.qr-secret:}") qrSecret: String,
    @Value("\${app.jwt.secret}") jwtSecret: String
) {

    private val secretKey: SecretKeySpec = SecretKeySpec(
        (qrSecret.ifBlank { jwtSecret }).toByteArray(Charsets.UTF_8),
        "HmacSHA256"
    )
    private val random = SecureRandom()
    private val dayFormat = DateTimeFormatter.ofPattern("yyMMdd").withZone(ZoneId.of("UTC"))

    fun newOrderCode(now: Instant = Instant.now()): String = "ORD" + dayFormat.format(now) + suffix(6)

    fun newTicketCode(now: Instant = Instant.now()): String = "TKT" + dayFormat.format(now) + suffix(8)

    fun newRegistrationCode(now: Instant = Instant.now()): String = "REG" + dayFormat.format(now) + suffix(8)

    fun newPaymentReference(now: Instant = Instant.now()): String = "PAY" + dayFormat.format(now) + suffix(8)

    /** Mã QR hiển thị cho người dùng; tất định theo mã vé nên đọc lại vé vẫn ra cùng mã. */
    fun qrTokenFor(ticketCode: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal("ticket:$ticketCode".toByteArray(Charsets.UTF_8)).toHex()
    }

    /** Giá trị lưu ở `tickets.qr_token_hash` (CHAR(64), UNIQUE). */
    fun hashToken(rawToken: String): String =
        MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray(Charsets.UTF_8)).toHex()

    private fun suffix(length: Int): String {
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
            .replace("-", "")
            .replace("_", "")
            .uppercase()
            .take(length)
    }

    private fun ByteArray.toHex(): String {
        val out = StringBuilder(size * 2)
        for (b in this) out.append("%02x".format(b))
        return out.toString()
    }
}
