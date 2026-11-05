package com.eventapp.app.ui.admin.viewmodel;

import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.admin.data.AdminDemo;
import com.eventapp.app.ui.admin.model.AdminEventItem;

import java.util.List;

/** Giám sát sự kiện của quản trị (FR-A05). */
public class AdminEventsViewModel extends ViewModel {
    public List<AdminEventItem> events() {
        return AdminDemo.events();
    }
}
