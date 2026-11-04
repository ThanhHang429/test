package com.eventapp.app.ui.admin.model;

/** Một hồ sơ sự kiện đang chờ kiểm duyệt. */
public final class AdminReview {

    public static final int TONE_PRIORITY = 0;
    public static final int TONE_WARNING = 1;
    public static final int TONE_OVERDUE = 2;

    public final String title;
    public final String organizer;
    public final String category;
    public final String capacity;
    public final String price;
    public final String deadline;
    public final String reviewer;
    public final String initials;
    public final int tone;
    public final int coverRes;

    public AdminReview(String title, String organizer, String category, String capacity,
                       String price, String deadline, String reviewer, String initials,
                       int tone, int coverRes) {
        this.title = title;
        this.organizer = organizer;
        this.category = category;
        this.capacity = capacity;
        this.price = price;
        this.deadline = deadline;
        this.reviewer = reviewer;
        this.initials = initials;
        this.tone = tone;
        this.coverRes = coverRes;
    }
}
