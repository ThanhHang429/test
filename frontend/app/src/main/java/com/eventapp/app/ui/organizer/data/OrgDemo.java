package com.eventapp.app.ui.organizer.data;

import com.eventapp.app.ui.organizer.model.OrgOrder;
import com.eventapp.app.ui.organizer.model.OrgTone;

import java.util.Arrays;
import java.util.List;

/**
 * Dữ liệu mẫu cho phần chưa có API.
 *
 * <p>Sự kiện đã chuyển sang {@code OrganizerRepository} (FR-O02) nên chỉ còn đơn hàng —
 * backend chưa có endpoint danh sách đơn cho organizer.</p>
 */
public final class OrgDemo {

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

    public static List<OrgOrder> orders() {
        return ORDERS;
    }

    public static OrgOrder findOrder(String code) {
        for (OrgOrder order : ORDERS) {
            if (order.code.equals(code)) {
                return order;
            }
        }
        return ORDERS.get(0);
    }
}
