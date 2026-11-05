package com.eventapp.app.ui.admin.viewmodel;

import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.admin.data.AdminDemo;
import com.eventapp.app.ui.admin.model.AdminAccount;

import java.util.List;

/** Tài khoản quản trị (FR-A05). */
public class AdminAccountsViewModel extends ViewModel {
    public List<AdminAccount> users() {
        return AdminDemo.users();
    }

    public List<AdminAccount> organizers() {
        return AdminDemo.organizers();
    }
}
