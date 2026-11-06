package com.eventapp.app.ui.admin.viewmodel;

import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.admin.data.AdminDemo;
import com.eventapp.app.ui.admin.model.AdminNotification;

import java.util.List;

/** Thông báo của quản trị (FR-A05). */
public class AdminNotificationsViewModel extends ViewModel {
    public List<AdminNotification> notifications() {
        return AdminDemo.notifications();
    }
}
