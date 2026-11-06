package com.eventapp.app.ui.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.data.remote.dto.auth.DevAccountDto;
import com.eventapp.app.ui.model.DevAccountUiModel;

import java.util.ArrayList;
import java.util.List;

/** Chuyển tài khoản seed backend sang DevAccountUiModel và ngược lại. */
public final class DevAccountMapper {

    private DevAccountMapper() {
    }

    @NonNull
    public static List<DevAccountUiModel> fromList(@Nullable List<DevAccountDto> items) {
        List<DevAccountUiModel> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (DevAccountDto item : items) {
            if (item != null) {
                result.add(from(item));
            }
        }
        return result;
    }

    @NonNull
    public static DevAccountUiModel from(@NonNull DevAccountDto item) {
        return new DevAccountUiModel(item.userId, item.email, item.fullName, item.roles);
    }

    @NonNull
    public static DevAccountDto toDto(@NonNull DevAccountUiModel item) {
        DevAccountDto dto = new DevAccountDto();
        dto.userId = item.userId;
        dto.email = item.email;
        dto.fullName = item.fullName;
        dto.roles = new ArrayList<>(item.roles);
        return dto;
    }
}
