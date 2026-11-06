package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Khám phá & tìm kiếm sự kiện (FR-U04 / FR-U05). */
public interface EventApi {

    @GET("events")
    Call<ResponseWrapper.Envelope<List<DiscoveryDtos.EventCard>>> search(
            @Query("q") String q,
            @Query("categoryIds") List<Long> categoryIds,
            @Query("dateFrom") String dateFrom,
            @Query("dateTo") String dateTo,
            @Query("city") String city,
            @Query("format") String format,
            @Query("priceType") String priceType,
            @Query("priceMin") Long priceMin,
            @Query("priceMax") Long priceMax,
            @Query("availableOnly") boolean availableOnly,
            @Query("includePast") boolean includePast,
            @Query("sort") String sort,
            @Query("page") int page,
            @Query("size") int size);

    @GET("events/{id}")
    Call<ResponseWrapper.Envelope<DiscoveryDtos.EventDetail>> detail(@Path("id") long id);

    @GET("events/cities")
    Call<ResponseWrapper.Envelope<List<String>>> cities();
}
