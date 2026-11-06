package com.eventapp.app.ui.model;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/** Tài khoản seed hiển thị ở màn đăng nhập (chỉ có ở backend dev). */
public final class DevAccountUiModel {

    public final long userId;
    public final String email;
    public final String fullName;
    public final List<String> roles;

    public DevAccountUiModel(long userId, @Nullable String email, @Nullable String fullName,
                             @Nullable List<String> roles) {
        this.userId = userId;
        this.email = email == null ? "" : email;
        this.fullName = fullName == null ? "" : fullName;
        this.roles = roles == null ? Collections.emptyList() : roles;
    }
}
