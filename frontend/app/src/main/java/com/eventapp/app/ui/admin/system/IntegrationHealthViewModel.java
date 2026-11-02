package com.eventapp.app.ui.admin.system;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.R;
import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.integration.IntegrationDtos;
import com.eventapp.app.data.repository.SystemConfigRepository;
import com.eventapp.app.ui.model.AiTaskToggleUiModel;
import com.eventapp.app.ui.model.IntegrationErrorUiModel;
import com.eventapp.app.ui.model.ServiceHealthUiModel;
import com.google.gson.JsonElement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FR-A20 — giám sát tích hợp từ {@code GET /admin/integrations} và các API AI telemetry.
 *
 * <p>Công tắc từng tác vụ AI gọi {@code PUT /admin/integrations/ai/features/{taskType}} và có hiệu
 * lực ngay. Đồ thị lỗi theo giờ chưa có API nên chỉ hiển thị khi có dữ liệu.</p>
 */
public class IntegrationHealthViewModel extends ViewModel {

    public static class ActionEvent {
        public final String taskType;
        public final boolean enabled;
        public final String message;

        ActionEvent(String taskType, boolean enabled, @Nullable String message) {
            this.taskType = taskType;
            this.enabled = enabled;
            this.message = message;
        }
    }

    private final SystemConfigRepository repository =
            ServiceLocator.get(SystemConfigRepository.class);

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

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", new Locale("vi", "VN"));

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
        repository.toggleFeature(taskType, enabled, "Điều chỉnh từ màn giám sát tích hợp",
                new ApiCallback<IntegrationDtos.ToggleResponse>() {
                    @Override
                    public void onSuccess(@Nullable IntegrationDtos.ToggleResponse data,
                                          @Nullable JsonElement meta) {
                        if (data != null && data.features != null) {
                            tasks.setValue(mapTasks(data.features));
                        }
                        actionEvent.setValue(new ActionEvent(taskType, enabled, null));
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        actionEvent.setValue(new ActionEvent(taskType, enabled,
                                exception.getUserMessage()));
                        load();
                    }
                });
    }

    private void load() {
        loading.setValue(true);
        error.setValue(false);
        loadHealth();
        loadUsage();
        loadErrors();
    }

    private void loadHealth() {
        repository.health(new ApiCallback<List<IntegrationDtos.Health>>() {
            @Override
            public void onSuccess(@Nullable List<IntegrationDtos.Health> data,
                                  @Nullable JsonElement meta) {
                List<ServiceHealthUiModel> items = new ArrayList<>();
                ServiceHealthUiModel.Status worst = ServiceHealthUiModel.Status.UNKNOWN;
                if (data != null) {
                    for (IntegrationDtos.Health health : data) {
                        if (health == null) {
                            continue;
                        }
                        ServiceHealthUiModel.Status status = status(health.status);
                        worst = worse(worst, status);
                        items.add(new ServiceHealthUiModel(
                                nameRes(health.name),
                                status,
                                (int) Math.round(health.errorRate * 100),
                                health.p95LatencyMs == null ? 0 : health.p95LatencyMs.intValue(),
                                (int) Math.min(Integer.MAX_VALUE, health.requestCount)));
                    }
                }
                services.setValue(items);
                overall.setValue(worst);
                updatedAt.setValue(timeFormat.format(new Date()));
                loading.setValue(false);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                loading.setValue(false);
                error.setValue(true);
            }
        });
    }

    private void loadUsage() {
        Integer hours = rangeHours.getValue();
        String range = hours != null && hours >= 168 ? "7d" : (hours != null && hours <= 1
                ? "1h" : "24h");
        repository.aiUsage(range, new ApiCallback<IntegrationDtos.Usage>() {
            @Override
            public void onSuccess(@Nullable IntegrationDtos.Usage data,
                                  @Nullable JsonElement meta) {
                if (data == null) {
                    return;
                }
                if (data.features != null) {
                    tasks.setValue(mapTasks(data.features));
                }
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                // Khối lỗi riêng, không chặn trạng thái dịch vụ.
            }
        });
    }

    private void loadErrors() {
        repository.aiErrors(20, new ApiCallback<List<IntegrationDtos.ErrorItem>>() {
            @Override
            public void onSuccess(@Nullable List<IntegrationDtos.ErrorItem> data,
                                  @Nullable JsonElement meta) {
                List<IntegrationErrorUiModel> items = new ArrayList<>();
                if (data != null) {
                    for (IntegrationDtos.ErrorItem item : data) {
                        if (item == null) {
                            continue;
                        }
                        items.add(new IntegrationErrorUiModel(
                                timeLabel(item.at),
                                item.taskType == null ? "" : item.taskType,
                                item.errorCode == null ? "" : item.errorCode,
                                item.latencyMs == null ? 0 : item.latencyMs,
                                item.correlationId == null ? "" : item.correlationId));
                    }
                }
                errors.setValue(items);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                errors.setValue(new ArrayList<>());
            }
        });
    }

    @NonNull
    private String timeLabel(@Nullable String iso) {
        long millis = DateTimeUtils.toMillis(iso);
        return millis <= 0L ? "" : timeFormat.format(new Date(millis));
    }

    @NonNull
    private static List<AiTaskToggleUiModel> mapTasks(@Nullable Map<String, Boolean> features) {
        List<AiTaskToggleUiModel> items = new ArrayList<>();
        if (features == null) {
            return items;
        }
        addTask(items, features, "RECOMMEND", R.string.hc_health_task_recommend);
        addTask(items, features, "CHAT", R.string.hc_health_task_chat);
        addTask(items, features, "DESCRIPTION", R.string.hc_health_task_description);
        addTask(items, features, "ANALYTICS", R.string.hc_health_task_analytics);
        addTask(items, features, "MODERATION", R.string.hc_health_task_moderation);
        return items;
    }

    private static void addTask(@NonNull List<AiTaskToggleUiModel> items,
                                @NonNull Map<String, Boolean> features,
                                @NonNull String key, int labelRes) {
        Boolean enabled = features.get(key);
        if (enabled != null) {
            items.add(new AiTaskToggleUiModel(key, labelRes, enabled));
        }
    }

    private static int nameRes(@Nullable String name) {
        if (name == null) {
            return R.string.hc_health_service_ai;
        }
        switch (name.toUpperCase(Locale.ROOT)) {
            case "PAYMENT":
                return R.string.hc_health_service_payment;
            case "FCM":
                return R.string.hc_health_service_fcm;
            case "AI":
            default:
                return R.string.hc_health_service_ai;
        }
    }

    @NonNull
    private static ServiceHealthUiModel.Status status(@Nullable String raw) {
        if (raw == null) {
            return ServiceHealthUiModel.Status.UNKNOWN;
        }
        switch (raw.toUpperCase(Locale.ROOT)) {
            case "HEALTHY":
                return ServiceHealthUiModel.Status.HEALTHY;
            case "DEGRADED":
                return ServiceHealthUiModel.Status.DEGRADED;
            case "DOWN":
                return ServiceHealthUiModel.Status.DOWN;
            case "UNKNOWN":
            default:
                return ServiceHealthUiModel.Status.UNKNOWN;
        }
    }

    @NonNull
    private static ServiceHealthUiModel.Status worse(
            @NonNull ServiceHealthUiModel.Status a,
            @NonNull ServiceHealthUiModel.Status b) {
        if (a == ServiceHealthUiModel.Status.DOWN || b == ServiceHealthUiModel.Status.DOWN) {
            return ServiceHealthUiModel.Status.DOWN;
        }
        if (a == ServiceHealthUiModel.Status.DEGRADED
                || b == ServiceHealthUiModel.Status.DEGRADED) {
            return ServiceHealthUiModel.Status.DEGRADED;
        }
        if (a == ServiceHealthUiModel.Status.HEALTHY
                || b == ServiceHealthUiModel.Status.HEALTHY) {
            return ServiceHealthUiModel.Status.HEALTHY;
        }
        return ServiceHealthUiModel.Status.UNKNOWN;
    }
}
