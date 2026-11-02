package com.eventapp.app.data.remote.dto.auth;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Một tài khoản trong {@code GET /api/v1/dev/accounts} — chỉ có ở profile {@code dev}.
 *
 * <pre>{ "userId": 7, "email": "admin@eticket.local", "fullName": "...", "roles": ["ADMIN"] }</pre>
 */
public class DevAccountDto {

    public long userId;
    @Nullable
    public String email;
    @Nullable
    public String fullName;
    @Nullable
    public List<String> roles;
}
