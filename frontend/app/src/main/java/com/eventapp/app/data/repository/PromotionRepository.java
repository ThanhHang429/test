package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.data.remote.api.PromotionApi;
import com.eventapp.app.data.remote.dto.promotion.PromotionDtos;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository xử lý mã giảm giá (FR-O18).
 */
public class PromotionRepository {

    private final PromotionApi api;

    public PromotionRepository(PromotionApi api) {
        this.api = api;
    }

    public void applyCoupon(String code, long eventId, BigDecimal subtotal,
                            ApiCallback<PromotionDtos.CouponResponse> callback) {
        PromotionDtos.ApplyCouponRequest req = new PromotionDtos.ApplyCouponRequest(code, eventId, subtotal);
        ApiCallback.enqueue(api.applyCoupon(req), callback);
    }

    public void getEventCoupons(long eventId,
                                @NonNull ApiCallback<List<PromotionDtos.OrganizerCoupon>> callback) {
        ApiCallback.enqueue(api.getEventCoupons(eventId), callback);
    }

    public void createCoupon(long eventId,
                             @NonNull PromotionDtos.CreateCouponRequest request,
                             @NonNull ApiCallback<PromotionDtos.OrganizerCoupon> callback) {
        ApiCallback.enqueue(api.createCoupon(eventId, request), callback);
    }

    public void updateCouponStatus(long couponId,
                                   @NonNull String status,
                                   @NonNull ApiCallback<PromotionDtos.OrganizerCoupon> callback) {
        ApiCallback.enqueue(
                api.updateCouponStatus(couponId, new PromotionDtos.UpdateCouponStatusRequest(status)),
                callback
        );
    }
}
