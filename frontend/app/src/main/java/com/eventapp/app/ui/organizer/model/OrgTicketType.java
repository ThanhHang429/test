package com.eventapp.app.ui.organizer.model;

/** Một hạng vé của sự kiện. */
public final class OrgTicketType {

    public final String name;
    public final long price;
    public final int sold;
    public final int total;
    public final String statusLabel;
    public final int tone;

    public OrgTicketType(String name, long price, int sold, int total, String statusLabel, int tone) {
        this.name = name;
        this.price = price;
        this.sold = sold;
        this.total = total;
        this.statusLabel = statusLabel;
        this.tone = tone;
    }

    public int percent() {
        return total <= 0 ? 0 : Math.round(sold * 100f / total);
    }
}
