package com.eventapp.app.ui.fake;


import com.eventapp.app.ui.model.ModerationUiModel;
import java.util.ArrayList;
import java.util.List;

/** Deterministic moderation queue until FR-A07/A14 are connected to the admin API. */
public final class SampleModerationData {

    private static final long MINUTE = 60L * 1000L;
    private static final long HOUR = 60L * MINUTE;
    private static final long DAY = 24L * HOUR;

    private SampleModerationData() {
    }

    public static int handledTodayBase() {
        return 7;
    }

    public static List<ModerationUiModel> createFlags() {
        long now = System.currentTimeMillis();
        List<ModerationUiModel> flags = new ArrayList<>();

        flags.add(new ModerationUiModel.Builder()
                .id(701)
                .targetType(ModerationUiModel.TargetType.EVENT_TEXT)
                .targetId(1)
                .targetName("Đêm nhạc Indie Sài Gòn 2026")
                .severity(ModerationUiModel.Severity.HIGH)
                .source(ModerationUiModel.Source.AI)
                .confidence(96)
                .reason("Nội dung chứa số điện thoại và dấu hiệu mua bán vé ngoài hệ thống.")
                .evidence("liên hệ Zalo 0909 123 456 để nhận mã")
                .createdAt(now - 12 * MINUTE)
                .build());

        flags.add(new ModerationUiModel.Builder()
                .id(702)
                .targetType(ModerationUiModel.TargetType.EVENT_IMAGE)
                .targetId(11)
                .targetName("Ảnh bìa sự kiện")
                .severity(ModerationUiModel.Severity.HIGH)
                .source(ModerationUiModel.Source.RULE)
                .confidence(100)
                .reason("Ảnh có tỷ lệ văn bản quá lớn và chứa số điện thoại liên hệ.")
                .evidence("Hotline 0912 888 999")
                .createdAt(now - 35 * MINUTE)
                .build());

        flags.add(new ModerationUiModel.Builder()
                .id(703)
                .targetType(ModerationUiModel.TargetType.TAG)
                .targetId(21)
                .targetName("#bán-vé-giá-rẻ")
                .severity(ModerationUiModel.Severity.MEDIUM)
                .source(ModerationUiModel.Source.RULE)
                .confidence(88)
                .reason("Nhãn trùng với cụm từ bán vé ngoài hệ thống.")
                .evidence("#bán-vé-giá-rẻ")
                .createdAt(now - 2 * HOUR)
                .unchecked(true)
                .build());

        flags.add(new ModerationUiModel.Builder()
                .id(704)
                .targetType(ModerationUiModel.TargetType.EVENT_TEXT)
                .targetId(2)
                .targetName("Hội thảo AI & Tương lai 2026")
                .severity(ModerationUiModel.Severity.MEDIUM)
                .source(ModerationUiModel.Source.AI)
                .confidence(84)
                .reason("Mô tả có dấu hiệu mâu thuẫn giữa vé miễn phí và phí tham dự.")
                .evidence("miễn phí tham dự, phí tài liệu 200.000đ")
                .createdAt(now - 3 * HOUR)
                .contentChanged(true)
                .build());

        flags.add(new ModerationUiModel.Builder()
                .id(705)
                .targetType(ModerationUiModel.TargetType.REVIEW)
                .targetId(9001)
                .targetName("Đánh giá của Tấn Phát")
                .severity(ModerationUiModel.Severity.LOW)
                .source(ModerationUiModel.Source.AI)
                .confidence(71)
                .reason("Đánh giá có thể chứa thông tin liên hệ cá nhân.")
                .evidence("Zalo 0909 123 456")
                .createdAt(now - 4 * HOUR)
                .build());

        flags.add(new ModerationUiModel.Builder()
                .id(706)
                .targetType(ModerationUiModel.TargetType.EVENT_TEXT)
                .targetId(4)
                .targetName("Triển lãm Sắc màu Đương đại")
                .severity(ModerationUiModel.Severity.HIGH)
                .source(ModerationUiModel.Source.RULE)
                .confidence(100)
                .reason("Tiêu đề chứa từ khóa bị cấm trong cấu hình kiểm duyệt.")
                .evidence("sắc màu đương đại")
                .createdAt(now - 6 * HOUR)
                .status(ModerationUiModel.Status.CONFIRMED)
                .reviewedAt(now - 1 * HOUR)
                .reviewNote("Đã xác nhận vi phạm.")
                .build());

        flags.add(new ModerationUiModel.Builder()
                .id(707)
                .targetType(ModerationUiModel.TargetType.EVENT_IMAGE)
                .targetId(12)
                .targetName("Ảnh minh họa #2")
                .severity(ModerationUiModel.Severity.MEDIUM)
                .source(ModerationUiModel.Source.AI)
                .confidence(67)
                .reason("Ảnh có khả năng chứa nội dung không phù hợp.")
                .evidence("image hash 9f2c…")
                .createdAt(now - 9 * HOUR)
                .status(ModerationUiModel.Status.DISMISSED)
                .reviewedAt(now - 2 * HOUR)
                .reviewNote("False positive, ảnh hợp lệ.")
                .build());

        flags.add(new ModerationUiModel.Builder()
                .id(708)
                .targetType(ModerationUiModel.TargetType.TAG)
                .targetId(22)
                .targetName("#sale-50")
                .severity(ModerationUiModel.Severity.LOW)
                .source(ModerationUiModel.Source.AI)
                .confidence(62)
                .reason("Nhãn có dấu hiệu giảm giá gây hiểu nhầm.")
                .evidence("#sale-50")
                .createdAt(now - 1 * DAY)
                .status(ModerationUiModel.Status.CONFIRMED)
                .reviewedAt(now - 1 * DAY)
                .reviewNote("Đã xác nhận.")
                .build());

        flags.add(new ModerationUiModel.Builder()
                .id(709)
                .targetType(ModerationUiModel.TargetType.EVENT_TEXT)
                .targetId(5)
                .targetName("Food Festival Đà Nẵng")
                .severity(ModerationUiModel.Severity.HIGH)
                .source(ModerationUiModel.Source.AI)
                .confidence(93)
                .reason("Nội dung quảng cáo dịch vụ ngoài phạm vi sự kiện.")
                .evidence("đặt bàn qua số 090…")
                .createdAt(now - 18 * MINUTE)
                .build());

        return flags;
    }
}
