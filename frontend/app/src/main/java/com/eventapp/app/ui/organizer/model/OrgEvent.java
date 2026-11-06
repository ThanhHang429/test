package com.eventapp.app.ui.organizer.model;

import com.eventapp.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sự kiện trong khu nhà tổ chức — map từ {@code OrganizerEventDtos.Event} (FR-O02).
 *
 * <p>Model giữ cả hai vòng đời độc lập: duyệt nội dung ({@code reviewStatus}) và vận hành
 * ({@code lifecycleStatus}) đúng như backend trả về.</p>
 */
public final class OrgEvent {

    public static final String FILTER_ALL = "ALL";
    public static final String FILTER_ON_SALE = "PUBLISHED";
    public static final String FILTER_PENDING = "PENDING_REVIEW";
    public static final String FILTER_DRAFT = "DRAFT";
    public static final String FILTER_ENDED = "COMPLETED";

    public final long id;
    public final String code;
    public final String title;
    public final String coverUrl;
    public final String reviewStatus;
    public final String lifecycleStatus;
    public final String city;
    public final String venueName;
    public final String currency;
    public final long startAt;
    public final long endAt;
    public final Integer capacityLimit;
    public final int ticketsSold;
    public final long revenue;
    public final List<TicketType> ticketTypes;

    public OrgEvent(long id, String code, String title, String coverUrl, String reviewStatus,
                    String lifecycleStatus, String city, String venueName, String currency,
                    long startAt, long endAt, Integer capacityLimit, int ticketsSold, long revenue,
                    List<TicketType> ticketTypes) {
        this.id = id;
        this.code = code == null ? "" : code;
        this.title = title == null ? "" : title;
        this.coverUrl = coverUrl;
        this.reviewStatus = reviewStatus == null ? "" : reviewStatus;
        this.lifecycleStatus = lifecycleStatus == null ? "" : lifecycleStatus;
        this.city = city == null ? "" : city;
        this.venueName = venueName == null ? "" : venueName;
        this.currency = currency == null ? "VND" : currency;
        this.startAt = startAt;
        this.endAt = endAt;
        this.capacityLimit = capacityLimit;
        this.ticketsSold = ticketsSold;
        this.revenue = revenue;
        this.ticketTypes = Collections.unmodifiableList(
                ticketTypes == null ? new ArrayList<>() : new ArrayList<>(ticketTypes));
    }

    public int totalTickets() {
        if (capacityLimit != null && capacityLimit > 0) {
            return capacityLimit;
        }
        int total = 0;
        for (TicketType type : ticketTypes) {
            total += type.total;
        }
        return total;
    }

    public int percent() {
        int total = totalTickets();
        return total <= 0 ? 0 : Math.round(ticketsSold * 100f / total);
    }

    /** Nhãn gộp: ưu tiên vòng đời duyệt nội dung khi chưa được duyệt. */
    public String statusLabel() {
        if (!"APPROVED".equals(reviewStatus)) {
            return reviewLabel();
        }
        return lifecycleLabel();
    }

    public int tone() {
        if (!"APPROVED".equals(reviewStatus)) {
            return reviewTone();
        }
        return lifecycleTone();
    }

    public String reviewLabel() {
        switch (reviewStatus) {
            case "DRAFT":
                return "Bản nháp";
            case "PENDING_REVIEW":
                return "Chờ duyệt";
            case "REVISION_REQUIRED":
                return "Cần sửa";
            case "REJECTED":
                return "Bị từ chối";
            case "APPROVED":
                return "Đã duyệt";
            default:
                return reviewStatus.isEmpty() ? "—" : reviewStatus;
        }
    }

    public int reviewTone() {
        switch (reviewStatus) {
            case "REJECTED":
            case "REVISION_REQUIRED":
                return OrgTone.DANGER;
            case "PENDING_REVIEW":
                return OrgTone.WARNING;
            case "DRAFT":
                return OrgTone.NEUTRAL;
            case "APPROVED":
                return OrgTone.SUCCESS;
            default:
                return OrgTone.INFO;
        }
    }

    public String lifecycleLabel() {
        switch (lifecycleStatus) {
            case "UNPUBLISHED":
                return "Chưa phát hành";
            case "PUBLISHED":
                return "Đang bán";
            case "SUSPENDED":
                return "Tạm dừng";
            case "CANCELLED":
                return "Đã huỷ";
            case "COMPLETED":
                return "Đã kết thúc";
            default:
                return lifecycleStatus.isEmpty() ? "—" : lifecycleStatus;
        }
    }

    public int lifecycleTone() {
        switch (lifecycleStatus) {
            case "PUBLISHED":
                return OrgTone.SUCCESS;
            case "SUSPENDED":
                return OrgTone.WARNING;
            case "CANCELLED":
                return OrgTone.DANGER;
            case "UNPUBLISHED":
            case "COMPLETED":
                return OrgTone.NEUTRAL;
            default:
                return OrgTone.INFO;
        }
    }

    /** Ảnh bìa nền — backend chưa seed cover cho mọi sự kiện nên chọn theo id. */
    public int coverRes() {
        int index = (int) Math.abs(id % COVERS.length);
        return COVERS[index];
    }

    private static final int[] COVERS = {
            R.drawable.bg_org_cover_forest,
            R.drawable.bg_org_cover_navy,
            R.drawable.bg_org_cover_teal,
            R.drawable.bg_org_cover_violet,
            R.drawable.bg_org_cover_amber
    };

    /** Một hạng vé kèm tồn kho. */
    public static final class TicketType {
        public final long id;
        public final String name;
        public final String kind;
        public final long price;
        public final int sold;
        public final int total;
        public final Integer remaining;
        public final String status;

        public TicketType(long id, String name, String kind, long price, int sold, int total,
                          Integer remaining, String status) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.kind = kind == null ? "" : kind;
            this.price = price;
            this.sold = sold;
            this.total = total;
            this.remaining = remaining;
            this.status = status == null ? "" : status;
        }

        public int percent() {
            return total <= 0 ? 0 : Math.round(sold * 100f / total);
        }

        public String statusLabel() {
            switch (status) {
                case "PAUSED":
                    return "TẠM DỪNG";
                case "ENDED":
                    return "ĐÃ KẾT THÚC";
                case "ARCHIVED":
                    return "LƯU TRỮ";
                default:
                    break;
            }
            if (total > 0 && sold >= total) {
                return "HẾT VÉ";
            }
            return "ĐANG BÁN";
        }

        public int tone() {
            switch (status) {
                case "PAUSED":
                    return OrgTone.WARNING;
                case "ENDED":
                case "ARCHIVED":
                    return OrgTone.NEUTRAL;
                default:
                    break;
            }
            if (total > 0 && sold >= total) {
                return OrgTone.DANGER;
            }
            return OrgTone.SUCCESS;
        }
    }
}
