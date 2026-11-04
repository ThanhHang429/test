package com.eventapp.app.ui.organizer.model;

/** Sự kiện trong khu nhà tổ chức (dữ liệu mẫu để kiểm thử giao diện). */
public final class OrgEvent {

    public static final String FILTER_ALL = "ALL";
    public static final String FILTER_ON_SALE = "ON_SALE";
    public static final String FILTER_PENDING = "PENDING";
    public static final String FILTER_DRAFT = "DRAFT";
    public static final String FILTER_ENDED = "ENDED";

    public final String code;
    public final String title;
    public final String category;
    public final String meta;
    public final String location;
    public final String filter;
    public final String statusLabel;
    public final int tone;
    public final int coverRes;
    public final int sold;
    public final int total;
    public final long revenue;
    public final int checkin;

    public OrgEvent(String code, String title, String category, String meta, String location,
                    String filter, String statusLabel, int tone, int coverRes,
                    int sold, int total, long revenue, int checkin) {
        this.code = code;
        this.title = title;
        this.category = category;
        this.meta = meta;
        this.location = location;
        this.filter = filter;
        this.statusLabel = statusLabel;
        this.tone = tone;
        this.coverRes = coverRes;
        this.sold = sold;
        this.total = total;
        this.revenue = revenue;
        this.checkin = checkin;
    }

    public int percent() {
        return total <= 0 ? 0 : Math.round(sold * 100f / total);
    }
}
