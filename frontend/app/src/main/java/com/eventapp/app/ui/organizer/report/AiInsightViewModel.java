package com.eventapp.app.ui.organizer.report;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

/**
 * FR-O27 state holder. Refreshes are limited to 5 per event per day; after that the last stored
 * analysis is shown again (SRS quota rule).
 */
public class AiInsightViewModel extends ViewModel {

    public static final int MAX_REFRESH_PER_DAY = 5;
    private static final long LOAD_DELAY_MS = 420L;

    private final MutableLiveData<AiInsightUiModel.Analysis> analysis = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> quotaReached = new MutableLiveData<>(false);
    private final List<AiInsightUiModel.EventOption> events = SampleInsightData.createEvents();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long eventId;
    private int refreshCount;

    public AiInsightViewModel() {
        eventId = events.isEmpty() ? -1L : events.get(0).id;
        load();
    }

    public List<AiInsightUiModel.EventOption> getEvents() {
        return events;
    }

    public LiveData<AiInsightUiModel.Analysis> getAnalysis() {
        return analysis;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<Boolean> getQuotaReached() {
        return quotaReached;
    }

    public long getEventId() {
        return eventId;
    }

    public void selectEvent(long id) {
        if (id <= 0L || id == eventId) {
            return;
        }
        eventId = id;
        refreshCount = 0;
        quotaReached.setValue(false);
        load();
    }

    public boolean canRefresh() {
        return refreshCount < MAX_REFRESH_PER_DAY;
    }

    public void refresh() {
        if (!canRefresh()) {
            quotaReached.setValue(true);
            return;
        }
        refreshCount++;
        quotaReached.setValue(refreshCount >= MAX_REFRESH_PER_DAY);
        load();
    }

    public void retry() {
        load();
    }

    private void load() {
        loading.setValue(true);
        error.setValue(false);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            analysis.setValue(SampleInsightData.create(eventId));
            loading.setValue(false);
        }, LOAD_DELAY_MS);
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
