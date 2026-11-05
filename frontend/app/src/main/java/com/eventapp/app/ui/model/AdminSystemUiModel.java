package com.eventapp.app.ui.model;

/** Tình trạng ba tích hợp ngoài hiển thị ở tab Hệ thống của quản trị (FR-A20). */
public final class AdminSystemUiModel {

    public enum Status {
        HEALTHY,
        DEGRADED,
        DOWN,
        UNKNOWN
    }

    public final Status paymentStatus;
    public final Status fcmStatus;
    public final Status aiStatus;

    public AdminSystemUiModel(Status paymentStatus, Status fcmStatus, Status aiStatus) {
        this.paymentStatus = statusOrUnknown(paymentStatus);
        this.fcmStatus = statusOrUnknown(fcmStatus);
        this.aiStatus = statusOrUnknown(aiStatus);
    }

    private static Status statusOrUnknown(Status status) {
        return status == null ? Status.UNKNOWN : status;
    }
}
