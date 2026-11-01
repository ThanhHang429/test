package com.eventapp.app.ui.model;

import com.eventapp.app.core.enums.EventCategory;

/**
 * UI model cho danh sách "Vé của tôi" (FR-U14 + FR-U18 gộp chung một màn).
 * Dữ liệu thật sẽ được map từ DTO của backend, adapter không phụ thuộc tên field API.
 */
public class TicketUiModel {

    public enum Status {
        /** Vé cho sự kiện chưa diễn ra — tab "Sắp diễn ra". */
        UPCOMING,
        /** Sự kiện đã kết thúc — tab "Đã qua". */
        PAST,
        /** Đơn bị huỷ, vé không còn giá trị sử dụng. */
        CANCELLED
    }

    public final long id;
    public final long eventId;
    public final String eventTitle;
    public final String coverUrl;
    public final EventCategory category;
    public final String ticketType;
    public final String venue;
    public final String city;
    public final long startAt;
    public final int quantity;
    public final long totalPrice;
    public final String orderCode;
    public final Status status;

    private TicketUiModel(Builder builder) {
        id = builder.id;
        eventId = builder.eventId;
        eventTitle = builder.eventTitle;
        coverUrl = builder.coverUrl;
        category = builder.category;
        ticketType = builder.ticketType;
        venue = builder.venue;
        city = builder.city;
        startAt = builder.startAt;
        quantity = builder.quantity;
        totalPrice = builder.totalPrice;
        orderCode = builder.orderCode;
        status = builder.status;
    }

    public boolean isUpcoming() {
        return status == Status.UPCOMING;
    }

    public static class Builder {
        private long id;
        private long eventId;
        private String eventTitle = "";
        private String coverUrl = "";
        private EventCategory category = EventCategory.OTHER;
        private String ticketType = "";
        private String venue = "";
        private String city = "";
        private long startAt;
        private int quantity = 1;
        private long totalPrice;
        private String orderCode = "";
        private Status status = Status.UPCOMING;

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder eventId(long eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder eventTitle(String eventTitle) {
            this.eventTitle = eventTitle == null ? "" : eventTitle;
            return this;
        }

        public Builder coverUrl(String coverUrl) {
            this.coverUrl = coverUrl == null ? "" : coverUrl.trim();
            return this;
        }

        public Builder category(EventCategory category) {
            this.category = category == null ? EventCategory.OTHER : category;
            return this;
        }

        public Builder ticketType(String ticketType) {
            this.ticketType = ticketType == null ? "" : ticketType;
            return this;
        }

        public Builder venue(String venue) {
            this.venue = venue == null ? "" : venue;
            return this;
        }

        public Builder city(String city) {
            this.city = city == null ? "" : city;
            return this;
        }

        public Builder startAt(long startAt) {
            this.startAt = startAt;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = Math.max(1, quantity);
            return this;
        }

        public Builder totalPrice(long totalPrice) {
            this.totalPrice = Math.max(0L, totalPrice);
            return this;
        }

        public Builder orderCode(String orderCode) {
            this.orderCode = orderCode == null ? "" : orderCode;
            return this;
        }

        public Builder status(Status status) {
            this.status = status == null ? Status.UPCOMING : status;
            return this;
        }

        public TicketUiModel build() {
            return new TicketUiModel(this);
        }
    }
}
