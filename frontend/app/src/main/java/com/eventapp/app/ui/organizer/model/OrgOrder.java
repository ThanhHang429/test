package com.eventapp.app.ui.organizer.model;

/** Đơn hàng trong khu nhà tổ chức. */
public final class OrgOrder {

    public static final String FILTER_ALL = "ALL";
    public static final String FILTER_PAID = "PAID";
    public static final String FILTER_PENDING = "PENDING";
    public static final String FILTER_REFUND = "REFUND";

    public final long orderId;
    public final String rawStatus;
    public final String code;
    public final long amount;
    public final String filter;
    public final String statusLabel;
    public final int tone;
    public final String buyer;
    public final String event;
    public final String ticketType;
    public final String time;
    public final String payment;
    public final String contact;

    public OrgOrder(String code, long amount, String filter, String statusLabel, int tone,
                    String buyer, String event, String ticketType, String time, String payment,
                    String contact) {
        this(0L, filter, code, amount, filter, statusLabel, tone, buyer, event, ticketType, time, payment, contact);
    }

    public OrgOrder(long orderId, String rawStatus, String code, long amount, String filter,
                    String statusLabel, int tone, String buyer, String event, String ticketType,
                    String time, String payment, String contact) {
        this.orderId = orderId;
        this.rawStatus = rawStatus;
        this.code = code;
        this.amount = amount;
        this.filter = filter;
        this.statusLabel = statusLabel;
        this.tone = tone;
        this.buyer = buyer;
        this.event = event;
        this.ticketType = ticketType;
        this.time = time;
        this.payment = payment;
        this.contact = contact;
    }
}
