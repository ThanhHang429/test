package com.eventapp.ticketing.dto

import java.math.BigDecimal
import java.time.Instant

data class OrganizerReconItemDto(
    val organizerId: Long,
    val organizerName: String,
    val gmv: BigDecimal,
    val fee: BigDecimal,
    val status: String
)

data class AdminFinanceSummaryDto(
    val totalGmv: BigDecimal,
    val platformFeeCollected: BigDecimal,
    val platformFeeOutstanding: BigDecimal,
    val platformFeeRate: Double,
    val settlementCycle: String,
    val totalOrders: Int,
    val paidOrders: Int,
    val totalEvents: Int,
    val totalOrganizers: Int,
    val reconciliations: List<OrganizerReconItemDto>
)

data class UpdateFeeRateRequest(
    val feeRate: Double,
    val effectiveFrom: Instant? = null,
    val description: String? = null
)

data class EventFinanceReportDto(
    val eventId: Long,
    val eventTitle: String,
    val ticketsSold: Int,
    val capacityTotal: Int,
    val ticketsIssued: Int,
    val checkedInCount: Int,
    val grossRevenue: BigDecimal,
    val discountTotal: BigDecimal,
    val platformFeeRate: Double,
    val platformFeeAmount: BigDecimal,
    val refundedAmount: BigDecimal,
    val netRevenue: BigDecimal,
    val paidOrdersCount: Int,
    val refundedOrdersCount: Int,
    val pendingRefundsCount: Int
)

data class WebhookLogItemDto(
    val logId: Long,
    val providerCode: String,
    val eventType: String,
    val externalTransactionId: String,
    val transferContent: String,
    val amount: BigDecimal,
    val status: String,
    val errorMessage: String?,
    val receivedAt: Instant?
)