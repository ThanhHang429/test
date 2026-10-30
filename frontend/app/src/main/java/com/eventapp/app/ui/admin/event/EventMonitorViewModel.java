package com.eventapp.app.ui.admin.event;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;

/** FR-A08: ViewModel của màn hình EventMonitor - nạp dữ liệu qua Repository. */
public class EventMonitorViewModel extends ViewModel {

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getError() { return error; }

    public void setLoading(Boolean loading) { this.loading.setValue(loading); }
    public void setError(Boolean error) { this.error.setValue(error); }

    public void load() {
        loading.setValue(true);
        error.setValue(false);
    }

    public void refresh() {
        load();
    }
}
