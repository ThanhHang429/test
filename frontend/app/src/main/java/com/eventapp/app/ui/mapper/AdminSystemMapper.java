package com.eventapp.app.ui.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.data.remote.dto.integration.IntegrationDtos;
import com.eventapp.app.ui.model.AdminSystemUiModel;

import java.util.List;
import java.util.Locale;

/** Gom tình trạng tích hợp thanh toán/FCM/AI thành AdminSystemUiModel. */
public final class AdminSystemMapper {

    private AdminSystemMapper() {
    }

    @NonNull
    public static AdminSystemUiModel fromHealth(@Nullable List<IntegrationDtos.Health> items) {
        AdminSystemUiModel.Status payment = AdminSystemUiModel.Status.UNKNOWN;
        AdminSystemUiModel.Status fcm = AdminSystemUiModel.Status.UNKNOWN;
        AdminSystemUiModel.Status ai = AdminSystemUiModel.Status.UNKNOWN;
        if (items != null) {
            for (IntegrationDtos.Health item : items) {
                if (item == null || item.name == null) {
                    continue;
                }
                switch (item.name) {
                    case "PAYMENT":
                        payment = status(item.status);
                        break;
                    case "FCM":
                        fcm = status(item.status);
                        break;
                    case "AI":
                        ai = status(item.status);
                        break;
                    default:
                        break;
                }
            }
        }
        return new AdminSystemUiModel(payment, fcm, ai);
    }

    @NonNull
    private static AdminSystemUiModel.Status status(@Nullable String raw) {
        if (raw == null) {
            return AdminSystemUiModel.Status.UNKNOWN;
        }
        switch (raw.toUpperCase(Locale.ROOT)) {
            case "HEALTHY":
                return AdminSystemUiModel.Status.HEALTHY;
            case "DEGRADED":
                return AdminSystemUiModel.Status.DEGRADED;
            case "DOWN":
                return AdminSystemUiModel.Status.DOWN;
            default:
                return AdminSystemUiModel.Status.UNKNOWN;
        }
    }
}
