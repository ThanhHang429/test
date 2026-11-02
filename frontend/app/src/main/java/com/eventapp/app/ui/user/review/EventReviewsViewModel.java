package com.eventapp.app.ui.user.review;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;
import com.eventapp.app.data.remote.dto.review.ReviewDtos;
import com.eventapp.app.data.repository.ReviewRepository;
import com.eventapp.app.ui.mapper.ReviewMapper;
import com.eventapp.app.ui.model.ReviewUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * FR-U20 — danh sách đánh giá công khai của một sự kiện.
 *
 * <p>Sắp xếp và phân trang do backend quyết định ({@code GET /events/{id}/reviews}) để đánh giá
 * của chính người dùng luôn được ghim đầu danh sách (U20-R03). Phần tổng hợp (điểm trung bình,
 * phân bố) lấy từ {@code /reviews/summary}.</p>
 */
public class EventReviewsViewModel extends ViewModel {

    public enum Sort {
        NEWEST,
        RATING_HIGH,
        RATING_LOW
    }

    public static class Summary {
        public final float average;
        public final int total;
        public final int[] distribution;
        public final boolean canWrite;
        public final boolean hasMine;

        Summary(float average, int total, int[] distribution, boolean canWrite,
                boolean hasMine) {
            this.average = average;
            this.total = total;
            this.distribution = distribution;
            this.canWrite = canWrite;
            this.hasMine = hasMine;
        }
    }

    private static final int PAGE_SIZE = 10;

    private final ReviewRepository repository = ServiceLocator.get(ReviewRepository.class);

    private final MutableLiveData<List<ReviewUiModel>> reviews = new MutableLiveData<>();
    private final MutableLiveData<Summary> summary = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(false);

    private final List<ReviewUiModel> all = new ArrayList<>();

    private long eventId = -1L;
    private String eventTitle = "";
    private boolean canWrite = true;
    private boolean hasMine;
    private Sort sort = Sort.NEWEST;
    private int page;
    private boolean loadingPage;
    private float summaryAverage;
    private int summaryTotal;
    private int[] summaryDistribution = new int[5];

    public LiveData<List<ReviewUiModel>> getReviews() {
        return reviews;
    }

    public LiveData<Summary> getSummary() {
        return summary;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<Boolean> getHasMore() {
        return hasMore;
    }

    public Sort getSort() {
        return sort;
    }

    public void load(long eventId, String eventTitle, boolean canWrite, boolean hasMine) {
        this.eventId = eventId;
        this.eventTitle = eventTitle == null ? "" : eventTitle;
        this.canWrite = canWrite;
        this.hasMine = hasMine;
        loadSummary();
        loadPage(0);
    }

    public void refresh() {
        loadSummary();
        loadPage(0);
    }

    public void retry() {
        error.setValue(false);
        refresh();
    }

    public void setSort(Sort sort) {
        Sort next = sort == null ? Sort.NEWEST : sort;
        if (next == this.sort) {
            return;
        }
        this.sort = next;
        loadPage(0);
    }

    public void loadMore() {
        if (!Boolean.TRUE.equals(hasMore.getValue()) || loadingPage) {
            return;
        }
        loadPage(page + 1);
    }

    private void loadPage(int target) {
        if (eventId <= 0L || loadingPage) {
            return;
        }
        loadingPage = true;
        boolean firstPage = target == 0;
        if (firstPage) {
            loading.setValue(true);
            error.setValue(false);
        }
        repository.list(eventId, sortParam(), target, PAGE_SIZE,
                new ApiCallback<Paged<ReviewDtos.Review>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<ReviewDtos.Review> data,
                                          @Nullable JsonElement meta) {
                        page = target;
                        if (firstPage) {
                            all.clear();
                        }
                        if (data != null && data.items != null) {
                            for (ReviewDtos.Review dto : data.items) {
                                ReviewUiModel mapped = ReviewMapper.fromPublic(dto);
                                all.add(mapped);
                                if (mapped.mine) {
                                    hasMine = true;
                                }
                            }
                        }
                        reviews.setValue(new ArrayList<>(all));
                        hasMore.setValue(data != null && data.hasNext());
                        if (firstPage && summaryTotal <= 0 && data != null) {
                            summaryTotal = (int) Math.min(Integer.MAX_VALUE, data.totalItems());
                        }
                        loading.setValue(false);
                        publishSummary();
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        loading.setValue(false);
                        if (firstPage) {
                            error.setValue(true);
                        }
                        publishSummary();
                    }

                    @Override
                    public void onFinish() {
                        loadingPage = false;
                    }
                });
    }

    private void loadSummary() {
        if (eventId <= 0L) {
            return;
        }
        repository.summary(eventId, new ApiCallback<DiscoveryDtos.ReviewSummary>() {
            @Override
            public void onSuccess(@Nullable DiscoveryDtos.ReviewSummary data,
                                  @Nullable JsonElement meta) {
                if (data == null) {
                    return;
                }
                summaryAverage = (float) data.averageRating;
                summaryTotal = (int) Math.min(Integer.MAX_VALUE, data.totalReviews);
                summaryDistribution = distribution(data);
                publishSummary();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                // Tổng hợp lỗi không chặn danh sách đánh giá.
            }
        });
    }

    private void publishSummary() {
        summary.setValue(new Summary(summaryAverage, summaryTotal, summaryDistribution,
                canWrite, hasMine));
    }

    @NonNull
    private static int[] distribution(@Nullable DiscoveryDtos.ReviewSummary data) {
        int[] values = new int[5];
        if (data != null && data.distribution != null) {
            for (int star = 1; star <= 5; star++) {
                Long count = data.distribution.get(String.valueOf(star));
                values[star - 1] = count == null ? 0 : count.intValue();
            }
        }
        return values;
    }

    @NonNull
    private String sortParam() {
        switch (sort) {
            case RATING_HIGH:
                return "RATING_DESC";
            case RATING_LOW:
                return "RATING_ASC";
            case NEWEST:
            default:
                return "NEWEST";
        }
    }
}
