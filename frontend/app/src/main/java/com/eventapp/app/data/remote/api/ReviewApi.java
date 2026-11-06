package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;
import com.eventapp.app.data.remote.dto.review.ReviewDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Đánh giá (FR-U20). */
public interface ReviewApi {

    @GET("events/{eventId}/reviews")
    Call<ResponseWrapper.Envelope<List<ReviewDtos.Review>>> list(
            @Path("eventId") long eventId,
            @Query("sort") String sort,
            @Query("page") int page,
            @Query("size") int size);

    @GET("events/{eventId}/reviews/summary")
    Call<ResponseWrapper.Envelope<DiscoveryDtos.ReviewSummary>> summary(@Path("eventId") long eventId);

    @POST("events/{eventId}/reviews")
    Call<ResponseWrapper.Envelope<ReviewDtos.Mutation>> create(
            @Path("eventId") long eventId,
            @Header("Idempotency-Key") String idempotencyKey,
            @Body ReviewDtos.CreateRequest request);

    @PUT("events/{eventId}/reviews/me")
    Call<ResponseWrapper.Envelope<ReviewDtos.Mutation>> update(
            @Path("eventId") long eventId,
            @Body ReviewDtos.UpdateRequest request);

    @GET("me/reviews")
    Call<ResponseWrapper.Envelope<List<ReviewDtos.MyReview>>> mine(
            @Query("page") int page,
            @Query("size") int size);

    @POST("reviews/{reviewId}/report")
    Call<ResponseWrapper.Envelope<ReviewDtos.ReportResult>> report(
            @Path("reviewId") long reviewId,
            @Body ReviewDtos.ReportRequest request);
}
