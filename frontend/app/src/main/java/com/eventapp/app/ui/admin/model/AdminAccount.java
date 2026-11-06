package com.eventapp.app.ui.admin.model;

/** Tài khoản người dùng hoặc nhà tổ chức trong khu quản trị. */
public final class AdminAccount {

    public static final int TYPE_USER = 0;
    public static final int TYPE_ORG = 1;

    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_FLAGGED = 1;
    public static final int STATUS_LOCKED = 2;

    public final String id;
    public final String name;
    public final String email;
    public final String initials;
    public final String meta;
    public final int type;
    public final int status;

    public AdminAccount(String id, String name, String email, String initials, String meta,
                        int type, int status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.initials = initials;
        this.meta = meta;
        this.type = type;
        this.status = status;
    }
}
