package com.eventapp.app.data.remote.dto.auth;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Phản hồi của {@code POST /api/v1/dev/token?userId=...}.
 *
 * <pre>{ "userId": 10, "email": "...", "roles": ["USER"], "token": "eyJ...",
 *         "tokenType": "Bearer", "expiresInSeconds": 86400 }</pre>
 */
public class DevTokenDto {

    public long userId;
    @Nullable
    public String email;
    @Nullable
    public List<String> roles;
    @Nullable
    public String token;
    @Nullable
    public String tokenType;
    public long expiresInSeconds;
}
