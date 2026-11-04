package com.eventapp.app.ui.user.model;

import androidx.annotation.DrawableRes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sự kiện mẫu dùng cho giai đoạn dựng giao diện.
 *
 * <p>Chỉ tồn tại ở tầng UI: khi nối API thật, dữ liệu này được thay bằng {@code EventUiModel}
 * do repository trả về. Không có logic nghiệp vụ nào phụ thuộc vào class này.</p>
 */
public class DemoEvent {

    public final long id;
    public final String title;
    public final String categoryKey;
    public final String categoryLabel;
    public final String organizer;
    public final String venue;
    public final String city;
    public final long startAt;
    public final long endAt;
    public final long priceFrom;
    public final int remaining;
    public final String description;
    @DrawableRes
    public final int coverRes;
    @DrawableRes
    public final int iconRes;
    public final String tag;
    public final List<TicketType> ticketTypes;

    private DemoEvent(Builder builder) {
        id = builder.id;
        title = builder.title;
        categoryKey = builder.categoryKey;
        categoryLabel = builder.categoryLabel;
        organizer = builder.organizer;
        venue = builder.venue;
        city = builder.city;
        startAt = builder.startAt;
        endAt = builder.endAt;
        priceFrom = builder.priceFrom;
        remaining = builder.remaining;
        description = builder.description;
        coverRes = builder.coverRes;
        iconRes = builder.iconRes;
        tag = builder.tag;
        ticketTypes = Collections.unmodifiableList(new ArrayList<>(builder.ticketTypes));
    }

    public boolean isFree() {
        return priceFrom <= 0L;
    }

    public boolean matchesCategory(String key) {
        return key == null || key.isEmpty() || "all".equals(key) || key.equals(categoryKey);
    }

    /** Loại vé rẻ nhất còn mở bán; dùng cho nút đặt vé nhanh. */
    public TicketType cheapestAvailable() {
        TicketType result = null;
        for (TicketType type : ticketTypes) {
            if (type.remaining <= 0) {
                continue;
            }
            if (result == null || type.price < result.price) {
                result = type;
            }
        }
        return result;
    }

    public static class TicketType {
        public final String name;
        public final String note;
        public final long price;
        public final int remaining;
        public final boolean popular;

        public TicketType(String name, String note, long price, int remaining, boolean popular) {
            this.name = name;
            this.note = note;
            this.price = price;
            this.remaining = remaining;
            this.popular = popular;
        }

        public boolean soldOut() {
            return remaining <= 0;
        }
    }

    public static class Builder {
        private long id;
        private String title = "";
        private String categoryKey = "all";
        private String categoryLabel = "";
        private String organizer = "";
        private String venue = "";
        private String city = "TP. Hồ Chí Minh";
        private long startAt;
        private long endAt;
        private long priceFrom;
        private int remaining;
        private String description = "";
        @DrawableRes
        private int coverRes;
        @DrawableRes
        private int iconRes;
        private String tag = "";
        private final List<TicketType> ticketTypes = new ArrayList<>();

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder category(String key, String label) {
            this.categoryKey = key;
            this.categoryLabel = label;
            return this;
        }

        public Builder organizer(String organizer) {
            this.organizer = organizer;
            return this;
        }

        public Builder venue(String venue, String city) {
            this.venue = venue;
            this.city = city;
            return this;
        }

        public Builder time(long startAt, long endAt) {
            this.startAt = startAt;
            this.endAt = endAt;
            return this;
        }

        public Builder priceFrom(long priceFrom) {
            this.priceFrom = priceFrom;
            return this;
        }

        public Builder remaining(int remaining) {
            this.remaining = remaining;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder cover(@DrawableRes int coverRes, @DrawableRes int iconRes) {
            this.coverRes = coverRes;
            this.iconRes = iconRes;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder ticketType(String name, String note, long price, int remaining) {
            ticketTypes.add(new TicketType(name, note, price, remaining, false));
            return this;
        }

        public Builder popularTicket(String name, String note, long price, int remaining) {
            ticketTypes.add(new TicketType(name, note, price, remaining, true));
            return this;
        }

        public DemoEvent build() {
            return new DemoEvent(this);
        }
    }
}
