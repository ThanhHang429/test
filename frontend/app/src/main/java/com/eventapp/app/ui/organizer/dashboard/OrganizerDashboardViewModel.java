package com.eventapp.app.ui.organizer.dashboard;


import com.eventapp.app.ui.fake.SampleDashboardData;
import com.eventapp.app.ui.model.DashboardUiModel;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * FR-O03 state holder. It mirrors API-17: one snapshot whose blocks keep individual statuses, so a
 * failed source never blanks the whole dashboard (O03-R05).
 */
public class OrganizerDashboardViewModel extends ViewModel {

    public enum Range {
        LAST_7_DAYS,
        LAST_30_DAYS,
        CUSTOM
    }

    public static final int MAX_RANGE_DAYS = 366;
    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;
    private static final long LOAD_DELAY_MS = 320L;
    private static final long BLOCK_RETRY_DELAY_MS = 420L;

    private final MutableLiveData<DashboardUiModel.Snapshot> snapshot = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Range> range = new MutableLiveData<>(Range.LAST_30_DAYS);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long customFrom;
    private long customTo;
    private boolean checkinLoaded;

    public OrganizerDashboardViewModel() {
        load();
    }

    public LiveData<DashboardUiModel.Snapshot> getSnapshot() {
        return snapshot;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<Range> getRange() {
        return range;
    }

    public void load() {
        loading.setValue(true);
        error.setValue(false);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::publish, LOAD_DELAY_MS);
    }

    public void refresh() {
        load();
    }

    public void setRange(Range value) {
        Range next = value == null ? Range.LAST_30_DAYS : value;
        if (next == range.getValue() && snapshot.getValue() != null) {
            return;
        }
        range.setValue(next);
        load();
    }

    /**
     * Stores a custom range. Returns false when the span exceeds 366 days (SRS: 400
     * VALIDATION_ERROR on the server, blocked here first).
     */
    public boolean setCustomRange(long from, long to) {
        if (from > 0 && to > 0) {
            long start = Math.min(from, to);
            long end = Math.max(from, to);
            long days = (end - start) / DAY_MILLIS + 1;
            if (days > MAX_RANGE_DAYS) {
                return false;
            }
            customFrom = start;
            customTo = end;
        } else {
            if (from > 0) {
                customFrom = from;
            }
            if (to > 0) {
                customTo = to;
            }
        }
        range.setValue(Range.CUSTOM);
        if (customFrom > 0 && customTo > 0) {
            load();
        }
        return true;
    }

    public long getCustomFrom() {
        return customFrom;
    }

    public long getCustomTo() {
        return customTo;
    }

    /** Reloads a single failing block without a full-screen skeleton (O03-R05). */
    public void retryMetric(DashboardUiModel.MetricKey key) {
        if (key == DashboardUiModel.MetricKey.CHECKIN) {
            checkinLoaded = true;
        }
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> snapshot.setValue(
                SampleDashboardData.create(rangeDays(), checkinLoaded)), BLOCK_RETRY_DELAY_MS);
    }

    public int rangeDays() {
        if (range.getValue() == Range.LAST_7_DAYS) {
            return 7;
        }
        if (range.getValue() == Range.CUSTOM && customFrom > 0 && customTo > 0) {
            long days = (customTo - customFrom) / DAY_MILLIS + 1;
            return (int) Math.min(MAX_RANGE_DAYS, Math.max(1L, days));
        }
        return 30;
    }

    private void publish() {
        snapshot.setValue(SampleDashboardData.create(rangeDays(), checkinLoaded));
        loading.setValue(false);
        error.setValue(false);
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
