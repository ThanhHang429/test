package com.eventapp.integration.payment

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

/**
 * Sinh ảnh QR PNG từ payload text.
 *
 * Sinh ở phía server để client chỉ cần tải ảnh, không phải kèm thư viện mã hoá QR.
 */
@Component
class QrCodeBuilder {

    fun png(payload: String, size: Int = 720): ByteArray {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints)
        val out = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(matrix, "PNG", out)
        return out.toByteArray()
    }
}
