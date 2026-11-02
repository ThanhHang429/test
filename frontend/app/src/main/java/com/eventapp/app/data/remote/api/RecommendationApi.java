package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.recommendation.RecommendationDtos;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Gợi ý AI (FR-U07). */
public interface RecommendationApi {

    @GET("recommendations")
    Call<ResponseWrapper.Envelope<RecommendationDtos.ListResult>> forMe(
            @Query("limit") int limit,
            @Query("refresh") boolean refresh);

    @POST("recommendations/preview")
    Call<ResponseWrapper.Envelope<RecommendationDtos.ListResult>> preview(
            @Body RecommendationDtos.PreviewRequest request);

    @POST("recommendations/{eventId}/dismiss")
    Call<ResponseWrapper.Envelope<RecommendationDtos.DismissResult>> dismiss(
            @Path("eventId") long eventId);
}
