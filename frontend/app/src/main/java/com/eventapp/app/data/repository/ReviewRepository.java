package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.data.remote.api.ModerationApi;
import com.eventapp.app.data.remote.api.ReviewApi;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;
import com.eventapp.app.data.remote.dto.review.ReviewDtos;
import com.google.gson.JsonElement;

import java.util.List;

/** Đánh giá công khai (FR-U20) + kiểm duyệt (FR-A14 / FR-A07). */
public class ReviewRepository {

    private final ReviewApi api;
    private final ModerationApi moderationApi;

    public ReviewRepository(@NonNull ReviewApi api, @NonNull ModerationApi moderationApi) {
        this.api = api;
        this.moderationApi = moderationApi;
    }

    public void list(long eventId, @NonNull String sort, int page, int size,
                     @NonNull ApiCallback<Paged<ReviewDtos.Review>> callback) {
        ApiCallback.enqueue(api.list(eventId, sort, page, size), paged(callback));
    }

    public void summary(long eventId,
                        @NonNull ApiCallback<DiscoveryDtos.ReviewSummary> callback) {
        ApiCallback.enqueue(api.summary(eventId), callback);
    }

    public void create(long eventId, @NonNull String idempotencyKey,
                       @NonNull ReviewDtos.CreateRequest request,
                       @NonNull ApiCallback<ReviewDtos.Mutation> callback) {
        ApiCallback.enqueue(api.create(eventId, idempotencyKey, request), callback);
    }

    public void update(long eventId, @NonNull ReviewDtos.UpdateRequest request,
                       @NonNull ApiCallback<ReviewDtos.Mutation> callback) {
        ApiCallback.enqueue(api.update(eventId, request), callback);
    }

    public void mine(int page, int size,
                     @NonNull ApiCallback<Paged<ReviewDtos.MyReview>> callback) {
        ApiCallback.enqueue(api.mine(page, size), paged(callback));
    }

    public void report(long reviewId, @NonNull String reasonCode, String note,
                       @NonNull ApiCallback<ReviewDtos.ReportResult> callback) {
        ApiCallback.enqueue(api.report(reviewId, ReviewDtos.ReportRequest.of(reasonCode, note)),
                callback);
    }

    // ------------------------------------------------------------------ admin

    public void adminReviews(String status, Long eventId, Integer rating, int page, int size,
                             @NonNull ApiCallback<Paged<ReviewDtos.AdminReview>> callback) {
        ApiCallback.enqueue(moderationApi.reviews(status, eventId, rating, page, size),
                paged(callback));
    }

    public void approve(long reviewId, @NonNull ReviewDtos.ModerationAction action,
                        @NonNull ApiCallback<ReviewDtos.AdminReview> callback) {
        ApiCallback.enqueue(moderationApi.approve(reviewId, action), callback);
    }

    public void hide(long reviewId, @NonNull ReviewDtos.ModerationAction action,
                     @NonNull ApiCallback<ReviewDtos.AdminReview> callback) {
        ApiCallback.enqueue(moderationApi.hide(reviewId, action), callback);
    }

    public void restore(long reviewId, @NonNull ReviewDtos.ModerationAction action,
                        @NonNull ApiCallback<ReviewDtos.AdminReview> callback) {
        ApiCallback.enqueue(moderationApi.restore(reviewId, action), callback);
    }

    public void flags(@NonNull String source, int page, int size,
                      @NonNull ApiCallback<Paged<ReviewDtos.ModerationFlag>> callback) {
        ApiCallback.enqueue(moderationApi.flags(source, page, size), paged(callback));
    }

    public void confirmFlag(@NonNull String flagId, @NonNull ReviewDtos.ModerationAction action,
                            @NonNull ApiCallback<ReviewDtos.AdminReview> callback) {
        ApiCallback.enqueue(moderationApi.confirmFlag(flagId, action), callback);
    }

    public void dismissFlag(@NonNull String flagId, @NonNull ReviewDtos.ModerationAction action,
                            @NonNull ApiCallback<ReviewDtos.AdminReview> callback) {
        ApiCallback.enqueue(moderationApi.dismissFlag(flagId, action), callback);
    }

    public void rescan(Integer sinceDays,
                       @NonNull ApiCallback<ReviewDtos.RescanResult> callback) {
        ApiCallback.enqueue(moderationApi.rescan(new ReviewDtos.RescanRequest(sinceDays)), callback);
    }

    @NonNull
    private static <T> ApiCallback<List<T>> paged(
            @NonNull ApiCallback<Paged<T>> callback) {
        return new ApiCallback.Delegate<List<T>>(callback) {
            @Override
            public void onSuccess(List<T> data, JsonElement meta) {
                callback.onSuccess(new Paged<>(data, meta), meta);
            }

            @Override
            public void onError(@NonNull com.eventapp.app.core.network.ApiException error) {
                callback.onError(error);
            }
        };
    }
}
