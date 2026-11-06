package com.eventapp.app.ui.model;

import com.eventapp.app.core.enums.EventCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model hiển thị cho sự kiện — dùng chung cho thẻ (FR-U04) và chi tiết (FR-U05).
 *
 * <p>Được map trực tiếp từ {@code DiscoveryDtos.EventCard} / {@code EventDetail} qua
 * {@code EventMapper}; màn hình và adapter không phụ thuộc tên field của API.</p>
 */
public class EventUiModel {

    public enum Availability {
        AVAILABLE,
        FEW_LEFT,
        SOLD_OUT,
        CANCELLED,
        UNKNOWN
    }

    public final long id;
    public final String code;
    public final String title;
    public final String description;
    /** URL ảnh bìa do backend trả về; rỗng thì card dùng placeholder theo category. */
    public final String coverUrl;
    public final String organizer;
    public final String venue;
    public final String city;
    public final long startAt;
    public final long endAt;
    public final boolean online;
    public final long priceFrom;
    public final long priceTo;
    public final boolean free;
    public final Availability availability;
    /** Nhãn do server tính: "Đang mở bán", "Hết vé", "Đã kết thúc bán"… */
    public final String statusLabel;
    public final String saleStatus;
    public final EventCategory category;
    public final float rating;
    public final int reviewCount;
    public final String aiReason;
    public final List<String> tags;

    /** Chi tiết sự kiện — rỗng ở thẻ danh sách. */
    public final List<TicketType> ticketTypes;
    public final List<String> allowedActions;
    public final String disabledReason;
    public final boolean canReview;
    public final String registrationStatus;

    /** Cờ yêu thích; cập nhật tại chỗ sau khi gọi API favorite. */
    public boolean favorite;

    private EventUiModel(Builder builder) {
        id = builder.id;
        code = builder.code;
        title = builder.title;
        description = builder.description;
        coverUrl = builder.coverUrl;
        organizer = builder.organizer;
        venue = builder.venue;
        city = builder.city;
        startAt = builder.startAt;
        endAt = builder.endAt;
        online = builder.online;
        priceFrom = builder.priceFrom;
        priceTo = builder.priceTo;
        free = builder.free;
        availability = builder.availability;
        statusLabel = builder.statusLabel;
        saleStatus = builder.saleStatus;
        category = builder.category;
        rating = builder.rating;
        reviewCount = builder.reviewCount;
        aiReason = builder.aiReason;
        tags = Collections.unmodifiableList(new ArrayList<>(builder.tags));
        ticketTypes = Collections.unmodifiableList(new ArrayList<>(builder.ticketTypes));
        allowedActions = Collections.unmodifiableList(new ArrayList<>(builder.allowedActions));
        disabledReason = builder.disabledReason;
        canReview = builder.canReview;
        registrationStatus = builder.registrationStatus;
        favorite = builder.favorite;
    }

    public boolean allows(String action) {
        for (String value : allowedActions) {
            if (value != null && value.equalsIgnoreCase(action)) {
                return true;
            }
        }
        return false;
    }

    /** Một hạng vé kèm tồn kho do server trả về. */
    public static final class TicketType {
        public final long id;
        public final String name;
        public final String description;
        /** FREE | PAID */
        public final String kind;
        public final long price;
        public final String currency;
        /** NOT_STARTED | ON_SALE | ENDED | SOLD_OUT */
        public final String saleStatus;
        /** null = server không công bố số vé còn lại. */
        public final Integer remaining;
        public final int minPerOrder;
        public final int maxPerUser;

        public TicketType(long id, String name, String description, String kind, long price,
                          String currency, String saleStatus, Integer remaining,
                          int minPerOrder, int maxPerUser) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.description = description == null ? "" : description;
            this.kind = kind == null ? "" : kind;
            this.price = price;
            this.currency = currency == null ? "VND" : currency;
            this.saleStatus = saleStatus == null ? "" : saleStatus;
            this.remaining = remaining;
            this.minPerOrder = Math.max(1, minPerOrder);
            this.maxPerUser = maxPerUser <= 0 ? 10 : maxPerUser;
        }

        public boolean isFree() {
            return price <= 0L || "FREE".equalsIgnoreCase(kind);
        }

        public boolean soldOut() {
            return "SOLD_OUT".equalsIgnoreCase(saleStatus)
                    || (remaining != null && remaining <= 0);
        }

        public boolean onSale() {
            return saleStatus.isEmpty() || "ON_SALE".equalsIgnoreCase(saleStatus);
        }
    }

    public static class Builder {
        private long id;
        private String code = "";
        private String title = "";
        private String description = "";
        private String coverUrl = "";
        private String organizer = "";
        private String venue = "";
        private String city = "";
        private long startAt;
        private long endAt;
        private boolean online;
        private long priceFrom;
        private long priceTo;
        private boolean free;
        private Availability availability = Availability.AVAILABLE;
        private String statusLabel = "";
        private String saleStatus = "";
        private EventCategory category = EventCategory.OTHER;
        private float rating;
        private int reviewCount;
        private String aiReason;
        private final List<String> tags = new ArrayList<>();
        private final List<TicketType> ticketTypes = new ArrayList<>();
        private final List<String> allowedActions = new ArrayList<>();
        private String disabledReason;
        private boolean canReview;
        private String registrationStatus;
        private boolean favorite;

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder code(String code) {
            this.code = code == null ? "" : code;
            return this;
        }

        public Builder title(String title) {
            this.title = title == null ? "" : title;
            return this;
        }

        public Builder description(String description) {
            this.description = description == null ? "" : description;
            return this;
        }

        public Builder organizer(String organizer) {
            this.organizer = organizer == null ? "" : organizer;
            return this;
        }

        public Builder coverUrl(String coverUrl) {
            this.coverUrl = coverUrl == null ? "" : coverUrl.trim();
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

        public Builder online(boolean online) {
            this.online = online;
            return this;
        }

        public Builder priceFrom(long priceFrom) {
            this.priceFrom = priceFrom;
            this.free = priceFrom <= 0L;
            return this;
        }

        public Builder priceTo(long priceTo) {
            this.priceTo = priceTo;
            return this;
        }

        public Builder free(boolean free) {
            this.free = free;
            return this;
        }

        public Builder availability(Availability availability) {
            this.availability = availability;
            return this;
        }

        public Builder statusLabel(String statusLabel) {
            this.statusLabel = statusLabel == null ? "" : statusLabel;
            return this;
        }

        public Builder saleStatus(String saleStatus) {
            this.saleStatus = saleStatus == null ? "" : saleStatus;
            return this;
        }

        public Builder category(EventCategory category) {
            this.category = category;
            return this;
        }

        public Builder rating(float rating) {
            this.rating = rating;
            return this;
        }

        public Builder reviewCount(int reviewCount) {
            this.reviewCount = reviewCount;
            return this;
        }

        public Builder aiReason(String aiReason) {
            this.aiReason = aiReason;
            return this;
        }

        public Builder addTag(String tag) {
            if (tag != null && !tag.trim().isEmpty()) {
                tags.add(tag.trim());
            }
            return this;
        }

        public Builder addTicketType(TicketType type) {
            if (type != null) {
                ticketTypes.add(type);
            }
            return this;
        }

        public Builder addAction(String action) {
            if (action != null && !action.trim().isEmpty()) {
                allowedActions.add(action.trim());
            }
            return this;
        }

        public Builder disabledReason(String disabledReason) {
            this.disabledReason = disabledReason;
            return this;
        }

        public Builder canReview(boolean canReview) {
            this.canReview = canReview;
            return this;
        }

        public Builder registrationStatus(String registrationStatus) {
            this.registrationStatus = registrationStatus;
            return this;
        }

        public Builder favorite(boolean favorite) {
            this.favorite = favorite;
            return this;
        }

        public EventUiModel build() {
            return new EventUiModel(this);
        }
    }
}
