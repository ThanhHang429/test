package com.eventapp.app.ui.admin.model;

/** Một sự kiện đang được giám sát vận hành. */
public final class AdminEventItem {

    public static final int TONE_LIVE = 0;
    public static final int TONE_FLAGGED = 1;
    public static final int TONE_PAUSED = 2;

    public final String title;
    public final String organizer;
    public final String category;
    public final int sold;
    public final int total;
    public final String revenue;
    public final String checkin;
    public final String risk;
    public final int tone;
    public final int coverRes;

    public AdminEventItem(String title, String organizer, String category, int sold, int total,
                          String revenue, String checkin, String risk, int tone, int coverRes) {
        this.title = title;
        this.organizer = organizer;
        this.category = category;
        this.sold = sold;
        this.total = total;
        this.revenue = revenue;
        this.checkin = checkin;
        this.risk = risk;
        this.tone = tone;
        this.coverRes = coverRes;
    }

    public int percent() {
        if (total <= 0) {
            return 0;
        }
        return Math.min(100, Math.round(sold * 100f / total));
    }
}
