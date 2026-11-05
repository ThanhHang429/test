package com.eventapp.app.data.remote.dto.notification;

/**
 * Thông báo organizer gửi cho người đã đăng ký (FR-O28).
 *
 * <p>Dùng chung cho lịch sử gửi của organizer và danh sách {@code GET /me/notifications} của
 * người tham dự.</p>
 */
public class NotificationResponse {

    public long id;
    public long eventId;
    public String eventTitle;
    public String title;
    public String body;
    /** REGISTERED_NOT_CHECKED_IN | ALL_REGISTERED */
    public String audience;
    public int recipientCount;
    /** AI | MANUAL */
    public String source;
    public String sentAt;
}
