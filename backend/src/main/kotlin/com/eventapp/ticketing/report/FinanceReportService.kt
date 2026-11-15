package com.eventapp.ticketing.report

import com.eventapp.common.enums.OrderStatus
import com.eventapp.common.enums.TicketStatus
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.repository.EventRepository
import com.eventapp.event.repository.TicketTypeRepository
import com.eventapp.ticketing.TicketingIdentitySupport
import com.eventapp.ticketing.dto.*
import com.eventapp.ticketing.entity.PlatformSetting
import com.eventapp.ticketing.order.OrderService
import com.eventapp.ticketing.repository.*
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Service
class FinanceReportService(
    private val eventRepository: EventRepository,
    private val ticketTypeRepository: TicketTypeRepository,
    private val ticketRepository: TicketRepository,
    private val checkInRepository: CheckInRepository,
    private val orderRepository: OrderRepository,
    private val refundRepository: RefundRepository,
    private val platformSettingRepository: PlatformSettingRepository,
    private val webhookLogRepository: PaymentWebhookLogRepository,
    private val orderService: OrderService,
    private val identitySupport: TicketingIdentitySupport
) {
    private fun currentFeeRate(): Double =
        (platformSettingRepository
            .findFirstBySettingKeyAndEffectiveFromLessThanEqualOrderByEffectiveFromDescVersionNoDesc(
                "PLATFORM_FEE_RATE",
                Instant.now()
            ) ?: platformSettingRepository.findFirstBySettingKeyOrderByVersionNoDesc("PLATFORM_FEE_RATE"))
            ?.settingValue?.trim()?.removeSurrounding("\"")?.toDoubleOrNull() ?: 5.0

    private fun currentSettlementCycle(): String =
        platformSettingRepository.findFirstBySettingKeyOrderByVersionNoDesc("SETTLEMENT_CYCLE")
            ?.settingValue?.trim()?.removeSurrounding("\"") ?: "Hàng tháng ngày 05"

    /** FR-O26: Báo cáo hiệu quả & tài chính chi tiết theo từng sự kiện cho Organizer. */
    @Transactional(readOnly = true)
    fun getEventFinanceReport(callerUserId: Long, eventId: Long): EventFinanceReportDto {
        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.createdByUserId != callerUserId && !identitySupport.isAdmin()) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn không có quyền xem báo cáo sự kiện này")
        }

        val ticketTypes = ticketTypeRepository.findByEventIdIn(listOf(eventId))
        val ticketsSold = ticketTypes.sumOf { it.quantitySold }
        val capacityTotal = event.capacityLimit ?: ticketTypes.sumOf { it.quantityTotal ?: 0 }

        val validCount = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.VALID).toInt()
        val checkedInCount = checkInRepository.countByEventId(eventId).toInt()
        val ticketsIssued = validCount + checkedInCount

        val orders = orderRepository.findByEventId(eventId)
        val revenueStatuses = setOf(OrderStatus.PAID, OrderStatus.REFUND_PENDING, OrderStatus.PARTIALLY_REFUNDED)
        val paidOrders = orders.filter { it.status in revenueStatuses }
        val refundedOrdersCount = orders.count { it.status == OrderStatus.REFUNDED }

        val grossRevenue = paidOrders.sumOf { it.totalAmount }
        val discountTotal = paidOrders.sumOf { it.discountAmount }
        val platformFeeAmount = paidOrders.sumOf { it.platformFeeAmount }
        val refundedAmount = refundRepository.sumApprovedAmountByEventId(eventId)
        val pendingRefundsCount = refundRepository.countPendingByEventId(eventId).toInt()

        val netRevenue = paidOrders
            .sumOf { it.totalAmount.subtract(it.platformFeeAmount) }
            .subtract(refundedAmount)
            .max(BigDecimal.ZERO)

        return EventFinanceReportDto(
            eventId = eventId,
            eventTitle = event.title,
            ticketsSold = ticketsSold,
            capacityTotal = capacityTotal,
            ticketsIssued = ticketsIssued,
            checkedInCount = checkedInCount,
            grossRevenue = grossRevenue,
            discountTotal = discountTotal,
            platformFeeRate = currentFeeRate(),
            platformFeeAmount = platformFeeAmount,
            refundedAmount = refundedAmount,
            netRevenue = netRevenue,
            paidOrdersCount = paidOrders.size,
            refundedOrdersCount = refundedOrdersCount,
            pendingRefundsCount = pendingRefundsCount
        )
    }

    /** FR-A17: Báo cáo tài chính toàn nền tảng cho Admin (không tính đơn PENDING/EXPIRED/CANCELLED). */
    @Transactional(readOnly = true)
    fun getAdminFinanceSummary(): AdminFinanceSummaryDto {
        identitySupport.requireAdmin()

        val feeRate = currentFeeRate()
        val cycle = currentSettlementCycle()

        val allEvents = eventRepository.findAll().filter { it.deletedAt == null }
        val eventMap = allEvents.associateBy { it.eventId!! }

        val allOrders = orderRepository.findAll()
        val revenueStatuses = setOf(OrderStatus.PAID, OrderStatus.REFUND_PENDING, OrderStatus.PARTIALLY_REFUNDED)
        val paidOrders = allOrders.filter { it.status in revenueStatuses }

        val totalGmv = paidOrders.sumOf { it.totalAmount }
        val totalFee = paidOrders.sumOf { it.platformFeeAmount }

        val byOrgUserId = paidOrders.groupBy { eventMap[it.eventId]?.createdByUserId ?: 0L }
            .filterKeys { it > 0L }
        val orgNames = identitySupport.loadOrganizerNames(byOrgUserId.keys)

        val reconciliations = byOrgUserId.entries.map { (orgUserId, orgOrders) ->
            val orgGmv = orgOrders.sumOf { it.totalAmount }
            val orgFee = orgOrders.sumOf { it.platformFeeAmount }
            OrganizerReconItemDto(
                organizerId = orgUserId,
                organizerName = orgNames[orgUserId] ?: "Organizer #$orgUserId",
                gmv = orgGmv,
                fee = orgFee,
                status = if (orgOrders.any { it.status == OrderStatus.REFUND_PENDING }) "PENDING" else "PAID"
            )
        }.sortedByDescending { it.gmv }

        val distinctOrganizers = allEvents.map { it.createdByUserId }.distinct().size

        return AdminFinanceSummaryDto(
            totalGmv = totalGmv,
            platformFeeCollected = totalFee,
            platformFeeOutstanding = BigDecimal.ZERO,
            platformFeeRate = feeRate,
            settlementCycle = cycle,
            totalOrders = allOrders.size,
            paidOrders = paidOrders.size,
            totalEvents = allEvents.size,
            totalOrganizers = distinctOrganizers,
            reconciliations = reconciliations
        )
    }

    /** FR-A16: Admin cập nhật tỷ lệ phí nền tảng (tạo version mới + effectiveFrom). */
    @Transactional
    fun updateFeeRate(adminUserId: Long, request: UpdateFeeRateRequest): AdminFinanceSummaryDto {
        identitySupport.requireAdmin()
        if (request.feeRate < 0.0 || request.feeRate > 30.0) {
            throw validationError("feeRate", "Tỷ lệ phí phải từ 0% đến 30%")
        }
        val latest = platformSettingRepository.findFirstBySettingKeyOrderByVersionNoDesc("PLATFORM_FEE_RATE")
        val nextVersion = (latest?.versionNo ?: 0) + 1
        platformSettingRepository.save(
            PlatformSetting(
                settingKey = "PLATFORM_FEE_RATE",
                versionNo = nextVersion,
                settingValue = "\"${BigDecimal.valueOf(request.feeRate).stripTrailingZeros().toPlainString()}\"",
                description = request.description?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "Admin #$adminUserId cập nhật tỷ lệ phí nền tảng",
                effectiveFrom = request.effectiveFrom ?: Instant.now(),
                changedByUserId = adminUserId
            )
        )
        return getAdminFinanceSummary()
    }

    /** FR-A10: Admin tra cứu đơn hàng theo mã đơn. */
    @Transactional(readOnly = true)
    fun lookupOrder(query: String): OrderDto {
        identitySupport.requireAdmin()
        val q = query.trim()
        if (q.isEmpty()) throw validationError("query", "Vui lòng nhập mã đơn hàng cần tra cứu")

        val exact = orderRepository.findByOrderCode(q.uppercase())
        if (exact != null) return orderService.toDto(exact)

        val partial = orderRepository.findByOrderCodeContainingIgnoreCaseOrderByCreatedAtDesc(q).firstOrNull()
            ?: throw NotFoundException(message = "Không tìm thấy đơn hàng khớp với \"$q\"")
        return orderService.toDto(partial)
    }

    /** FR-A11: Admin xem nhật ký Webhook cổng thanh toán để đối soát. */
    @Transactional(readOnly = true)
    fun getWebhookLogs(limit: Int): List<WebhookLogItemDto> {
        identitySupport.requireAdmin()
        val safeLimit = limit.coerceIn(1, 100)
        return webhookLogRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
            .take(safeLimit)
            .map { log ->
                val mappedStatus = if (log.targetType.equals("UNMATCHED", ignoreCase = true)) {
                    "UNMATCHED"
                } else {
                    "SUCCESS"
                }
                WebhookLogItemDto(
                    logId = log.id ?: 0L,
                    providerCode = log.providerCode,
                    eventType = log.targetType ?: "PAYMENT",
                    externalTransactionId = log.providerTransactionId,
                    transferContent = log.transferContent.orEmpty(),
                    amount = log.amount,
                    status = mappedStatus,
                    errorMessage = if (mappedStatus == "UNMATCHED") "Không tìm thấy đơn hàng hoặc lệnh nạp khớp nội dung" else null,
                    receivedAt = log.processedAt
                )
            }
    }
}