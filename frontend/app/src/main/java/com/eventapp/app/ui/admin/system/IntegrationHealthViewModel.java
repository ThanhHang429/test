package com.eventapp.app.ui.admin.system;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.fake.SampleIntegrationData;
import com.eventapp.app.ui.model.AiTaskToggleUiModel;
import com.eventapp.app.ui.model.IntegrationErrorUiModel;
import com.eventapp.app.ui.model.ServiceHealthUiModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * FR-A20 state holder: overall status, per-service metrics, AI kill switches and recent errors.
 * The numbers come from {@link SampleIntegrationData} until {@code /admin/integrations} exists.
 */
public class IntegrationHealthViewModel extends ViewModel {

    public static class ActionEvent {
        public final String taskType;
        public final boolean enabled;

        ActionEvent(String taskType, boolean enabled) {
            this.taskType = taskType;
            this.enabled = enabled;
        }
    }

    private static final long LOAD_DELAY_MS = 360L;

    private final MutableLiveData<List<ServiceHealthUiModel>> services =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<AiTaskToggleUiModel>> tasks =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<IntegrationErrorUiModel>> errors =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ServiceHealthUiModel.Status> overall =
            new MutableLiveData<>(ServiceHealthUiModel.Status.UNKNOWN);
    private final MutableLiveData<float[]> hourlyErrors = new MutableLiveData<>(new float[0]);
    private final MutableLiveData<String> updatedAt = new MutableLiveData<>("");
    private final MutableLiveData<Integer> rangeHours = new MutableLiveData<>(24);
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<ActionEvent> actionEvent = new MutableLiveData<>();

    private final Handler handler = new Handler(Looper.getMainLooper());

    public IntegrationHealthViewModel() {
        load();
    }

    public LiveData<List<ServiceHealthUiModel>> getServices() {
        return services;
    }

    public LiveData<List<AiTaskToggleUiModel>> getTasks() {
        return tasks;
    }

    public LiveData<List<IntegrationErrorUiModel>> getErrors() {
        return errors;
    }

    public LiveData<ServiceHealthUiModel.Status> getOverall() {
        return overall;
    }

    public LiveData<float[]> getHourlyErrors() {
        return hourlyErrors;
    }

    public LiveData<String> getUpdatedAt() {
        return updatedAt;
    }

    public LiveData<Integer> getRangeHours() {
        return rangeHours;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<ActionEvent> getActionEvent() {
        return actionEvent;
    }

    public void setRange(int hours) {
        rangeHours.setValue(hours);
        load();
    }

    public void retry() {
        load();
    }

    public void setTaskEnabled(String taskType, boolean enabled) {
        List<AiTaskToggleUiModel> current = tasks.getValue();
        if (current == null) {
            return;
        }
        List<AiTaskToggleUiModel> updated = new ArrayList<>(current.size());
        for (AiTaskToggleUiModel item : current) {
            updated.add(item.taskType.equals(taskType) ? item.withEnabled(enabled) : item);
        }
        tasks.setValue(updated);
        actionEvent.setValue(new ActionEvent(taskType, enabled));
    }

    private void load() {
        loading.setValue(true);
        error.setValue(false);
        Integer hours = rangeHours.getValue();
        final int range = hours == null ? 24 : hours;
        handler.postDelayed(() -> {
            List<ServiceHealthUiModel> serviceItems = SampleIntegrationData.services();
            services.setValue(serviceItems);
            overall.setValue(SampleIntegrationData.overall(serviceItems));
            // Công tắc là trạng thái người vận hành đã đặt: đổi khoảng thời gian không được reset.
            List<AiTaskToggleUiModel> currentTasks = tasks.getValue();
            if (currentTasks == null || currentTasks.isEmpty()) {
                tasks.setValue(SampleIntegrationData.tasks());
            }
            errors.setValue(SampleIntegrationData.errors());
            hourlyErrors.setValue(SampleIntegrationData.hourlyErrors(range));
            updatedAt.setValue(new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date()));
            loading.setValue(false);
        }, LOAD_DELAY_MS);
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
