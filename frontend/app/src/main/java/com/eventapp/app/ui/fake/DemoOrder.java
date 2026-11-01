package com.eventapp.app.ui.fake;

import java.io.Serializable;

/**
 * Đơn hàng mẫu dùng riêng cho luồng UI mua vé / đăng ký miễn phí.
 *
 * <p>Không phải DTO của backend: lớp này chỉ mang dữ liệu giữa các màn hình trong giai đoạn
 * UI-only và có thể bị thay thế khi tầng đơn hàng thật được nối vào.</p>
 */
public final class DemoOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum State {
        PENDING_PAYMENT,
        PAID,
        FREE_CONFIRMED,
        CANCELLED
    }

    public final String code;
    public final long eventId;
    public final String eventTitle;
    public final String eventVenue;
    public final String eventCity;
    public final long eventStartAt;
    public final String attendeeName;
    public final String attendeeEmail;
    public final String attendeePhone;
    public final String ticketName;
    public final int quantity;
    public final long unitPrice;
    public final long total;
    public final State state;
    public final long createdAt;
    public final String paymentMethod;
    public final String ticketCode;

    DemoOrder(String code,
              long eventId,
              String eventTitle,
              String eventVenue,
              String eventCity,
              long eventStartAt,
              String attendeeName,
              String attendeeEmail,
              String attendeePhone,
              String ticketName,
              int quantity,
              long unitPrice,
              long total,
              State state,
              long createdAt,
              String paymentMethod,
              String ticketCode) {
        this.code = code;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.eventVenue = eventVenue;
        this.eventCity = eventCity;
        this.eventStartAt = eventStartAt;
        this.attendeeName = attendeeName;
        this.attendeeEmail = attendeeEmail;
        this.attendeePhone = attendeePhone;
        this.ticketName = ticketName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = total;
        this.state = state;
        this.createdAt = createdAt;
        this.paymentMethod = paymentMethod;
        this.ticketCode = ticketCode;
    }

    public DemoOrder withState(State newState) {
        return new DemoOrder(code, eventId, eventTitle, eventVenue, eventCity, eventStartAt,
                attendeeName, attendeeEmail, attendeePhone, ticketName, quantity, unitPrice, total,
                newState, createdAt, paymentMethod, ticketCode);
    }

    public boolean isFree() {
        return total <= 0L;
    }
}
