package com.eventapp.app.data.remote.dto.recommendation;

import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;

import java.util.ArrayList;
import java.util.List;

/** Gợi ý AI (FR-U07). */
public final class RecommendationDtos {

    private RecommendationDtos() {
    }

    public static class Item {
        public DiscoveryDtos.EventCard event;
        public String reason;
        public String reasonSource;
    }

    public static class Meta {
        public String title;
        public String generatedAt;
        public boolean isFallback;
        public boolean personalized;
        public String engineVersion;
        public String modelCode;
        public Long aiRequestId;
        public String correlationId;
    }

    public static class ListResult {
        public List<Item> items = new ArrayList<>();
        public Meta meta;
    }

    public static class PreviewRequest {
        public List<Long> categoryIds = new ArrayList<>();
        public String city;
        public String priceMode;
        public Long maxBudgetVnd;
        public String eventFormat;
        public List<String> preferredDays = new ArrayList<>();
        public Integer limit;
    }

    public static class DismissResult {
        public long eventId;
        public boolean dismissed;
    }
}
