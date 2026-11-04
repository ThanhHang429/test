package com.eventapp.app.ui.user.model;

/** Vé mẫu gắn với một sự kiện; mã vé đóng vai trò khoá tra cứu. */
public class DemoTicket {

    public enum Status {
        UPCOMING,
        USED,
        CANCELLED
    }

    public final String code;
    public final DemoEvent event;
    public final String typeName;
    public final int quantity;
    public final long total;
    public final long purchasedAt;
    public final Status status;

    public DemoTicket(String code, DemoEvent event, String typeName, int quantity,
                      long total, long purchasedAt, Status status) {
        this.code = code;
        this.event = event;
        this.typeName = typeName;
        this.quantity = quantity;
        this.total = total;
        this.purchasedAt = purchasedAt;
        this.status = status;
    }

    public boolean isUpcoming() {
        return status == Status.UPCOMING;
    }
}
