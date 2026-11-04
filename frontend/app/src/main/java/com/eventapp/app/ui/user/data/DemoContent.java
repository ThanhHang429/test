package com.eventapp.app.ui.user.data;

import com.eventapp.app.R;
import com.eventapp.app.ui.user.model.DemoEvent;
import com.eventapp.app.ui.user.model.DemoNotification;
import com.eventapp.app.ui.user.model.DemoTicket;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nguồn dữ liệu mẫu cho khu người dùng.
 *
 * <p>Dữ liệu nằm trong bộ nhớ để test giao diện: lưu sự kiện, đặt vé và thông báo đều có tác dụng
 * ngay trong phiên chạy. Khi nối backend, thay các lời gọi này bằng repository tương ứng.</p>
 */
public final class DemoContent {

    public static final String DEMO_EMAIL = "demo@nexus.vn";
    public static final String DEMO_PASSWORD = "123456";
    public static final String USER_NAME = "Trần Hoàng Long";
    public static final String USER_EMAIL = DEMO_EMAIL;
    public static final String USER_PHONE = "0908 123 456";
    public static final String USER_MEMBER_SINCE = "Thành viên từ 03/2024";

    private static DemoContent instance;

    private final List<DemoEvent> events = new ArrayList<>();
    private final Set<Long> savedIds = new HashSet<>();
    private final List<DemoTicket> tickets = new ArrayList<>();
    private final List<DemoNotification> notifications = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public static synchronized DemoContent get() {
        if (instance == null) {
            instance = new DemoContent();
        }
        return instance;
    }

    private DemoContent() {
        seed();
    }

    // ------------------------------------------------------------------ events

    public List<DemoEvent> events() {
        return Collections.unmodifiableList(events);
    }

    public List<DemoEvent> featured() {
        List<DemoEvent> result = new ArrayList<>(events);
        result.sort(Comparator.comparingLong(e -> e.startAt));
        return result.size() > 4 ? new ArrayList<>(result.subList(0, 4)) : result;
    }

    public List<DemoEvent> freeEvents() {
        List<DemoEvent> result = new ArrayList<>();
        for (DemoEvent event : events) {
            if (event.isFree()) {
                result.add(event);
            }
        }
        return result;
    }

    public DemoEvent event(long id) {
        for (DemoEvent event : events) {
            if (event.id == id) {
                return event;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------- saved

    public boolean isSaved(long eventId) {
        return savedIds.contains(eventId);
    }

    /** Trả về trạng thái lưu sau khi đổi. */
    public boolean toggleSaved(long eventId) {
        boolean saved;
        if (savedIds.contains(eventId)) {
            savedIds.remove(eventId);
            saved = false;
        } else {
            savedIds.add(eventId);
            saved = true;
        }
        notifyChanged();
        return saved;
    }

    public List<DemoEvent> savedEvents() {
        List<DemoEvent> result = new ArrayList<>();
        for (DemoEvent event : events) {
            if (savedIds.contains(event.id)) {
                result.add(event);
            }
        }
        result.sort(Comparator.comparingLong(e -> e.startAt));
        return result;
    }

    public int savedCount() {
        return savedIds.size();
    }

    // ----------------------------------------------------------------- tickets

    public List<DemoTicket> tickets() {
        return Collections.unmodifiableList(tickets);
    }

    public List<DemoTicket> tickets(DemoTicket.Status status) {
        List<DemoTicket> result = new ArrayList<>();
        for (DemoTicket ticket : tickets) {
            if (ticket.status == status) {
                result.add(ticket);
            }
        }
        return result;
    }

    public DemoTicket ticket(String code) {
        for (DemoTicket ticket : tickets) {
            if (ticket.code.equals(code)) {
                return ticket;
            }
        }
        return null;
    }

    public int ticketsByStatusCount(DemoTicket.Status status) {
        int total = 0;
        for (DemoTicket ticket : tickets) {
            if (ticket.status == status) {
                total += ticket.quantity;
            }
        }
        return total;
    }

    public int purchasedCount() {
        int total = 0;
        for (DemoTicket ticket : tickets) {
            if (ticket.status != DemoTicket.Status.CANCELLED) {
                total += ticket.quantity;
            }
        }
        return total;
    }

    public DemoTicket purchase(DemoEvent event, String typeName, int quantity, long total) {
        String code = "NX-" + twoDigits((int) (Math.random() * 90) + 10)
                + "11-" + fourDigits();
        DemoTicket ticket = new DemoTicket(code, event, typeName, quantity, total,
                System.currentTimeMillis(), DemoTicket.Status.UPCOMING);
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
        DemoEvent indie = new DemoEvent.Builder()
                .id(1)
                .title("Đêm Nhạc Indie: Sài Gòn Mùa Đông")
                .category("music", "Âm nhạc")
                .organizer("Nexus Live Studio")
                .venue("Nhà hát Hòa Bình, Quận 10", "TP. Hồ Chí Minh")
                .time(at(2, 19, 30), at(2, 22, 0))
                .priceFrom(450_000)
                .remaining(120)
                .cover(R.drawable.bg_user_cover_indigo, R.drawable.ic_ds_mic)
                .tag("Bán chạy")
                .description("Một đêm nhạc dành cho những ai yêu chất giọng mộc mạc và không gian "
                        + "ấm cúng. Chương trình quy tụ 5 ban nhạc indie nổi bật cùng phần trình "
                        + "diễn ánh sáng được thiết kế riêng cho khán phòng Hòa Bình.")
                .popularTicket("Vé Tiêu Chuẩn", "Khu vực khán đài A", 450_000, 80)
                .ticketType("Vé VIP", "Hàng ghế đầu, kèm poster giới hạn", 890_000, 24)
                .ticketType("Early Bird", "Đã bán hết", 350_000, 0)
                .build();

        DemoEvent tech = new DemoEvent.Builder()
                .id(2)
                .title("Vietnam Tech Summit 2026")
                .category("tech", "Công nghệ")
                .organizer("Nexus Technology")
                .venue("SECC, 799 Nguyễn Văn Linh, Quận 7", "TP. Hồ Chí Minh")
                .time(at(7, 8, 30), at(7, 17, 30))
                .priceFrom(1_250_000)
                .remaining(34)
                .cover(R.drawable.bg_user_cover_violet, R.drawable.ic_ds_ai_sparkle)
                .tag("Sắp diễn ra")
                .description("Hội nghị công nghệ lớn nhất năm với 40 diễn giả đến từ các công ty "
                        + "AI hàng đầu châu Á. Nội dung tập trung vào Generative AI, hạ tầng dữ "
                        + "liệu và kỹ năng cho kỷ nguyên tự động hóa.")
                .popularTicket("Vé Tiêu Chuẩn", "Bao gồm toàn bộ phiên tham luận", 1_250_000, 30)
                .ticketType("Vé VIP Premium", "Khu vực VIP, networking lunch", 2_400_000, 0)
                .ticketType("Early Bird", "Đã bán hết", 950_000, 0)
                .build();

        DemoEvent startup = new DemoEvent.Builder()
                .id(3)
                .title("Hội Thảo Khởi Nghiệp AI 2026")
                .category("business", "Hội thảo")
                .organizer("Aura Innovation Hub")
                .venue("Hội trường B, SECC, Quận 7", "TP. Hồ Chí Minh")
                .time(at(4, 14, 0), at(4, 17, 0))
                .priceFrom(0)
                .remaining(60)
                .cover(R.drawable.bg_user_cover_teal, R.drawable.ic_ds_users)
                .tag("Miễn phí")
                .description("Chuỗi chia sẻ thực chiến từ các founder đã gọi vốn thành công. "
                        + "Bạn sẽ được hướng dẫn cách xây dựng sản phẩm AI tối giản, tìm khách "
                        + "hàng đầu tiên và chuẩn bị hồ sơ gọi vốn.")
                .popularTicket("Standard Free Pass", "Tham dự toàn bộ phiên", 0, 60)
                .ticketType("VIP Networking", "Khu vực gặp gỡ diễn giả", 0, 10)
                .build();

        DemoEvent art = new DemoEvent.Builder()
                .id(4)
                .title("Triển Lãm Ánh Sáng: Vũ Trụ Trong Tôi")
                .category("art", "Nghệ thuật")
                .organizer("Studio Lumière")
                .venue("Bảo tàng Mỹ thuật TP.HCM, Quận 1", "TP. Hồ Chí Minh")
                .time(at(11, 10, 0), at(11, 21, 0))
                .priceFrom(180_000)
                .remaining(80)
                .cover(R.drawable.bg_user_cover_coral, R.drawable.ic_ds_image)
                .tag("Mới")
                .description("Không gian sắp đặt ánh sáng tương tác gồm 8 phòng, mỗi phòng là một "
                        + "lát cắt cảm xúc khác nhau. Triển lãm phù hợp cho những buổi hẹn cuối "
                        + "tuần và những ai yêu thích nhiếp ảnh.")
                .popularTicket("Vé Tiêu Chuẩn", "Tham quan tự do", 180_000, 70)
                .ticketType("Vé có hướng dẫn", "Kèm hướng dẫn viên 45 phút", 320_000, 10)
                .build();

        DemoEvent run = new DemoEvent.Builder()
                .id(5)
                .title("Giải Chạy Vì Cộng Đồng 2026")
                .category("sports", "Thể thao")
                .organizer("Nexus Community")
                .venue("Công viên Tao Đàn, Quận 1", "TP. Hồ Chí Minh")
                .time(at(14, 6, 0), at(14, 10, 0))
                .priceFrom(250_000)
                .remaining(200)
                .cover(R.drawable.bg_user_cover_amber, R.drawable.ic_ds_event)
                .tag("Sắp diễn ra")
                .description("Cung đường 5km và 10km xuyên qua trung tâm thành phố. Toàn bộ lợi "
                        + "nhuận được gửi tới quỹ hỗ trợ trẻ em vùng cao. Mỗi vận động viên nhận "
                        + "áo thi đấu, huy chương và bữa sáng nhẹ.")
                .popularTicket("Cự ly 5km", "Bao gồm áo và huy chương", 250_000, 150)
                .ticketType("Cự ly 10km", "Bao gồm áo, huy chương và bữa sáng", 450_000, 50)
                .build();

        DemoEvent workshop = new DemoEvent.Builder()
                .id(6)
                .title("Workshop Kể Chuyện Bằng Hình Ảnh")
                .category("education", "Giáo dục")
                .organizer("The Factory Contemporary")
                .venue("The Factory, 15 Nguyễn Ư Dĩ, Quận 1", "TP. Hồ Chí Minh")
                .time(at(18, 9, 0), at(18, 12, 0))
                .priceFrom(320_000)
                .remaining(24)
                .cover(R.drawable.bg_user_cover_navy, R.drawable.ic_ds_event)
                .tag("Còn ít chỗ")
                .description("Buổi workshop dành cho người mới bắt đầu nhiếp ảnh kể chuyện. Bạn "
                        + "sẽ thực hành ngay tại chỗ với máy ảnh hoặc điện thoại và nhận góp ý "
                        + "trực tiếp từ giảng viên.")
                .popularTicket("Vé Tiêu Chuẩn", "Bao gồm tài liệu và đồ uống", 320_000, 24)
                .build();

        DemoEvent food = new DemoEvent.Builder()
                .id(7)
                .title("Food Tour Chợ Lớn: Vị Nhớ Sài Gòn")
                .category("food", "Ẩm thực")
                .organizer("Saigon Food Map")
                .venue("Chợ Bình Tây, Quận 6", "TP. Hồ Chí Minh")
                .time(at(1, 17, 0), at(1, 20, 30))
                .priceFrom(199_000)
                .remaining(12)
                .cover(R.drawable.bg_user_cover_sky, R.drawable.ic_ds_star_filled)
                .tag("Sắp diễn ra")
                .description("Đi bộ qua 6 điểm ăn nổi tiếng trong khu Chợ Lớn cùng hướng dẫn "
                        + "viên địa phương. Thực đơn thay đổi theo ngày và phù hợp cho cả người "
                        + "ăn chay.")
                .popularTicket("Vé Tiêu Chuẩn", "6 món và nước uống", 199_000, 10)
                .ticketType("Combo 2 người", "Tiết kiệm 10%", 350_000, 2)
                .build();

        events.add(food);
        events.add(indie);
        events.add(startup);
        events.add(tech);
        events.add(art);
        events.add(run);
        events.add(workshop);
        events.sort(Comparator.comparingLong(e -> e.startAt));

        savedIds.add(tech.id);
        savedIds.add(art.id);
        savedIds.add(startup.id);

        DemoEvent past = new DemoEvent.Builder()
                .id(98)
                .title("Meetup Design System 2026")
                .category("art", "Nghệ thuật")
                .organizer("Nexus Design Guild")
                .venue("Dreamplex, Quận 3", "TP. Hồ Chí Minh")
                .time(at(-12, 18, 0), at(-12, 21, 0))
                .priceFrom(150_000)
                .remaining(0)
                .cover(R.drawable.bg_user_cover_coral, R.drawable.ic_ds_image)
                .tag("Đã kết thúc")
                .description("Buổi gặp gỡ cộng đồng thiết kế sản phẩm.")
                .ticketType("Vé Tiêu Chuẩn", "", 150_000, 0)
                .build();

        DemoEvent cancelled = new DemoEvent.Builder()
                .id(97)
                .title("Đêm Nhạc Acoustic Mùa Thu")
                .category("music", "Âm nhạc")
                .organizer("Nexus Live Studio")
                .venue("Nhà hát Lớn Hà Nội", "Hà Nội")
                .time(at(9, 20, 0), at(9, 22, 0))
                .priceFrom(300_000)
                .remaining(0)
                .cover(R.drawable.bg_user_cover_violet, R.drawable.ic_ds_mic)
                .tag("Đã huỷ")
                .description("Sự kiện đã bị huỷ bởi nhà tổ chức.")
                .ticketType("Vé Tiêu Chuẩn", "", 300_000, 0)
                .build();

        tickets.add(new DemoTicket("NX-2811-9843", tech, "Vé Tiêu Chuẩn", 2, 2_500_000,
                at(-3, 9, 12), DemoTicket.Status.UPCOMING));
        tickets.add(new DemoTicket("FREE-1512-8839", startup, "Standard Free Pass", 1, 0,
                at(-1, 15, 40), DemoTicket.Status.UPCOMING));
        tickets.add(new DemoTicket("DS-0921-4410", past, "Vé Tiêu Chuẩn", 1, 150_000,
                at(-20, 11, 0), DemoTicket.Status.USED));
        tickets.add(new DemoTicket("AC-0311-7720", cancelled, "Vé Tiêu Chuẩn", 1, 300_000,
                at(-6, 20, 15), DemoTicket.Status.CANCELLED));

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
                "Nhập mã NEXUS20 khi thanh toán. Hạn sử dụng đến 30/11/2026.",
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
