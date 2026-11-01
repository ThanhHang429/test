package com.eventapp.app.ui.fake;

import com.eventapp.app.R;
import com.eventapp.app.ui.model.AiTaskToggleUiModel;
import com.eventapp.app.ui.model.IntegrationErrorUiModel;
import com.eventapp.app.ui.model.ServiceHealthUiModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Sample data for FR-A20 while {@code /admin/integrations} is not wired. */
public final class SampleIntegrationData {

    private SampleIntegrationData() {
    }

    public static List<ServiceHealthUiModel> services() {
        return Arrays.asList(
                new ServiceHealthUiModel(R.string.hc_health_service_ai,
                        ServiceHealthUiModel.Status.HEALTHY, 2, 640, 412),
                new ServiceHealthUiModel(R.string.hc_health_service_payment,
                        ServiceHealthUiModel.Status.DEGRADED, 18, 1240, 236),
                new ServiceHealthUiModel(R.string.hc_health_service_fcm,
                        ServiceHealthUiModel.Status.HEALTHY, 0, 210, 1890));
    }

    public static List<AiTaskToggleUiModel> tasks() {
        List<AiTaskToggleUiModel> items = new ArrayList<>();
        items.add(new AiTaskToggleUiModel("RECOMMEND", R.string.hc_health_task_recommend, true));
        items.add(new AiTaskToggleUiModel("CHAT", R.string.hc_health_task_chat, true));
        items.add(new AiTaskToggleUiModel("DESCRIPTION", R.string.hc_health_task_description, true));
        items.add(new AiTaskToggleUiModel("ANALYTICS", R.string.hc_health_task_analytics, false));
        items.add(new AiTaskToggleUiModel("MODERATION", R.string.hc_health_task_moderation, true));
        return items;
    }

    public static List<IntegrationErrorUiModel> errors() {
        List<IntegrationErrorUiModel> items = new ArrayList<>();
        items.add(new IntegrationErrorUiModel("09:42", "DESCRIPTION", "AI_UNAVAILABLE", 20000,
                "8f3c2a71-4d1e-4b8a-9f22-1c7d0e5a3b64"));
        items.add(new IntegrationErrorUiModel("09:31", "RECOMMEND", "AI_INVALID_OUTPUT", 1840,
                "5b0e9d12-77ac-4c31-8a04-2e6f9b1d4c07"));
        items.add(new IntegrationErrorUiModel("09:18", "ANALYTICS", "RATE_LIMITED", 120,
                "c4a71e58-0d9b-4f62-b3a1-7e5c2d8f9a10"));
        items.add(new IntegrationErrorUiModel("08:57", "MODERATION", "AI_UNAVAILABLE", 15000,
                "1d9f4b60-3c2a-4e71-9b85-6a0c7e2f4d39"));
        items.add(new IntegrationErrorUiModel("08:44", "CHAT", "TIMEOUT", 20000,
                "e27b5c94-8a13-4d60-9f2b-3c1e8a7d5f02"));
        items.add(new IntegrationErrorUiModel("08:20", "RECOMMEND", "CIRCUIT_OPEN", 12,
                "6f0a3d17-9c4e-4b82-8d51-0e2b7c9a4f68"));
        items.add(new IntegrationErrorUiModel("07:58", "DESCRIPTION", "AI_DISABLED", 8,
                "3a8e1f72-5b09-4c6d-9a34-8f2e0d7b1c95"));
        items.add(new IntegrationErrorUiModel("07:35", "MODERATION", "AI_INVALID_OUTPUT", 2210,
                "9c5d2b08-1e7f-4a93-b6c4-5d0a8e3f7b21"));
        return items;
    }

    /** Lỗi theo từng mốc thời gian, đổi độ chi tiết theo khoảng đang chọn (1 giờ / 24 giờ / 7 ngày). */
    public static float[] hourlyErrors(int rangeHours) {
        if (rangeHours <= 1) {
            return new float[]{0, 0, 1, 0, 2, 1, 3, 2, 1, 4, 2, 1};
        }
        if (rangeHours >= 168) {
            return new float[]{12, 9, 18, 14, 7, 11, 6};
        }
        return new float[]{0, 0, 1, 0, 0, 2, 1, 3, 4, 2, 1, 5, 3, 2, 1, 0, 2, 4, 3, 1, 0, 1, 0, 0};
    }

    public static ServiceHealthUiModel.Status overall(List<ServiceHealthUiModel> services) {
        if (services == null || services.isEmpty()) {
            return ServiceHealthUiModel.Status.UNKNOWN;
        }
        boolean degraded = false;
        for (ServiceHealthUiModel service : services) {
            if (service.status == ServiceHealthUiModel.Status.DOWN) {
                return ServiceHealthUiModel.Status.DOWN;
            }
            if (service.status == ServiceHealthUiModel.Status.DEGRADED) {
                degraded = true;
            }
        }
        return degraded ? ServiceHealthUiModel.Status.DEGRADED
                : ServiceHealthUiModel.Status.HEALTHY;
    }
}
