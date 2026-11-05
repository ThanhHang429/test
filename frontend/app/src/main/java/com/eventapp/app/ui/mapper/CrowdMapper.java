package com.eventapp.app.ui.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.data.remote.dto.organizer.CrowdDtos;
import com.eventapp.app.ui.model.CrowdStatusUiModel;
import com.eventapp.app.ui.model.OverflowRoomUiModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Chuyển dữ liệu điều phối phòng backend sang UI model. */
public final class CrowdMapper {

    private CrowdMapper() {
    }

    @NonNull
    public static CrowdStatusUiModel fromStatus(@NonNull CrowdDtos.CrowdStatus item) {
        return new CrowdStatusUiModel(
                (int) Math.round(item.occupancyRate * 100),
                item.capacityLimit,
                item.registered,
                item.checkedIn,
                item.notCheckedIn,
                level(item.level),
                item.warning,
                item.suggestion,
                item.suggestedRoom == null ? null : fromRoom(item.suggestedRoom));
    }

    @NonNull
    public static List<OverflowRoomUiModel> fromRooms(
            @Nullable List<CrowdDtos.OverflowRoom> items) {
        List<OverflowRoomUiModel> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (CrowdDtos.OverflowRoom item : items) {
            if (item != null) {
                result.add(fromRoom(item));
            }
        }
        return result;
    }

    @NonNull
    private static OverflowRoomUiModel fromRoom(@NonNull CrowdDtos.OverflowRoom item) {
        return new OverflowRoomUiModel(
                item.id,
                item.roomName,
                item.capacity,
                item.note,
                "ACTIVE".equalsIgnoreCase(item.status));
    }

    @NonNull
    private static CrowdStatusUiModel.Level level(@Nullable String raw) {
        if (raw == null) {
            return CrowdStatusUiModel.Level.NORMAL;
        }
        switch (raw.toUpperCase(Locale.ROOT)) {
            case "WATCH":
                return CrowdStatusUiModel.Level.WATCH;
            case "CROWDED":
                return CrowdStatusUiModel.Level.CROWDED;
            case "FULL":
                return CrowdStatusUiModel.Level.FULL;
            default:
                return CrowdStatusUiModel.Level.NORMAL;
        }
    }
}
