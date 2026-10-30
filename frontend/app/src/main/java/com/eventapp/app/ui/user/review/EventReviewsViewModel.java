package com.eventapp.app.ui.user.review;


import com.eventapp.app.ui.fake.SampleReviewData;
import com.eventapp.app.ui.model.ReviewUiModel;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Sort and pagination state for the public event review list (FR-U20). */
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

    private static final int PAGE_SIZE = 4;

    private final MutableLiveData<List<ReviewUiModel>> reviews = new MutableLiveData<>();
    private final MutableLiveData<Summary> summary = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(false);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long eventId;
    private String eventTitle;
    private boolean canWrite;
    private boolean hasMine;
    private ReviewUiModel myReview;
    private List<ReviewUiModel> all = new ArrayList<>();
    private List<ReviewUiModel> sorted = new ArrayList<>();
    private Sort sort = Sort.NEWEST;
    private int page;

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
        loadInternal();
    }

    public void refresh() {
        loadInternal();
    }

    public void retry() {
        error.setValue(false);
        loadInternal();
    }

    public void setSort(Sort sort) {
        this.sort = sort == null ? Sort.NEWEST : sort;
        sortAll();
        page = 0;
        reviews.setValue(pageItems());
        hasMore.setValue(sorted.size() > PAGE_SIZE);
    }

    public void loadMore() {
        if (!Boolean.TRUE.equals(hasMore.getValue())) {
            return;
        }
        page++;
        reviews.setValue(pageItems());
        hasMore.setValue(Math.min((page + 1) * PAGE_SIZE, sorted.size()) < sorted.size());
    }

    private void loadInternal() {
        loading.setValue(true);
        error.setValue(false);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            all = SampleReviewData.forEvent(eventId, eventTitle);
            myReview = hasMine
                    ? SampleReviewData.mine(eventId, eventTitle, ReviewUiModel.Status.VISIBLE)
                    : null;
            sortAll();
            page = 0;
            reviews.setValue(pageItems());
            hasMore.setValue(sorted.size() > PAGE_SIZE);
            float average = SampleReviewData.average(all);
            summary.setValue(new Summary(average, all.size(),
                    SampleReviewData.distribution(all), canWrite, hasMine));
            loading.setValue(false);
        }, 340L);
    }

    private void sortAll() {
        sorted = new ArrayList<>();
        if (myReview != null) {
            sorted.add(myReview);
        }
        List<ReviewUiModel> copy = new ArrayList<>(all);
        switch (sort) {
            case RATING_HIGH:
                Collections.sort(copy, (left, right) -> Float.compare(right.rating, left.rating));
                break;
            case RATING_LOW:
                Collections.sort(copy, (left, right) -> Float.compare(left.rating, right.rating));
                break;
            case NEWEST:
            default:
                Collections.sort(copy, (left, right) ->
                        Long.compare(right.createdAt, left.createdAt));
                break;
        }
        sorted.addAll(copy);
    }

    private List<ReviewUiModel> pageItems() {
        int to = Math.min((page + 1) * PAGE_SIZE, sorted.size());
        return new ArrayList<>(sorted.subList(0, to));
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
