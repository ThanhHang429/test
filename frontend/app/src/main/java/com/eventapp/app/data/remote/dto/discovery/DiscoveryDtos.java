package com.eventapp.app.data.remote.dto.discovery;

import java.util.ArrayList;
import java.util.List;

/** DTO khám phá sự kiện (FR-U04 / FR-U05). */
public final class DiscoveryDtos {

    private DiscoveryDtos() {
    }

    public static class EventCard {
        public long id;
        public String code;
        public String title;
        public String slug;
        public String shortDescription;
        public String coverUrl;
        public String startAt;
        public String endAt;
        public String timezone;
        public String city;
        public String venueName;
        public String eventFormat;
        public List<Long> categoryIds = new ArrayList<>();
        public List<String> categoryNames = new ArrayList<>();
        public List<String> tags = new ArrayList<>();
        public Double minPrice;
        public Double maxPrice;
        public boolean isFree;
        public Boolean available;
        public boolean availabilityUnknown;
        public String saleStatus;
        public String statusLabel;
        public Boolean isFavorite;
        public String reason;
        public String reasonSource;
    }

    public static class TicketType {
        public long id;
        public String name;
        public String description;
        public String kind;
        public Double price;
        public String currency;
        public String saleStatus;
        public String saleStartAt;
        public String saleEndAt;
        public Integer remaining;
        public String remainingLabel;
        public int minPerOrder;
        public int maxPerUser;
    }

    public static class OrganizerBrief {
        public long id;
        public String publicName;
        public String description;
        public String logoUrl;
        public long organizedEventCount;
    }

    public static class ReviewBrief {
        public long id;
        public String authorName;
        public int rating;
        public String text;
        public String createdAt;
    }

    public static class ReviewSummary {
        public double averageRating;
        public long totalReviews;
        public java.util.Map<String, Long> distribution = new java.util.LinkedHashMap<>();
        public List<ReviewBrief> highlights = new ArrayList<>();
    }

    public static class EventDetail {
        public long id;
        public String code;
        public String title;
        public String slug;
        public String shortDescription;
        public String description;
        public String eventFormat;
        public String lifecycleStatus;
        public String startAt;
        public String endAt;
        public String timezone;
        public String registrationStartAt;
        public String registrationEndAt;
        public String venueName;
        public String addressLine;
        public String city;
        public String province;
        public String onlineUrl;
        public Integer capacityLimit;
        public String participationConditions;
        public String cancellationPolicy;
        public String coverUrl;
        public List<String> mediaUrls = new ArrayList<>();
        public List<Long> categoryIds = new ArrayList<>();
        public List<String> categoryNames = new ArrayList<>();
        public List<String> tags = new ArrayList<>();
        public OrganizerBrief organizer;
        public List<TicketType> ticketTypes = new ArrayList<>();
        public Double minPrice;
        public boolean isFree;
        public ReviewSummary reviewSummary;
        public boolean isFavorite;
        public String registrationStatus;
        public boolean canReview;
        public List<String> allowedActions = new ArrayList<>();
        public String disabledReason;
        public String serverTime;
        public String fetchedAt;
    }
}
