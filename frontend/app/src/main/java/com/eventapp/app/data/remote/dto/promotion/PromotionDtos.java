package com.eventapp.app.data.remote.dto.promotion;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO Quản lý và áp dụng mã giảm giá (FR-O18).
 */
public class PromotionDtos {

    public static class ApplyCouponRequest implements Serializable {
        @SerializedName("code")
        public String code;

        @SerializedName("eventId")
        public long eventId;

        @SerializedName("subtotalAmount")
        public BigDecimal subtotalAmount;

        public ApplyCouponRequest(String code, long eventId, BigDecimal subtotalAmount) {
            this.code = code;
            this.eventId = eventId;
            this.subtotalAmount = subtotalAmount;
        }
    }

    public static class CouponResponse implements Serializable {
        @SerializedName("couponId")
        public Long couponId;

        @SerializedName("code")
        public String code;

        @SerializedName("discountType")
        public String discountType; // PERCENT | FIXED_AMOUNT

        @SerializedName("discountValue")
        public BigDecimal discountValue;

        @SerializedName("discountAmount")
        public BigDecimal discountAmount;

        @SerializedName("finalAmount")
        public BigDecimal finalAmount;

        @SerializedName("description")
        public String description;
    }

    public static class OrganizerCoupon implements Serializable {
        @SerializedName("couponId")
        public long couponId;

        @SerializedName("eventId")
        public long eventId;

        @SerializedName("code")
        public String code;

        @SerializedName("description")
        public String description;

        @SerializedName("discountType")
        public String discountType; // PERCENT | FIXED_AMOUNT

        @SerializedName("discountValue")
        public BigDecimal discountValue;

        @SerializedName("minOrderAmount")
        public BigDecimal minOrderAmount;

        @SerializedName("maxDiscountAmount")
        public BigDecimal maxDiscountAmount;

        @SerializedName("usageLimit")
        public Integer usageLimit;

        @SerializedName("usedCount")
        public int usedCount;

        @SerializedName("status")
        public String status; // ACTIVE | PAUSED | EXPIRED

        @SerializedName("startsAt")
        public String startsAt;

        @SerializedName("endsAt")
        public String endsAt;

        @SerializedName("createdAt")
        public String createdAt;
    }

    public static class CreateCouponRequest implements Serializable {
        @SerializedName("code")
        public String code;

        @SerializedName("description")
        public String description;

        @SerializedName("discountType")
        public String discountType;

        @SerializedName("discountValue")
        public BigDecimal discountValue;

        @SerializedName("minOrderAmount")
        public BigDecimal minOrderAmount;

        @SerializedName("maxDiscountAmount")
        public BigDecimal maxDiscountAmount;

        @SerializedName("usageLimit")
        public Integer usageLimit;

        @SerializedName("startsAt")
        public String startsAt;

        @SerializedName("endsAt")
        public String endsAt;
    }

    public static class UpdateCouponStatusRequest implements Serializable {
        @SerializedName("status")
        public String status;

        public UpdateCouponStatusRequest(String status) {
            this.status = status;
        }
    }
}
