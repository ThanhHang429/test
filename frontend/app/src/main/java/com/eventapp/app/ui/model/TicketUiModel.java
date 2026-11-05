package com.eventapp.app.ui.model;

import androidx.annotation.DrawableRes;

import com.eventapp.app.core.enums.EventCategory;

/**
 * Model hiển thị cho "Vé của tôi" (FR-U14 + FR-U18).
 *
 * <p>Backend chưa mở API đơn hàng/vé nên dữ liệu hiện do {@code DemoContent} sinh ra, nhưng màn
 * hình chỉ làm việc với model này — khi có API chỉ cần thay nguồn dữ liệu.</p>
 */
public class TicketUiModel {

    public enum Status {
        /** Vé cho sự kiện chưa diễn ra — tab "Sắp diễn ra". */
        UPCOMING,
        /** Vé đã check-in. */
        USED,
        /** Sự kiện đã kết thúc nhưng vé chưa dùng — tab "Đã qua". */
        PAST,
        /** Đơn bị huỷ, vé không còn giá trị sử dụng. */
        CANCELLED
    }

    public final long id;
    public final long eventId;
    /** Mã vé in trên QR. */
    public final String code;
    public final String eventTitle;
    public final String coverUrl;
    public final EventCategory category;
    public final String ticketType;
    public final String venue;
    public final String city;
    public final long startAt;
    public final long endAt;
    public final int quantity;
    public final long totalPrice;
    public final String orderCode;
    public final Status status;

    private TicketUiModel(Builder builder) {
        id = builder.id;
        eventId = builder.eventId;
        code = builder.code;
        eventTitle = builder.eventTitle;
        coverUrl = builder.coverUrl;
        category = builder.category;
        ticketType = builder.ticketType;
        venue = builder.venue;
        city = builder.city;
        startAt = builder.startAt;
        endAt = builder.endAt;
        quantity = builder.quantity;
        totalPrice = builder.totalPrice;
        orderCode = builder.orderCode;
        status = builder.status;
    }

    public boolean isUpcoming() {
        return status == Status.UPCOMING;
    }

    @DrawableRes
    public int coverRes() {
        return EventCategoryUi.coverRes(category);
    }

    @DrawableRes
    public int iconRes() {
        return EventCategoryUi.iconRes(category);
    }

    public static class Builder {
        private long id;
        private long eventId;
        private String code = "";
        private String eventTitle = "";
        private String coverUrl = "";
        private EventCategory category = EventCategory.OTHER;
        private String ticketType = "";
        private String venue = "";
        private String city = "";
        private long startAt;
        private long endAt;
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

        public Builder code(String code) {
            this.code = code == null ? "" : code;
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

        public Builder endAt(long endAt) {
            this.endAt = endAt;
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
