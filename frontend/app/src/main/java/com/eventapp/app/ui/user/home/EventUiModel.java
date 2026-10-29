package com.eventapp.app.ui.user.home;

import com.eventapp.app.core.enums.EventCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UI model for FR-U04 cards. The real API DTO will be mapped into this class; screens and the
 * shared adapter never depend on backend DTO field names.
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
    public final String title;
    public final String description;
    public final String organizer;
    public final String venue;
    public final String city;
    public final long startAt;
    public final long endAt;
    public final boolean online;
    public final long priceFrom;
    public final boolean free;
    public final Availability availability;
    public final EventCategory category;
    public final float rating;
    public final int reviewCount;
    public final Integer aiScore;
    public final String aiReason;
    public final List<String> tags;

    /** Local-only flag until FR-U06 is wired to the backend. */
    public boolean favorite;

    private EventUiModel(Builder builder) {
        id = builder.id;
        title = builder.title;
        description = builder.description;
        organizer = builder.organizer;
        venue = builder.venue;
        city = builder.city;
        startAt = builder.startAt;
        endAt = builder.endAt;
        online = builder.online;
        priceFrom = builder.priceFrom;
        free = builder.free;
        availability = builder.availability;
        category = builder.category;
        rating = builder.rating;
        reviewCount = builder.reviewCount;
        aiScore = builder.aiScore;
        aiReason = builder.aiReason;
        tags = Collections.unmodifiableList(new ArrayList<>(builder.tags));
        favorite = builder.favorite;
    }

    public long categoryId() {
        return EventCategoryUi.id(category);
    }

    public static class Builder {
        private long id;
        private String title = "";
        private String description = "";
        private String organizer = "";
        private String venue = "";
        private String city = "";
        private long startAt;
        private long endAt;
        private boolean online;
        private long priceFrom;
        private boolean free;
        private Availability availability = Availability.AVAILABLE;
        private EventCategory category = EventCategory.OTHER;
        private float rating;
        private int reviewCount;
        private Integer aiScore;
        private String aiReason;
        private final List<String> tags = new ArrayList<>();
        private boolean favorite;

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder organizer(String organizer) {
            this.organizer = organizer;
            return this;
        }

        public Builder venue(String venue) {
            this.venue = venue;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
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

        public Builder free(boolean free) {
            this.free = free;
            return this;
        }

        public Builder availability(Availability availability) {
            this.availability = availability;
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

        public Builder aiScore(Integer aiScore) {
            this.aiScore = aiScore;
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

        public Builder favorite(boolean favorite) {
            this.favorite = favorite;
            return this;
        }

        public EventUiModel build() {
            return new EventUiModel(this);
        }
    }
}
