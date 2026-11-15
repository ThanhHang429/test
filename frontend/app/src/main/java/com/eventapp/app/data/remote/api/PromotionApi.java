package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.promotion.PromotionDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * API Quản lý và áp dụng mã giảm giá (FR-O18).
 */
public interface PromotionApi {

    @POST("promotions/apply")
    Call<ResponseWrapper.Envelope<PromotionDtos.CouponResponse>> applyCoupon(
            @Body PromotionDtos.ApplyCouponRequest request
    );

    @GET("organizer/events/{eventId}/promotions")
    Call<ResponseWrapper.Envelope<List<PromotionDtos.OrganizerCoupon>>> getEventCoupons(
            @Path("eventId") long eventId
    );

    @POST("organizer/events/{eventId}/promotions")
    Call<ResponseWrapper.Envelope<PromotionDtos.OrganizerCoupon>> createCoupon(
            @Path("eventId") long eventId,
            @Body PromotionDtos.CreateCouponRequest request
    );

    @PATCH("organizer/promotions/{couponId}/status")
    Call<ResponseWrapper.Envelope<PromotionDtos.OrganizerCoupon>> updateCouponStatus(
            @Path("couponId") long couponId,
            @Body PromotionDtos.UpdateCouponStatusRequest request
    );
}
