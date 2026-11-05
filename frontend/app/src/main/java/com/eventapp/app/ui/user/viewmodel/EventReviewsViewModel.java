package com.eventapp.app.ui.user.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;
import com.eventapp.app.data.remote.dto.review.ReviewDtos;
import com.eventapp.app.data.repository.ReviewRepository;
import com.eventapp.app.ui.mapper.ReviewMapper;
import com.eventapp.app.ui.model.ReviewSummaryUiModel;
import com.eventapp.app.ui.model.ReviewUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Trạng thái trang đánh giá sự kiện: tổng hợp, phân trang, viết/sửa và báo cáo (FR-U20). */
public class EventReviewsViewModel extends ViewModel {

    private static final int PAGE_SIZE = 10;

    private final ReviewRepository repository = ServiceLocator.get(ReviewRepository.class);

    private final MutableLiveData<ReviewSummaryUiModel> summary = new MutableLiveData<>();
    private final MutableLiveData<List<ReviewUiModel>> reviews =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasNext = new MutableLiveData<>(false);
    private final MutableLiveData<ReviewUiModel> mine = new MutableLiveData<>();
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();
    private final MutableLiveData<String> submitNotice = new MutableLiveData<>();
    private final MutableLiveData<ApiException> submitError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> reportSent = new MutableLiveData<>();
    private final MutableLiveData<ApiException> reportError = new MutableLiveData<>();

    private long eventId;
    private String sort = "NEWEST";
    private int page;
    private ReviewDtos.MyReview mineValue;

    public LiveData<ReviewSummaryUiModel> getSummary() {
        return summary;
    }

    public LiveData<List<ReviewUiModel>> getReviews() {
        return reviews;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getHasNext() {
        return hasNext;
    }

    public LiveData<ReviewUiModel> getMine() {
        return mine;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public LiveData<String> getSubmitNotice() {
        return submitNotice;
    }

    public LiveData<ApiException> getSubmitError() {
        return submitError;
    }

    public LiveData<Boolean> getReportSent() {
        return reportSent;
    }

    public LiveData<ApiException> getReportError() {
        return reportError;
    }

    public void init(long eventId) {
        this.eventId = eventId;
    }

    public void refreshDetails() {
        loadSummary();
        loadMine();
    }

    public void selectSort(@NonNull String value) {
        sort = value;
        loadReviews(true);
    }

    public void loadMore() {
        loadReviews(false);
    }

    public void reload() {
        refreshDetails();
        loadReviews(true);
    }

    public void submit(int rating, @NonNull String text) {
        ReviewDtos.MyReview editing = mineValue;
        ApiCallback<ReviewDtos.Mutation> callback = new ApiCallback<ReviewDtos.Mutation>() {
            @Override
            public void onSuccess(@Nullable ReviewDtos.Mutation data, @Nullable JsonElement meta) {
                submitNotice.setValue(data == null
                        ? null : (data.notice == null ? "" : data.notice));
                reload();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                submitError.setValue(exception);
            }
        };

        if (editing != null) {
            ReviewDtos.UpdateRequest request = new ReviewDtos.UpdateRequest();
            request.rating = rating;
            request.text = text;
            request.version = DateTimeUtils.toMillis(editing.updatedAt);
            repository.update(eventId, request, callback);
        } else {
            ReviewDtos.CreateRequest request = new ReviewDtos.CreateRequest();
            request.rating = rating;
            request.text = text;
            repository.create(eventId, UUID.randomUUID().toString(), request, callback);
        }
    }

    public void report(long reviewId, @NonNull String reasonCode) {
        repository.report(reviewId, reasonCode, null,
                new ApiCallback<ReviewDtos.ReportResult>() {
                    @Override
                    public void onSuccess(@Nullable ReviewDtos.ReportResult data,
                                          @Nullable JsonElement meta) {
                        reportSent.setValue(Boolean.TRUE);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        reportError.setValue(exception);
                    }
                });
    }

    public void consumeError() {
        error.setValue(null);
    }

    public void consumeSubmitNotice() {
        submitNotice.setValue(null);
    }

    public void consumeSubmitError() {
        submitError.setValue(null);
    }

    public void consumeReportSent() {
        reportSent.setValue(null);
    }

    public void consumeReportError() {
        reportError.setValue(null);
    }

    private void loadSummary() {
        if (eventId <= 0) {
            return;
        }
        repository.summary(eventId, new ApiCallback<DiscoveryDtos.ReviewSummary>() {
            @Override
            public void onSuccess(@Nullable DiscoveryDtos.ReviewSummary data,
                                  @Nullable JsonElement meta) {
                if (data != null) {
                    summary.setValue(ReviewMapper.fromSummary(data));
                }
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                // Khối tổng hợp lỗi không chặn danh sách đánh giá.
            }
        });
    }

    private void loadMine() {
        if (eventId <= 0) {
            return;
        }
        repository.mine(0, 50, new ApiCallback<Paged<ReviewDtos.MyReview>>() {
            @Override
            public void onSuccess(@Nullable Paged<ReviewDtos.MyReview> data,
                                  @Nullable JsonElement meta) {
                mineValue = null;
                if (data != null) {
                    for (ReviewDtos.MyReview dto : data.items) {
                        if (dto.eventId == eventId) {
                            mineValue = dto;
                            break;
                        }
                    }
                }
                mine.setValue(mineValue == null ? null : ReviewMapper.fromMine(mineValue));
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                mine.setValue(mineValue == null ? null : ReviewMapper.fromMine(mineValue));
            }
        });
    }

    private void loadReviews(boolean reset) {
        if (eventId <= 0 || Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        loading.setValue(true);
        final int requestPage = reset ? 0 : page;

        repository.list(eventId, sort, requestPage, PAGE_SIZE,
                new ApiCallback<Paged<ReviewDtos.Review>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<ReviewDtos.Review> data,
                                          @Nullable JsonElement meta) {
                        List<ReviewUiModel> result = reset
                                ? new ArrayList<>() : copiesOf(reviews.getValue());
                        if (data != null) {
                            for (ReviewDtos.Review dto : data.items) {
                                result.add(ReviewMapper.fromPublic(dto));
                            }
                            hasNext.setValue(data.hasNext());
                        } else {
                            hasNext.setValue(false);
                        }
                        page = requestPage + 1;
                        reviews.setValue(result);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        error.setValue(exception);
                    }

                    @Override
                    public void onFinish() {
                        loading.setValue(false);
                    }
                });
    }

    @NonNull
    private static List<ReviewUiModel> copiesOf(@Nullable List<ReviewUiModel> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
