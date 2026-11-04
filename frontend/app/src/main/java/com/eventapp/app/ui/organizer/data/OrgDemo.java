package com.eventapp.app.ui.organizer.data;

import com.eventapp.app.R;
import com.eventapp.app.ui.organizer.model.OrgEvent;
import com.eventapp.app.ui.organizer.model.OrgOrder;
import com.eventapp.app.ui.organizer.model.OrgTicketType;
import com.eventapp.app.ui.organizer.model.OrgTone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Dữ liệu mẫu cho khu Nhà tổ chức — đủ nội dung để kiểm thử giao diện, chưa nối API. */
public final class OrgDemo {

    private static final List<OrgEvent> EVENTS = Arrays.asList(
            new OrgEvent("VTE-2025", "Vietnam Tech Expo 2025", "CÔNG NGHỆ & AI",
                    "15–18/11/2025 · 09:00", "SECC, Quận 7 · TP.HCM",
                    OrgEvent.FILTER_ON_SALE, "ĐANG BÁN", OrgTone.SUCCESS,
                    R.drawable.bg_org_cover_forest, 1720, 2000, 1_462_000_000L, 1240),
            new OrgEvent("TSN-2025", "Tech Startup Night 2025", "KHỞI NGHIỆP",
                    "04/12/2025 · 18:30", "The Hive, Thảo Điền · TP.HCM",
                    OrgEvent.FILTER_PENDING, "CHỜ DUYỆT", OrgTone.WARNING,
                    R.drawable.bg_org_cover_navy, 0, 300, 0L, 0),
            new OrgEvent("WGD-2025", "Workshop GenAI for Developers", "ĐÀO TẠO & WORKSHOP",
                    "22/11/2025 · 14:00", "Trực tuyến · Zoom Pro",
                    OrgEvent.FILTER_DRAFT, "BẢN NHÁP", OrgTone.NEUTRAL,
                    R.drawable.bg_org_cover_violet, 0, 150, 0L, 0),
            new OrgEvent("DLS-2025", "Deep Learning Summit 2025", "HỘI NGHỊ QUỐC TẾ",
                    "10–11/10/2025 · 08:30", "JW Marriott · Hà Nội",
                    OrgEvent.FILTER_ENDED, "ĐÃ KẾT THÚC", OrgTone.NEUTRAL,
                    R.drawable.bg_org_cover_amber, 580, 600, 812_000_000L, 552),
            new OrgEvent("FCA-2025", "Fintech Connect Asia 2025", "TÀI CHÍNH & NGÂN HÀNG",
                    "28–30/10/2025 · 09:00", "GEM Center, Quận 1 · TP.HCM",
                    OrgEvent.FILTER_ON_SALE, "ĐANG BÁN", OrgTone.SUCCESS,
                    R.drawable.bg_org_cover_teal, 342, 800, 1_180_000_000L, 210)
    );

    private static final List<OrgOrder> ORDERS = Arrays.asList(
            new OrgOrder("ORD-2025-98412", 3_700_000L, OrgOrder.FILTER_PAID,
                    "ĐÃ THANH TOÁN", OrgTone.SUCCESS, "Nguyễn Văn A",
                    "Vietnam Tech Expo 2025", "2x VIP All-Access",
                    "10:42 · 16/10/2025", "VietQR · Đã đối soát", "0987***210"),
            new OrgOrder("ORD-2025-98390", 650_000L, OrgOrder.FILTER_PENDING,
                    "CHỜ XỬ LÝ", OrgTone.WARNING, "Trần Thị B",
                    "Tech Startup Night 2025", "1x Standard Entry",
                    "10:35 · 16/10/2025", "VietQR · Chờ webhook", "0911***884"),
            new OrgOrder("ORD-2025-97815", 1_850_000L, OrgOrder.FILTER_REFUND,
                    "HOÀN TIỀN", OrgTone.DANGER, "Lê Hoàng C",
                    "Vietnam Tech Expo 2025", "1x VIP All-Access",
                    "08:15 · 15/10/2025", "Thẻ ATM · Đã hoàn", "0905***552"),
            new OrgOrder("ORD-2025-96501", 0L, OrgOrder.FILTER_PAID,
                    "MIỄN PHÍ", OrgTone.SUCCESS, "Phạm Duy K",
                    "Workshop GenAI for Developers", "1x Speaker Pass",
                    "14:10 · 14/10/2025", "Free pass", "0934***441"),
            new OrgOrder("ORD-2025-95120", 2_400_000L, OrgOrder.FILTER_PAID,
                    "ĐÃ THANH TOÁN", OrgTone.SUCCESS, "Vũ Minh E",
                    "Fintech Connect Asia 2025", "2x Standard",
                    "09:20 · 14/10/2025", "VietQR · Đã đối soát", "0977***120")
    );

    private OrgDemo() {
    }

    public static List<OrgEvent> events() {
        return EVENTS;
    }

    public static List<OrgOrder> orders() {
        return ORDERS;
    }

    public static OrgEvent findEvent(String code) {
        for (OrgEvent event : EVENTS) {
            if (event.code.equals(code)) {
                return event;
            }
        }
        return EVENTS.get(0);
    }

    public static OrgOrder findOrder(String code) {
        for (OrgOrder order : ORDERS) {
            if (order.code.equals(code)) {
                return order;
            }
        }
        return ORDERS.get(0);
    }

    /** Chia hạng vé theo tỷ lệ để màn chi tiết có dữ liệu nhất quán với số vé đã bán. */
    public static List<OrgTicketType> ticketTypes(OrgEvent event) {
        if (event == null) {
            return Collections.emptyList();
        }
        int totalStandard = Math.max(1, Math.round(event.total * 0.60f));
        int totalVip = Math.max(1, Math.round(event.total * 0.20f));
        int totalGuest = Math.max(0, event.total - totalStandard - totalVip);

        int soldVip = Math.min(totalVip, Math.round(event.sold * 0.25f));
        int soldStandard = Math.min(totalStandard, Math.round(event.sold * 0.55f));
        int soldGuest = Math.min(totalGuest, Math.max(0, event.sold - soldVip - soldStandard));
        int leftover = event.sold - soldVip - soldStandard - soldGuest;
        if (leftover > 0) {
            soldStandard += Math.min(leftover, totalStandard - soldStandard);
        }

        boolean closed = OrgEvent.FILTER_ENDED.equals(event.filter);
        boolean paused = OrgEvent.FILTER_DRAFT.equals(event.filter)
                || OrgEvent.FILTER_PENDING.equals(event.filter);

        List<OrgTicketType> types = new ArrayList<>(3);
        types.add(new OrgTicketType("Vé Tiêu Chuẩn", 450_000L, soldStandard, totalStandard,
                typeStatus(soldStandard, totalStandard, closed, paused),
                typeTone(soldStandard, totalStandard, closed, paused)));
        types.add(new OrgTicketType("Vé VIP All-Access", 1_850_000L, soldVip, totalVip,
                typeStatus(soldVip, totalVip, closed, paused),
                typeTone(soldVip, totalVip, closed, paused)));
        types.add(new OrgTicketType("Vé Khách Mời", 0L, soldGuest, totalGuest,
                paused || closed ? typeStatus(soldGuest, totalGuest, closed, paused) : "MIỄN PHÍ",
                paused || closed ? typeTone(soldGuest, totalGuest, closed, paused) : OrgTone.INFO));
        return types;
    }

    private static String typeStatus(int sold, int total, boolean closed, boolean paused) {
        if (closed) {
            return "ĐÃ KẾT THÚC";
        }
        if (paused) {
            return "TẠM DỪNG";
        }
        if (total > 0 && sold >= total) {
            return "HẾT VÉ";
        }
        if (total > 0 && sold * 100f / total >= 95f) {
            return "SẮP HẾT";
        }
        return "ĐANG BÁN";
    }

    private static int typeTone(int sold, int total, boolean closed, boolean paused) {
        if (closed || paused) {
            return OrgTone.NEUTRAL;
        }
        if (total > 0 && sold >= total) {
            return OrgTone.DANGER;
        }
        if (total > 0 && sold * 100f / total >= 95f) {
            return OrgTone.WARNING;
        }
        return OrgTone.SUCCESS;
    }
}
