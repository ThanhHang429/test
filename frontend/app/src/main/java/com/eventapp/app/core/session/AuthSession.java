package com.eventapp.app.core.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Phiên đăng nhập đang dùng trong bộ nhớ.
 *
 * <p>Không chứa refresh token vì backend hiện chỉ cấp access token (TTL 24h) qua
 * {@code POST /api/v1/dev/token}.</p>
 */
public final class AuthSession {

    public final long userId;
    @Nullable
    public final String email;
    @Nullable
    public final String fullName;
    @NonNull
    public final List<String> roles;
    @NonNull
    public final String token;
    public final long issuedAt;

    public AuthSession(long userId,
                       @Nullable String email,
                       @Nullable String fullName,
                       @Nullable List<String> roles,
                       @NonNull String token) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.roles = roles == null ? Collections.emptyList() : Collections.unmodifiableList(roles);
        this.token = token;
        this.issuedAt = System.currentTimeMillis();
    }

    public boolean hasRole(@Nullable String role) {
        if (role == null) {
            return false;
        }
        for (String value : roles) {
            if (role.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    /** Vai trò dùng để chọn màn hình host: ưu tiên ADMIN, rồi ORGANIZER, còn lại là USER. */
    @NonNull
    public String primaryRole() {
        if (hasRole("ADMIN")) {
            return "ADMIN";
        }
        if (hasRole("ORGANIZER")) {
            return "ORGANIZER";
        }
        return "USER";
    }

    /** Nhãn hiển thị ngắn cho header, ví dụ "ADMIN · admin@eticket.local". */
    @NonNull
    public String displayLabel() {
        String role = primaryRole();
        return email == null ? role : role + " · " + email;
    }
}
