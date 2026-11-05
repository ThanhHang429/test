package com.eventapp.app.ui.host.viewmodel;

import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.user.data.DemoContent;

/** Host người dùng: huy hiệu thông báo chưa đọc. */
public class UserHostViewModel extends ViewModel {
    public int unreadNotifications() {
        return DemoContent.get().unreadNotifications();
    }
}
