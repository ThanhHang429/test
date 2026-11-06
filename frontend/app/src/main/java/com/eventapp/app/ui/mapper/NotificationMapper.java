package com.eventapp.app.ui.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.notification.NotificationResponse;
import com.eventapp.app.ui.model.NotificationUiModel;

import java.util.ArrayList;
import java.util.List;

/** Chuyển thông báo backend sang NotificationUiModel. */
public final class NotificationMapper {

    private NotificationMapper() {
    }

    @NonNull
    public static List<NotificationUiModel> fromList(
            @Nullable List<NotificationResponse> items) {
        List<NotificationUiModel> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (NotificationResponse item : items) {
            if (item != null) {
                result.add(from(item));
            }
        }
        return result;
    }

    @NonNull
    private static NotificationUiModel from(@NonNull NotificationResponse item) {
        return new NotificationUiModel(
                item.id,
                item.title,
                item.body,
                DateTimeUtils.toMillis(item.sentAt),
                item.audience,
                item.recipientCount);
    }
}
