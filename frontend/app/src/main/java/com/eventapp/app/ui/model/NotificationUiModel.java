package com.eventapp.app.ui.model;

/** Thông báo đã gửi cho người tham gia, dùng ở trung tâm thông báo và lịch sử FR-O28. */
public final class NotificationUiModel {

    public final long id;
    public final String title;
    public final String body;
    public final long sentAtMillis;
    public final String audience;
    public final int recipientCount;

    public NotificationUiModel(long id, String title, String body, long sentAtMillis,
                               String audience, int recipientCount) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.body = body == null ? "" : body;
        this.sentAtMillis = sentAtMillis;
        this.audience = audience == null ? "" : audience;
        this.recipientCount = recipientCount;
    }
}
