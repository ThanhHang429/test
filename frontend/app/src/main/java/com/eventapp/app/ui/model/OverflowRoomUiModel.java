package com.eventapp.app.ui.model;

/** Phòng dự phòng dùng khi phòng chính quá tải (FR-O28). */
public final class OverflowRoomUiModel {

    public final long id;
    public final String roomName;
    public final int capacity;
    public final String note;
    public final boolean active;

    public OverflowRoomUiModel(long id, String roomName, int capacity, String note,
                               boolean active) {
        this.id = id;
        this.roomName = roomName == null ? "" : roomName;
        this.capacity = capacity;
        this.note = note == null ? "" : note;
        this.active = active;
    }
}
