package com.eventapp.app.ui.user.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

/** Thông báo mẫu cho trung tâm thông báo của khu người dùng. */
public class DemoNotification {

    public final String id;
    public final String title;
    public final String body;
    public final long createdAt;
    @DrawableRes
    public final int iconRes;
    @ColorRes
    public final int iconBackgroundRes;
    @ColorRes
    public final int iconTintRes;
    public boolean unread;

    public DemoNotification(String id, String title, String body, long createdAt,
                            @DrawableRes int iconRes, @ColorRes int iconBackgroundRes,
                            @ColorRes int iconTintRes, boolean unread) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.createdAt = createdAt;
        this.iconRes = iconRes;
        this.iconBackgroundRes = iconBackgroundRes;
        this.iconTintRes = iconTintRes;
        this.unread = unread;
    }
}
