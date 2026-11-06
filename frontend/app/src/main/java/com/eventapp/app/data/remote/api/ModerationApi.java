package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.review.ReviewDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Kiểm duyệt nội dung (FR-A14 / FR-A07). */
public interface ModerationApi {

    @GET("admin/reviews")
    Call<ResponseWrapper.Envelope<List<ReviewDtos.AdminReview>>> reviews(
            @Query("status") String status,
            @Query("eventId") Long eventId,
            @Query("rating") Integer rating,
            @Query("page") int page,
            @Query("size") int size);

    @POST("admin/reviews/{reviewId}/approve")
    Call<ResponseWrapper.Envelope<ReviewDtos.AdminReview>> approve(
            @Path("reviewId") long reviewId,
            @Body ReviewDtos.ModerationAction request);

    @POST("admin/reviews/{reviewId}/hide")
    Call<ResponseWrapper.Envelope<ReviewDtos.AdminReview>> hide(
            @Path("reviewId") long reviewId,
            @Body ReviewDtos.ModerationAction request);

    @POST("admin/reviews/{reviewId}/restore")
    Call<ResponseWrapper.Envelope<ReviewDtos.AdminReview>> restore(
            @Path("reviewId") long reviewId,
            @Body ReviewDtos.ModerationAction request);

    @GET("admin/moderation/flags")
    Call<ResponseWrapper.Envelope<List<ReviewDtos.ModerationFlag>>> flags(
            @Query("source") String source,
            @Query("page") int page,
            @Query("size") int size);

    @POST("admin/moderation/flags/{flagId}/confirm")
    Call<ResponseWrapper.Envelope<ReviewDtos.AdminReview>> confirmFlag(
            @Path("flagId") String flagId,
            @Body ReviewDtos.ModerationAction request);

    @POST("admin/moderation/flags/{flagId}/dismiss")
    Call<ResponseWrapper.Envelope<ReviewDtos.AdminReview>> dismissFlag(
            @Path("flagId") String flagId,
            @Body ReviewDtos.ModerationAction request);

    @POST("admin/moderation/rescan")
    Call<ResponseWrapper.Envelope<ReviewDtos.RescanResult>> rescan(
            @Body ReviewDtos.RescanRequest request);
}
