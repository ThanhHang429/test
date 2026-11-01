package com.eventapp.app.ui.user.review;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.fake.SampleReviewData;
import com.eventapp.app.ui.model.ReviewUiModel;

import java.util.ArrayList;
import java.util.List;

/**
 * FR-U20 "Đánh giá của tôi": danh sách đánh giá của chính người dùng, lọc theo trạng thái và
 * đếm theo từng trạng thái. Dữ liệu lấy từ {@link SampleReviewData} cho tới khi nối ReviewApi.
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

    private static final long LOAD_DELAY_MS = 320L;

    private final MutableLiveData<List<ReviewUiModel>> allReviews =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ReviewUiModel>> reviews =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Filter> filter = new MutableLiveData<>(Filter.ALL);
    private final MutableLiveData<Counts> counts = new MutableLiveData<>(new Counts(0, 0, 0, 0));
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);

    private final Handler handler = new Handler(Looper.getMainLooper());

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
        handler.postDelayed(() -> {
            allReviews.setValue(SampleReviewData.myReviews());
            counts.setValue(buildCounts(allReviews.getValue()));
            applyFilter();
            loading.setValue(false);
        }, LOAD_DELAY_MS);
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

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
