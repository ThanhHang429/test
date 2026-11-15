package com.eventapp.ticketing.wallet

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import com.eventapp.ticketing.dto.*
// [MỚI] Bổ sung 3 import bị thiếu
import com.eventapp.ticketing.payment.PaymentSseHub
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Tag(name = "Ví ETicket", description = "Quản lý ví điện tử, nạp tiền và thanh toán bằng ví")
@RestController
@RequestMapping("/api/v1/wallets")
class WalletController(
    private val walletService: WalletService,
    private val paymentSseHub: PaymentSseHub
) {

    @Operation(summary = "Lấy thông tin ví của người dùng hiện tại")
    @GetMapping("/me")
    fun getMyWallet(): ApiResponse<WalletDto?> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(walletService.getMyWallet(userId))
    }

    @Operation(summary = "Kích hoạt tạo ví mới cho người dùng (Số dư khởi tạo = 0 đ)")
    @PostMapping
    fun createWallet(): ApiResponse<WalletDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(walletService.createWallet(userId))
    }

    @Operation(summary = "Nạp tiền trực tiếp vào ví qua Thẻ ngân hàng")
    @PostMapping("/topup")
    fun topup(@RequestBody request: TopupWalletRequest): ApiResponse<WalletDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(walletService.topup(userId, request))
    }

    @Operation(summary = "Tạo yêu cầu nạp tiền ví VietQR chờ Webhook + SSE")
    @PostMapping("/topups")
    fun createTopupIntent(@RequestBody request: TopupWalletRequest): ApiResponse<TopupIntentResponse> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(walletService.createTopupIntent(userId, request))
    }

    @Operation(summary = "Xem trạng thái yêu cầu nạp tiền vào ví")
    @GetMapping("/topups/{topupId}")
    fun getTopupIntent(@PathVariable topupId: Long): ApiResponse<TopupIntentResponse> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(walletService.getTopupIntent(userId, topupId))
    }

    @Operation(summary = "Lắng nghe SSE trạng thái nạp tiền ví theo thời gian thực")
    @GetMapping("/topups/{topupId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamTopupEvents(@PathVariable topupId: Long): SseEmitter {
        val userId = CurrentUser.requireUserId()
        val current = walletService.getTopupIntent(userId, topupId)
        return paymentSseHub.subscribeTopup(topupId, current)
    }

    @Operation(summary = "Thanh toán đơn hàng bằng số dư Ví ETicket")
    @PostMapping("/pay")
    fun payOrder(@RequestBody request: PayWalletRequest): ApiResponse<WalletTransactionDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(walletService.payOrder(userId, request))
    }

    @Operation(summary = "Lấy danh sách lịch sử biến động số dư ví")
    @GetMapping("/transactions")
    fun getTransactions(): ApiResponse<List<WalletTransactionDto>> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(walletService.getTransactions(userId))
    }
    @Operation(summary = "FR-U13: Thiết lập hoặc đổi mã PIN 6 số cho Ví ETicket")
    @PostMapping("/pin")
    fun setPin(@RequestBody request: SetWalletPinRequest): ApiResponse<WalletDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(walletService.setPin(userId, request))
    }
}