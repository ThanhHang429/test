package com.eventapp.app.ui.user.home;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.enums.EventCategory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * FR-U07 state holder. Suggestions are independent from the event list: a failure here must not
 * block discovery, so error state is local to this screen.
 */
public class RecommendViewModel extends ViewModel {

    public static class Meta {
        public final long generatedAt;
        public final boolean personalized;
        public final boolean fallback;

        public Meta(long generatedAt, boolean personalized, boolean fallback) {
            this.generatedAt = generatedAt;
            this.personalized = personalized;
            this.fallback = fallback;
        }
    }

    private static final long REFRESH_INTERVAL_MS = 60_000L;
    private static final int MAX_PREFERENCE_CHIPS = 4;

    private final MutableLiveData<List<RecommendationUiModel>> items =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Meta> meta = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<List<EventCategory>> preferences =
            new MutableLiveData<>(new ArrayList<>());

    private final Handler handler = new Handler(Looper.getMainLooper());

    private long lastRefreshAt;
    private RecommendationUiModel dismissed;
    private int dismissedIndex = -1;

    public RecommendViewModel() {
        load();
    }

    public LiveData<List<RecommendationUiModel>> getItems() {
        return items;
    }

    public LiveData<Meta> getMeta() {
        return meta;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<List<EventCategory>> getPreferences() {
        return preferences;
    }

    public void load() {
        loading.setValue(true);
        error.setValue(false);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::bindSampleData, 320L);
    }

    /**
     * @return false when the one-minute refresh quota has not elapsed; the caller shows the
     *     cooldown message instead of calling the API again (U07-R06).
     */
    public boolean refresh() {
        long now = System.currentTimeMillis();
        if (lastRefreshAt > 0L && now - lastRefreshAt < REFRESH_INTERVAL_MS) {
            return false;
        }
        lastRefreshAt = now;
        load();
        return true;
    }

    public void dismiss(RecommendationUiModel item) {
        List<RecommendationUiModel> current = copyOf(items.getValue());
        int index = current.indexOf(item);
        if (index < 0) {
            return;
        }
        dismissed = current.remove(index);
        dismissedIndex = index;
        items.setValue(current);
    }

    public void undoDismiss() {
        if (dismissed == null) {
            return;
        }
        List<RecommendationUiModel> current = copyOf(items.getValue());
        int index = Math.max(0, Math.min(dismissedIndex, current.size()));
        current.add(index, dismissed);
        items.setValue(current);
        dismissed = null;
        dismissedIndex = -1;
    }

    private void bindSampleData() {
        List<RecommendationUiModel> values = SampleRecommendationData.create();
        items.setValue(values);
        meta.setValue(new Meta(System.currentTimeMillis(), true, false));
        preferences.setValue(collectCategories(values));
        loading.setValue(false);
    }

    private List<EventCategory> collectCategories(List<RecommendationUiModel> values) {
        Set<EventCategory> unique = new LinkedHashSet<>();
        for (RecommendationUiModel item : values) {
            unique.add(item.event.category);
            if (unique.size() >= MAX_PREFERENCE_CHIPS) {
                break;
            }
        }
        return new ArrayList<>(unique);
    }

    private List<RecommendationUiModel> copyOf(List<RecommendationUiModel> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
