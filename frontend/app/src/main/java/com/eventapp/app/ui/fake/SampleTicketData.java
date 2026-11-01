package com.eventapp.app.ui.fake;

import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.ui.model.TicketUiModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Vé mẫu dùng tạm cho FR-U14/FR-U18 tới khi API đơn hàng được nối. */
public final class SampleTicketData {

    private static final long DAY = 24L * 60L * 60L * 1000L;

    private SampleTicketData() {
    }

    public static List<TicketUiModel> createTickets() {
        // Neo về 00:00 hôm nay để vé hiển thị giờ tròn, khớp định dạng "T7, 16/11 • 19:00".
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        long now = midnight.getTimeInMillis();
        List<TicketUiModel> tickets = new ArrayList<>();

        tickets.add(new TicketUiModel.Builder()
                .id(501)
                .eventId(1)
                .eventTitle("Đêm nhạc Indie Sài Gòn 2026")
                .category(EventCategory.MUSIC)
                .ticketType("Vé VIP")
                .venue("Nhà hát Hòa Bình")
                .city("TP. Hồ Chí Minh")
                .startAt(now + 2 * DAY + 19 * 3600_000L)
                .quantity(2)
                .totalPrice(1_100_000L)
                .orderCode("ETK-24118")
                .status(TicketUiModel.Status.UPCOMING)
                .build());

        tickets.add(new TicketUiModel.Builder()
                .id(502)
                .eventId(2)
                .eventTitle("Hội thảo AI & Tương lai 2026")
                .category(EventCategory.TECH)
                .ticketType("Vé Online")
                .venue("Trực tuyến")
                .city("TP. Hồ Chí Minh")
                .startAt(now + 5 * DAY + 9 * 3600_000L)
                .quantity(1)
                .totalPrice(0L)
                .orderCode("ETK-24119")
                .status(TicketUiModel.Status.UPCOMING)
                .build());

        tickets.add(new TicketUiModel.Builder()
                .id(503)
                .eventId(3)
                .eventTitle("Giải chạy Marathon Quốc tế")
                .category(EventCategory.SPORTS)
                .ticketType("Vé Standard")
                .venue("Công viên 23/9")
                .city("TP. Hồ Chí Minh")
                .startAt(now + 9 * DAY + 5 * 3600_000L)
                .quantity(1)
                .totalPrice(450_000L)
                .orderCode("ETK-24120")
                .status(TicketUiModel.Status.UPCOMING)
                .build());

        tickets.add(new TicketUiModel.Builder()
                .id(504)
                .eventId(9)
                .eventTitle("Đêm nhạc Acoustic Hà Nội")
                .category(EventCategory.MUSIC)
                .ticketType("Vé GA")
                .venue("Nhà hát Lớn Hà Nội")
                .city("Hà Nội")
                .startAt(now - 12 * DAY + 20 * 3600_000L)
                .quantity(2)
                .totalPrice(700_000L)
                .orderCode("ETK-23981")
                .status(TicketUiModel.Status.PAST)
                .build());

        tickets.add(new TicketUiModel.Builder()
                .id(505)
                .eventId(7)
                .eventTitle("Workshop Nhiếp ảnh Mobile")
                .category(EventCategory.EDUCATION)
                .ticketType("Vé tham dự")
                .venue("Vườn ươm Sáng tạo")
                .city("TP. Hồ Chí Minh")
                .startAt(now - 26 * DAY + 14 * 3600_000L)
                .quantity(1)
                .totalPrice(250_000L)
                .orderCode("ETK-23877")
                .status(TicketUiModel.Status.PAST)
                .build());

        tickets.add(new TicketUiModel.Builder()
                .id(506)
                .eventId(12)
                .eventTitle("Triển lãm Công nghệ 2026")
                .category(EventCategory.TECH)
                .ticketType("Vé Online")
                .venue("Trực tuyến")
                .city("TP. Hồ Chí Minh")
                .startAt(now - 40 * DAY + 9 * 3600_000L)
                .quantity(1)
                .totalPrice(0L)
                .orderCode("ETK-23702")
                .status(TicketUiModel.Status.CANCELLED)
                .build());

        return tickets;
    }
}
