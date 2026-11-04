package com.eventapp.app.ui.admin.data;

import com.eventapp.app.R;
import com.eventapp.app.ui.admin.model.AdminAccount;
import com.eventapp.app.ui.admin.model.AdminEventItem;
import com.eventapp.app.ui.admin.model.AdminNotification;
import com.eventapp.app.ui.admin.model.AdminReview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dữ liệu mẫu cho khu Quản trị — đủ nội dung để kiểm thử giao diện, chưa nối API.
 * Khi nối backend chỉ cần thay nguồn dữ liệu trong các fragment.
 */
public final class AdminDemo {

    private static final List<AdminReview> REVIEWS = Arrays.asList(
            new AdminReview("Vietnam AI & NextGen Innovation Summit 2025", "NextGen Ventures",
                    "TECH", "2.500 vé", "250k – 1,8tr", "Còn 1h45m",
                    "Hải Đăng", "HĐ", AdminReview.TONE_PRIORITY, R.drawable.bg_admin_cover_navy),
            new AdminReview("Indie Sunset Acoustic Fest 2025", "Acoustic Soul Lab",
                    "MUSIC", "800 vé", "150k – 350k", "Còn 22h",
                    "Chưa phân công", "—", AdminReview.TONE_WARNING, R.drawable.bg_admin_cover_amber),
            new AdminReview("Workshop Kỹ Năng Phỏng Vấn Big Tech", "TechCareer Youth Club",
                    "EDU", "300 vé", "Miễn phí", "Còn 13h",
                    "Thùy Lê", "TL", AdminReview.TONE_OVERDUE, R.drawable.bg_admin_cover_violet)
    );

    private static final List<AdminEventItem> EVENTS = Arrays.asList(
            new AdminEventItem("Đại Nhạc Hội EDM Neon Sound 2025", "StarLight Entertainment JSC",
                    "MUSIC", 1840, 2000, "1,65 tỷ ₫", "0",
                    "Chênh lệch tồn kho vé", AdminEventItem.TONE_LIVE,
                    R.drawable.bg_admin_cover_teal),
            new AdminEventItem("Vietnam AI & NextGen Innovation Summit 2025", "NextGen Ventures",
                    "TECH", 1720, 2500, "840 triệu ₫", "0",
                    "Tốc độ bán bất thường", AdminEventItem.TONE_FLAGGED,
                    R.drawable.bg_admin_cover_navy),
            new AdminEventItem("Hội Thảo Đầu Tư Blockchain 2025", "CryptoAlpha Hub",
                    "FINANCE", 429, 800, "210 triệu ₫", "0",
                    "Tạm dừng do vi phạm chính sách", AdminEventItem.TONE_PAUSED,
                    R.drawable.bg_admin_cover_violet),
            new AdminEventItem("Indie Sunset Acoustic Fest 2025", "Acoustic Soul Lab",
                    "MUSIC", 800, 800, "280 triệu ₫", "612",
                    "", AdminEventItem.TONE_LIVE, R.drawable.bg_admin_cover_amber)
    );

    private static final List<AdminAccount> USERS = Arrays.asList(
            new AdminAccount("USER-1042", "Nguyễn Thu Linh", "linh.nguyen@email.com", "TL",
                    "Tham gia 12/2024 · 24 vé đã mua", AdminAccount.TYPE_USER,
                    AdminAccount.STATUS_ACTIVE),
            new AdminAccount("USER-0987", "Trần Minh Khoa", "khoa.tran@email.com", "MK",
                    "3 báo cáo · gắn cờ 02/11", AdminAccount.TYPE_USER,
                    AdminAccount.STATUS_FLAGGED),
            new AdminAccount("USER-0771", "Phạm Gia Hân", "han.pham@email.com", "GH",
                    "Tạm khoá 30 ngày · 28/10", AdminAccount.TYPE_USER,
                    AdminAccount.STATUS_LOCKED),
            new AdminAccount("USER-1120", "Vũ Hoàng Nam", "nam.vu@email.com", "HN",
                    "Tham gia 09/2025 · 6 vé đã mua", AdminAccount.TYPE_USER,
                    AdminAccount.STATUS_ACTIVE)
    );

    private static final List<AdminAccount> ORGANIZERS = Arrays.asList(
            new AdminAccount("ORG-2041", "NextGen Ventures", "hello@nextgen.vn", "NV",
                    "12 sự kiện · KYC đã xác thực", AdminAccount.TYPE_ORG,
                    AdminAccount.STATUS_ACTIVE),
            new AdminAccount("ORG-1988", "StarLight Entertainment", "ops@starlight.vn", "SL",
                    "8 sự kiện · GMV 1,65 tỷ", AdminAccount.TYPE_ORG,
                    AdminAccount.STATUS_ACTIVE),
            new AdminAccount("ORG-2107", "CryptoAlpha Hub", "team@cryptoalpha.vn", "CA",
                    "2 báo cáo · chờ giải trình", AdminAccount.TYPE_ORG,
                    AdminAccount.STATUS_FLAGGED),
            new AdminAccount("ORG-1765", "Acoustic Soul Lab", "hi@acousticsoul.vn", "AS",
                    "Tạm dừng · hồ sơ chưa đạt", AdminAccount.TYPE_ORG,
                    AdminAccount.STATUS_LOCKED)
    );

    private static final List<AdminNotification> NOTIFICATIONS = Arrays.asList(
            new AdminNotification("Hồ sơ RV-8842 quá hạn SLA",
                    "Còn 45 phút để xử lý hồ sơ Vietnam AI Summit.",
                    "5 phút trước", AdminNotification.TYPE_MODERATION, true),
            new AdminNotification("Webhook thanh toán chậm 1,2s",
                    "Cổng thanh toán đang chậm hơn ngưỡng 1s, cần theo dõi.",
                    "18 phút trước", AdminNotification.TYPE_SYSTEM, true),
            new AdminNotification("Phí nền tảng tháng 11 đã chốt",
                    "428,6 triệu ₫ từ 24 nhà tổ chức, còn 42,6 triệu chờ thu.",
                    "1 giờ trước", AdminNotification.TYPE_FINANCE, false),
            new AdminNotification("3 tài khoản bị gắn cờ",
                    "Cần rà soát trong 24h trước khi tạm khoá.",
                    "2 giờ trước", AdminNotification.TYPE_ACCOUNT, true),
            new AdminNotification("2 báo cáo vi phạm mới",
                    "Báo cáo mức nghiêm trọng cho sự kiện EVT-8842.",
                    "Hôm qua", AdminNotification.TYPE_MODERATION, false)
    );

    private AdminDemo() {
    }

    public static List<AdminReview> reviews() {
        return new ArrayList<>(REVIEWS);
    }

    public static List<AdminEventItem> events() {
        return new ArrayList<>(EVENTS);
    }

    public static List<AdminAccount> users() {
        return new ArrayList<>(USERS);
    }

    public static List<AdminAccount> organizers() {
        return new ArrayList<>(ORGANIZERS);
    }

    public static List<AdminNotification> notifications() {
        return new ArrayList<>(NOTIFICATIONS);
    }
}
