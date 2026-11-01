package com.eventapp.app.ui.demo;

import com.eventapp.app.core.enums.Role;

/**
 * Tài khoản cục bộ chỉ phục vụ luồng UI demo.
 *
 * <p>Không phải tài khoản backend, không có token và không được dùng cho production. Khi tầng
 * session thật hoàn thiện, toàn bộ package {@code ui.demo} có thể được thay thế mà không ảnh hưởng
 * tới layout/navigation.</p>
 */
public final class DemoAccount {

    private final String email;
    private final String passwordHash;
    private final String displayName;
    private final Role role;
    private final String phone;
    private final String organizationName;
    private final boolean emailVerified;

    public DemoAccount(String email,
                       String passwordHash,
                       String displayName,
                       Role role,
                       String phone,
                       String organizationName,
                       boolean emailVerified) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.phone = phone;
        this.organizationName = organizationName;
        this.emailVerified = emailVerified;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public String getPhone() {
        return phone;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public DemoAccount withPasswordHash(String newPasswordHash) {
        return new DemoAccount(email, newPasswordHash, displayName, role, phone,
                organizationName, emailVerified);
    }

    public DemoAccount withEmailVerified(boolean verified) {
        return new DemoAccount(email, passwordHash, displayName, role, phone,
                organizationName, verified);
    }
}
