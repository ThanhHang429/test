package com.eventapp.app.ui.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;
import com.eventapp.app.ui.model.EventUiModel;

import java.util.List;
import java.util.Locale;

/** Chuyển DTO sự kiện của backend sang mô hình UI dùng chung. */
public final class EventMapper {

    private EventMapper() {
    }

    @NonNull
    public static EventUiModel fromCard(@Nullable DiscoveryDtos.EventCard dto) {
        if (dto == null) {
            return new EventUiModel.Builder().build();
        }
        EventUiModel.Builder builder = new EventUiModel.Builder()
                .id(dto.id)
                .title(dto.title)
                .description(dto.shortDescription)
                .coverUrl(dto.coverUrl)
                .venue(dto.venueName)
                .city(dto.city)
                .startAt(DateTimeUtils.toMillis(dto.startAt))
                .endAt(DateTimeUtils.toMillis(dto.endAt))
                .online("ONLINE".equalsIgnoreCase(dto.eventFormat))
                .priceFrom(dto.minPrice == null ? 0L : Math.round(dto.minPrice))
                .free(dto.isFree)
                .availability(availability(dto))
                .category(categoryOf(dto.categoryNames, dto.tags))
                .favorite(dto.isFavorite != null && dto.isFavorite)
                .aiReason(dto.reason);
        if (dto.tags != null) {
            for (String tag : dto.tags) {
                builder.addTag(tag);
            }
        }
        return builder.build();
    }

    @NonNull
    public static EventUiModel fromDetail(@Nullable DiscoveryDtos.EventDetail dto) {
        if (dto == null) {
            return new EventUiModel.Builder().build();
        }
        EventUiModel.Builder builder = new EventUiModel.Builder()
                .id(dto.id)
                .title(dto.title)
                .description(dto.description == null ? dto.shortDescription : dto.description)
                .coverUrl(dto.coverUrl)
                .organizer(dto.organizer == null ? "" : dto.organizer.publicName)
                .venue(dto.venueName)
                .city(dto.city)
                .startAt(DateTimeUtils.toMillis(dto.startAt))
                .endAt(DateTimeUtils.toMillis(dto.endAt))
                .online("ONLINE".equalsIgnoreCase(dto.eventFormat))
                .priceFrom(dto.minPrice == null ? 0L : Math.round(dto.minPrice))
                .free(dto.isFree)
                .availability(detailAvailability(dto))
                .category(categoryOf(dto.categoryNames, dto.tags))
                .favorite(dto.isFavorite);
        if (dto.reviewSummary != null) {
            builder.rating((float) dto.reviewSummary.averageRating)
                    .reviewCount((int) Math.min(Integer.MAX_VALUE, dto.reviewSummary.totalReviews));
        }
        if (dto.tags != null) {
            for (String tag : dto.tags) {
                builder.addTag(tag);
            }
        }
        return builder.build();
    }

    @NonNull
    private static EventUiModel.Availability availability(DiscoveryDtos.EventCard dto) {
        if (dto.availabilityUnknown) {
            return EventUiModel.Availability.UNKNOWN;
        }
        if ("SOLD_OUT".equalsIgnoreCase(dto.saleStatus) || Boolean.FALSE.equals(dto.available)
                && dto.statusLabel != null && dto.statusLabel.toLowerCase(Locale.ROOT).contains("hết")) {
            return EventUiModel.Availability.SOLD_OUT;
        }
        return EventUiModel.Availability.AVAILABLE;
    }

    @NonNull
    private static EventUiModel.Availability detailAvailability(DiscoveryDtos.EventDetail dto) {
        if ("CANCELLED".equalsIgnoreCase(dto.lifecycleStatus)) {
            return EventUiModel.Availability.CANCELLED;
        }
        if ("COMPLETED".equalsIgnoreCase(dto.lifecycleStatus)) {
            return EventUiModel.Availability.SOLD_OUT;
        }
        return EventUiModel.Availability.AVAILABLE;
    }

    /** Đoán nhóm hiển thị từ tên danh mục backend (danh mục thật có id 9–24). */
    @NonNull
    public static EventCategory categoryOf(@Nullable List<String> names,
                                           @Nullable List<String> tags) {
        String source = names == null ? "" : String.join(" ", names).toLowerCase(Locale.ROOT);
        if (source.contains("nhạc") || source.contains("music")) {
            return EventCategory.MUSIC;
        }
        if (source.contains("thể thao") || source.contains("sport")) {
            return EventCategory.SPORTS;
        }
        if (source.contains("nghệ thuật") || source.contains("art")) {
            return EventCategory.ART;
        }
        if (source.contains("ẩm thực") || source.contains("food")) {
            return EventCategory.FOOD;
        }
        if (source.contains("công nghệ") || source.contains("tech")) {
            return EventCategory.TECH;
        }
        if (source.contains("kinh doanh") || source.contains("doanh nghiệp")
                || source.contains("business")) {
            return EventCategory.BUSINESS;
        }
        if (source.contains("giáo dục") || source.contains("education")) {
            return EventCategory.EDUCATION;
        }
        if (source.contains("sức khỏe") || source.contains("health")) {
            return EventCategory.HEALTH;
        }
        if (tags != null && !tags.isEmpty()) {
            return categoryOf(tags, null);
        }
        return EventCategory.OTHER;
    }
}
