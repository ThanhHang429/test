package com.eventapp.app.ui.model;

import androidx.annotation.Nullable;

/** Mức lấp đầy phòng và cảnh báo điều phối của một sự kiện (FR-O28). */
public final class CrowdStatusUiModel {

    public enum Level {
        NORMAL,
        WATCH,
        CROWDED,
        FULL
    }

    public final int occupancyPercent;
    public final int capacityLimit;
    public final long registered;
    public final long checkedIn;
    public final long notCheckedIn;
    public final Level level;
    public final String warning;
    public final String suggestion;
    @Nullable
    public final OverflowRoomUiModel suggestedRoom;

    public CrowdStatusUiModel(int occupancyPercent, int capacityLimit, long registered,
                              long checkedIn, long notCheckedIn, Level level,
                              @Nullable String warning, @Nullable String suggestion,
                              @Nullable OverflowRoomUiModel suggestedRoom) {
        this.occupancyPercent = occupancyPercent;
        this.capacityLimit = capacityLimit;
        this.registered = registered;
        this.checkedIn = checkedIn;
        this.notCheckedIn = notCheckedIn;
        this.level = level == null ? Level.NORMAL : level;
        this.warning = warning == null ? "" : warning;
        this.suggestion = suggestion == null ? "" : suggestion;
        this.suggestedRoom = suggestedRoom;
    }
}
