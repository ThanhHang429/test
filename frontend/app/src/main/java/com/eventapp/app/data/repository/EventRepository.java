package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.data.remote.api.EventApi;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;

import java.util.List;

/** Danh sách/tìm kiếm và chi tiết sự kiện công khai (FR-U04 / FR-U05). */
public class EventRepository {

    private final EventApi api;

    public EventRepository(@NonNull EventApi api) {
        this.api = api;
    }

    public void search(@NonNull SearchQuery query,
                       @NonNull ApiCallback<Paged<DiscoveryDtos.EventCard>> callback) {
        ApiCallback.enqueue(api.search(
                        emptyToNull(query.q),
                        query.categoryIds,
                        query.dateFrom,
                        query.dateTo,
                        emptyToNull(query.city),
                        query.format,
                        query.priceType,
                        query.priceMin,
                        query.priceMax,
                        query.availableOnly,
                        query.includePast,
                        query.sort,
                        query.page,
                        query.size),
                new ApiCallback.Delegate<List<DiscoveryDtos.EventCard>>(callback) {
                    @Override
                    public void onSuccess(List<DiscoveryDtos.EventCard> data, com.google.gson.JsonElement meta) {
                        callback.onSuccess(new Paged<>(data, meta), meta);
                    }

                    @Override
                    public void onError(@NonNull com.eventapp.app.core.network.ApiException error) {
                        callback.onError(error);
                    }
                });
    }

    public void detail(long id, @NonNull ApiCallback<DiscoveryDtos.EventDetail> callback) {
        ApiCallback.enqueue(api.detail(id), callback);
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /** Tham số tìm kiếm; mọi trường đều tuỳ chọn. */
    public static class SearchQuery {
        public String q;
        public List<Long> categoryIds;
        public String dateFrom;
        public String dateTo;
        public String city;
        public String format;
        public String priceType;
        public Long priceMin;
        public Long priceMax;
        public boolean availableOnly;
        public boolean includePast;
        public String sort = "START_TIME_ASC";
        public int page;
        public int size = 20;
    }
}
