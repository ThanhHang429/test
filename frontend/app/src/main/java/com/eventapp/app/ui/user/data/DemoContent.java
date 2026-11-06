package com.eventapp.app.ui.user.data;

import com.eventapp.app.R;
import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.ui.model.TicketUiModel;
import com.eventapp.app.ui.user.model.DemoNotification;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Dữ liệu cục bộ cho hai phần <b>chưa có API</b>: vé đã mua và thông báo.
 *
 * <p>Sự kiện, yêu thích và sở thích đã chuyển sang backend nên không còn nằm ở đây.</p>
 */
public final class DemoContent {

    public static final String USER_NAME = "Trần Hoàng Long";
    public static final String USER_EMAIL = "user1@eticket.local";
    public static final String USER_PHONE = "0908 123 456";
    public static final String USER_MEMBER_SINCE = "Thành viên từ 03/2024";

    private static DemoContent instance;

    private final List<TicketUiModel> tickets = new ArrayList<>();
    private final List<DemoNotification> notifications = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private long nextTicketId = 100L;

    public static synchronized DemoContent get() {
        if (instance == null) {
            instance = new DemoContent();
        }
        return instance;
    }

    private DemoContent() {
        seed();
    }

    // ----------------------------------------------------------------- tickets

    public List<TicketUiModel> tickets() {
        return Collections.unmodifiableList(tickets);
    }

    public List<TicketUiModel> tickets(TicketUiModel.Status status) {
        List<TicketUiModel> result = new ArrayList<>();
        for (TicketUiModel ticket : tickets) {
            if (ticket.status == status) {
                result.add(ticket);
            }
        }
        return result;
    }

    public TicketUiModel ticket(String code) {
        for (TicketUiModel ticket : tickets) {
            if (ticket.code.equals(code)) {
                return ticket;
            }
        }
        return null;
    }

    public int ticketsByStatusCount(TicketUiModel.Status status) {
        int total = 0;
        for (TicketUiModel ticket : tickets) {
            if (ticket.status == status) {
                total += ticket.quantity;
            }
        }
        return total;
    }

    public int purchasedCount() {
        int total = 0;
        for (TicketUiModel ticket : tickets) {
            if (ticket.status != TicketUiModel.Status.CANCELLED) {
                total += ticket.quantity;
            }
        }
        return total;
    }

    /** Sinh vé mô phỏng sau khi người dùng xác nhận đặt vé (thanh toán chưa có API). */
    public TicketUiModel purchase(TicketUiModel.Builder snapshot) {
        String code = "ET-" + twoDigits((int) (Math.random() * 90) + 10)
                + "11-" + fourDigits();
        TicketUiModel ticket = snapshot
                .id(nextTicketId++)
                .code(code)
                .orderCode(code)
                .status(TicketUiModel.Status.UPCOMING)
                .build();
        tickets.add(0, ticket);
        notifyChanged();
        return ticket;
    }

    // ------------------------------------------------------------ notifications

    public List<DemoNotification> notifications() {
        return Collections.unmodifiableList(notifications);
    }

    public int unreadNotifications() {
        int count = 0;
        for (DemoNotification notification : notifications) {
            if (notification.unread) {
                count++;
            }
        }
        return count;
    }

    public void markAllNotificationsRead() {
        for (DemoNotification notification : notifications) {
            notification.unread = false;
        }
        notifyChanged();
    }

    // ---------------------------------------------------------------- listeners

    public void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyChanged() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }

    // -------------------------------------------------------------------- seed

    private void seed() {
        tickets.add(new TicketUiModel.Builder()
                .id(1)
                .eventId(2)
                .code("ET-2811-9843")
                .eventTitle("Vietnam Tech Summit 2026")
                .category(EventCategory.TECH)
                .ticketType("Vé Tiêu Chuẩn")
                .venue("SECC, 799 Nguyễn Văn Linh, Quận 7")
                .city("TP. Hồ Chí Minh")
                .startAt(at(7, 8, 30))
                .endAt(at(7, 17, 30))
                .quantity(2)
                .totalPrice(2_500_000)
                .orderCode("ET-2811-9843")
                .status(TicketUiModel.Status.UPCOMING)
                .build());

        tickets.add(new TicketUiModel.Builder()
                .id(2)
                .eventId(3)
                .code("ET-1512-8839")
                .eventTitle("Hội Thảo Khởi Nghiệp AI 2026")
                .category(EventCategory.BUSINESS)
                .ticketType("Standard Free Pass")
                .venue("Hội trường B, SECC, Quận 7")
                .city("TP. Hồ Chí Minh")
                .startAt(at(4, 14, 0))
                .endAt(at(4, 17, 0))
                .quantity(1)
                .totalPrice(0)
                .orderCode("ET-1512-8839")
                .status(TicketUiModel.Status.UPCOMING)
                .build());

        tickets.add(new TicketUiModel.Builder()
                .id(3)
                .eventId(98)
                .code("ET-0921-4410")
                .eventTitle("Meetup Design System 2026")
                .category(EventCategory.ART)
                .ticketType("Vé Tiêu Chuẩn")
                .venue("Dreamplex, Quận 3")
                .city("TP. Hồ Chí Minh")
                .startAt(at(-12, 18, 0))
                .endAt(at(-12, 21, 0))
                .quantity(1)
                .totalPrice(150_000)
                .orderCode("ET-0921-4410")
                .status(TicketUiModel.Status.USED)
                .build());

        tickets.add(new TicketUiModel.Builder()
                .id(4)
                .eventId(97)
                .code("ET-0311-7720")
                .eventTitle("Đêm Nhạc Acoustic Mùa Thu")
                .category(EventCategory.MUSIC)
                .ticketType("Vé Tiêu Chuẩn")
                .venue("Nhà hát Lớn Hà Nội")
                .city("Hà Nội")
                .startAt(at(9, 20, 0))
                .endAt(at(9, 22, 0))
                .quantity(1)
                .totalPrice(300_000)
                .orderCode("ET-0311-7720")
                .status(TicketUiModel.Status.CANCELLED)
                .build());

        notifications.add(new DemoNotification("n1",
                "Vé của bạn đã sẵn sàng",
                "Vietnam Tech Summit 2026 · 2 vé. Mở mã QR để check-in tại cổng.",
                System.currentTimeMillis() - 2 * 3600_000L,
                R.drawable.ic_user_ticket, R.color.user_primary_soft, R.color.user_primary, true));
        notifications.add(new DemoNotification("n2",
                "Sự kiện bạn lưu sắp diễn ra",
                "Đêm Nhạc Indie: Sài Gòn Mùa Đông còn 2 ngày nữa. Đừng bỏ lỡ!",
                System.currentTimeMillis() - 5 * 3600_000L,
                R.drawable.ic_user_heart, R.color.user_danger_soft, R.color.user_danger, true));
        notifications.add(new DemoNotification("n3",
                "Ưu đãi 20% cho lần đặt vé tiếp theo",
                "Nhập mã ETICKET20 khi thanh toán. Hạn sử dụng đến 30/11/2026.",
                System.currentTimeMillis() - 26 * 3600_000L,
                R.drawable.ic_ds_star_filled, R.color.user_warning_soft, R.color.user_warning_text, false));
        notifications.add(new DemoNotification("n4",
                "Nhắc nhở check-in",
                "Cổng mở trước giờ bắt đầu 60 phút. Vui lòng chuẩn bị mã vé.",
                System.currentTimeMillis() - 52 * 3600_000L,
                R.drawable.ic_user_clock, R.color.user_info_soft, R.color.user_info_text, false));
    }

    private static long at(int daysFromNow, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, daysFromNow);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static String fourDigits() {
        int value = (int) (Math.random() * 9000) + 1000;
        return String.valueOf(value);
    }
}
