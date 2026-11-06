package com.eventapp.app.ui.admin.model;

/** Một thông báo vận hành trong trung tâm thông báo của admin. */
public final class AdminNotification {

    public static final int TYPE_MODERATION = 0;
    public static final int TYPE_FINANCE = 1;
    public static final int TYPE_SYSTEM = 2;
    public static final int TYPE_ACCOUNT = 3;

    public final String title;
    public final String body;
    public final String time;
    public final int type;
    public boolean unread;

    public AdminNotification(String title, String body, String time, int type, boolean unread) {
        this.title = title;
        this.body = body;
        this.time = time;
        this.type = type;
        this.unread = unread;
    }
}
