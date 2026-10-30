package com.eventapp.app.ui.user.home;


import com.eventapp.app.ui.fake.SampleEventData;
import com.eventapp.app.ui.model.EventUiModel;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Home data for FR-U04. Until the event API exists, the view model exposes deterministic sample
 * data so product states can be reviewed on a device.
 */
public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<EventUiModel>> aiEvents = new MutableLiveData<>();
    private final MutableLiveData<List<EventUiModel>> featuredEvents = new MutableLiveData<>();
    private final MutableLiveData<List<EventUiModel>> upcomingEvents = new MutableLiveData<>();
    private final MutableLiveData<List<EventUiModel>> trendingEvents = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);

    private final Handler handler = new Handler(Looper.getMainLooper());

    public HomeViewModel() {
        load();
    }

    public LiveData<List<EventUiModel>> getAiEvents() {
        return aiEvents;
    }

    public LiveData<List<EventUiModel>> getFeaturedEvents() {
        return featuredEvents;
    }

    public LiveData<List<EventUiModel>> getUpcomingEvents() {
        return upcomingEvents;
    }

    /** Sự kiện bán chạy cho mục "Thịnh hành tuần này" (sắp theo số lượt đánh giá). */
    public LiveData<List<EventUiModel>> getTrendingEvents() {
        return trendingEvents;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void refresh() {
        load();
    }

    private void load() {
        loading.setValue(true);
        handler.postDelayed(() -> {
            List<EventUiModel> all = SampleEventData.createEvents();

            List<EventUiModel> ai = new ArrayList<>();
            for (EventUiModel event : all) {
                if (event.aiScore != null) {
                    ai.add(event);
                }
            }
            Collections.sort(ai, (left, right) ->
                    Integer.compare(right.aiScore, left.aiScore));
            if (ai.size() > 6) {
                ai = new ArrayList<>(ai.subList(0, 6));
            }

            List<EventUiModel> featured = new ArrayList<>(all);
            Collections.sort(featured, (left, right) -> {
                int ratingCompare = Float.compare(right.rating, left.rating);
                if (ratingCompare != 0) {
                    return ratingCompare;
                }
                return Integer.compare(right.reviewCount, left.reviewCount);
            });
            if (featured.size() > 6) {
                featured = new ArrayList<>(featured.subList(0, 6));
            }

            List<EventUiModel> upcoming = new ArrayList<>(all);
            Collections.sort(upcoming, Comparator.comparingLong(event -> event.startAt));
            if (upcoming.size() > 6) {
                upcoming = new ArrayList<>(upcoming.subList(0, 6));
            }

            List<EventUiModel> trending = new ArrayList<>(all);
            Collections.sort(trending, (left, right) ->
                    Integer.compare(right.reviewCount, left.reviewCount));
            if (trending.size() > 4) {
                trending = new ArrayList<>(trending.subList(0, 4));
            }

            aiEvents.setValue(ai);
            featuredEvents.setValue(featured);
            upcomingEvents.setValue(upcoming);
            trendingEvents.setValue(trending);
            loading.setValue(false);
        }, 320L);
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
