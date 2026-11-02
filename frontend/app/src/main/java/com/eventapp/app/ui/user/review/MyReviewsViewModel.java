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
import com.eventapp.app.data.remote.dto.review.ReviewDtos;
import com.eventapp.app.data.repository.ReviewRepository;
import com.eventapp.app.ui.mapper.ReviewMapper;
import com.eventapp.app.ui.model.ReviewUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * FR-U20 "Đánh giá của tôi" — mọi trạng thái (VISIBLE/PENDING/HIDDEN/REMOVED) lấy từ
 * {@code GET /me/reviews} kèm số lần sửa và hạn sửa.
 */
public class MyReviewsViewModel extends ViewModel {

    public enum Filter {
        ALL,
        VISIBLE,
        HELD,
        HIDDEN
    }

    /** Số lượng theo trạng thái, dùng cho nhãn của chip lọc. */
    public static class Counts {
        public final int all;
        public final int visible;
        public final int held;
        public final int hidden;

        Counts(int all, int visible, int held, int hidden) {
            this.all = all;
            this.visible = visible;
            this.held = held;
            this.hidden = hidden;
        }
    }

    private static final int PAGE_SIZE = 50;

    private final ReviewRepository repository = ServiceLocator.get(ReviewRepository.class);

    private final MutableLiveData<List<ReviewUiModel>> allReviews =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ReviewUiModel>> reviews =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Filter> filter = new MutableLiveData<>(Filter.ALL);
    private final MutableLiveData<Counts> counts = new MutableLiveData<>(new Counts(0, 0, 0, 0));
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);

    public MyReviewsViewModel() {
        load();
    }

    public LiveData<List<ReviewUiModel>> getReviews() {
        return reviews;
    }

    public LiveData<Filter> getFilter() {
        return filter;
    }

    public LiveData<Counts> getCounts() {
        return counts;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public void setFilter(Filter next) {
        Filter value = next == null ? Filter.ALL : next;
        if (value == filter.getValue()) {
            return;
        }
        filter.setValue(value);
        applyFilter();
    }

    public void retry() {
        load();
    }

    private void load() {
        loading.setValue(true);
        error.setValue(false);
        repository.mine(0, PAGE_SIZE, new ApiCallback<Paged<ReviewDtos.MyReview>>() {
            @Override
            public void onSuccess(@Nullable Paged<ReviewDtos.MyReview> data,
                                  @Nullable JsonElement meta) {
                List<ReviewUiModel> mapped = new ArrayList<>();
                if (data != null) {
                    for (ReviewDtos.MyReview dto : data.items) {
                        mapped.add(ReviewMapper.fromMine(dto));
                    }
                }
                allReviews.setValue(mapped);
                counts.setValue(buildCounts(mapped));
                applyFilter();
                loading.setValue(false);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                loading.setValue(false);
                error.setValue(true);
            }
        });
    }

    private void applyFilter() {
        List<ReviewUiModel> source = allReviews.getValue();
        List<ReviewUiModel> result = new ArrayList<>();
        if (source != null) {
            Filter current = filter.getValue() == null ? Filter.ALL : filter.getValue();
            for (ReviewUiModel review : source) {
                if (matches(review, current)) {
                    result.add(review);
                }
            }
        }
        reviews.setValue(result);
    }

    private boolean matches(ReviewUiModel review, Filter current) {
        switch (current) {
            case VISIBLE:
                return review.status == ReviewUiModel.Status.VISIBLE;
            case HELD:
                return review.status == ReviewUiModel.Status.HELD;
            case HIDDEN:
                return review.status == ReviewUiModel.Status.HIDDEN;
            case ALL:
            default:
                return true;
        }
    }

    private Counts buildCounts(List<ReviewUiModel> source) {
        int visible = 0;
        int held = 0;
        int hidden = 0;
        if (source != null) {
            for (ReviewUiModel review : source) {
                switch (review.status) {
                    case HELD:
                        held++;
                        break;
                    case HIDDEN:
                        hidden++;
                        break;
                    case VISIBLE:
                    default:
                        visible++;
                        break;
                }
            }
        }
        return new Counts(visible + held + hidden, visible, held, hidden);
    }
}
