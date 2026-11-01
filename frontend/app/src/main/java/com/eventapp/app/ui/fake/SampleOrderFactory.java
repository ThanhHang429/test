package com.eventapp.app.ui.fake;

import com.eventapp.app.ui.model.EventUiModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Sinh dữ liệu mẫu cho luồng mua vé và đăng ký miễn phí.
 *
 * <p>Giá vé, tên hạng vé và tồn kho được suy ra từ {@link EventUiModel} giống màn chi tiết sự kiện
 * để các màn hình demo có cùng một nguồn dữ liệu. Không có lời gọi API hay xử lý nghiệp vụ thật.</p>
 */
public final class SampleOrderFactory {

    private static final long SERVICE_FEE = 0L;
    private static final int MAX_PAID_QUANTITY = 10;
    private static final int MAX_FREE_QUANTITY = 2;
    private static final Random RANDOM = new Random();

    private SampleOrderFactory() {
    }

    public static final class TicketOption {
        public final long id;
        public final String name;
        public final String description;
        public final long price;
        public final int remaining;
        public final int maxPerOrder;

        TicketOption(long id, String name, String description, long price, int remaining,
                     int maxPerOrder) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.remaining = remaining;
            this.maxPerOrder = Math.max(1, maxPerOrder);
        }

        public boolean isFree() {
            return price <= 0L;
        }
    }

    public static EventUiModel findEvent(long eventId) {
        for (EventUiModel event : SampleEventData.createEvents()) {
            if (event.id == eventId) {
                return event;
            }
        }
        return null;
    }

    public static List<TicketOption> ticketOptions(EventUiModel event) {
        List<TicketOption> options = new ArrayList<>();
        if (event == null) {
            return options;
        }
        if (event.free) {
            options.add(new TicketOption(1L, "Vé miễn phí",
                    "Mỗi tài khoản đăng ký tối đa 2 vé.", 0L, 120, MAX_FREE_QUANTITY));
            return options;
        }

        long basePrice = Math.max(event.priceFrom, 100_000L);
        int available = event.remainingTickets > 0 ? event.remainingTickets : 48;
        options.add(new TicketOption(1L, "Vé Standard",
                "Quyền tham dự toàn bộ chương trình.", basePrice,
                Math.max(1, available), MAX_PAID_QUANTITY));
        options.add(new TicketOption(2L, "Vé Premium",
                "Ghế gần sân khấu, nước uống và quà tặng.", basePrice + 250_000L,
                8, MAX_PAID_QUANTITY));
        if (event.rating >= 4.7f) {
            options.add(new TicketOption(3L, "Vé VIP",
                    "Phòng chờ riêng, check-in ưu tiên và gặp gỡ diễn giả.",
                    basePrice + 600_000L, 4, MAX_PAID_QUANTITY));
        }
        return options;
    }

    public static DemoOrder createPendingOrder(EventUiModel event,
                                               TicketOption ticket,
                                               int quantity,
                                               String attendeeName,
                                               String attendeeEmail,
                                               String attendeePhone) {
        long total = ticket.price * Math.max(1, quantity) + SERVICE_FEE;
        return new DemoOrder(newOrderCode(), event.id, event.title, event.venue, event.city,
                event.startAt, attendeeName, attendeeEmail, attendeePhone, ticket.name,
                Math.max(1, quantity), ticket.price, total, DemoOrder.State.PENDING_PAYMENT,
                System.currentTimeMillis(), "Chuyển khoản VietQR", newTicketCode(false));
    }

    public static DemoOrder createFreeOrder(EventUiModel event,
                                            int quantity,
                                            String attendeeName,
                                            String attendeeEmail,
                                            String attendeePhone) {
        return new DemoOrder(newOrderCode(), event.id, event.title, event.venue, event.city,
                event.startAt, attendeeName, attendeeEmail, attendeePhone, "Vé miễn phí",
                Math.max(1, quantity), 0L, 0L, DemoOrder.State.FREE_CONFIRMED,
                System.currentTimeMillis(), "", newTicketCode(true));
    }

    private static String newOrderCode() {
        String day = new SimpleDateFormat("yyMMdd", Locale.US).format(new Date());
        return "ORD-" + day + "-" + (100 + RANDOM.nextInt(900));
    }

    private static String newTicketCode(boolean free) {
        int serial = 100_000 + RANDOM.nextInt(900_000);
        return free ? "ETK-FREE-" + serial : "ETK-PASS-" + serial;
    }
}
