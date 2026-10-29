package com.eventapp.app.ui.user.review;

import java.util.ArrayList;
import java.util.List;

/** Deterministic dummy reviews until FR-U20 is connected to the review API. */
public final class SampleReviewData {

    private static final long DAY = 24L * 60L * 60L * 1000L;

    private SampleReviewData() {
    }

    public static List<ReviewUiModel> forEvent(long eventId, String eventTitle) {
        long now = System.currentTimeMillis();
        List<ReviewUiModel> reviews = new ArrayList<>();
        reviews.add(new ReviewUiModel.Builder()
                .id(eventId * 100 + 1)
                .eventId(eventId)
                .eventTitle(eventTitle)
                .authorName("Minh Anh")
                .rating(5)
                .comment("Không gian được chuẩn bị kỹ, ban tổ chức hỗ trợ rất nhanh. "
                        + "Phần check-in QR chỉ mất chưa đến một phút.")
                .createdAt(now - 2 * DAY)
                .build());
        reviews.add(new ReviewUiModel.Builder()
                .id(eventId * 100 + 2)
                .eventId(eventId)
                .eventTitle(eventTitle)
                .authorName("Quốc Bảo")
                .rating(4)
                .comment("Nội dung hấp dẫn, đúng chủ đề. Âm thanh ở khu vực cuối hơi nhỏ "
                        + "nhưng tổng thể vẫn đáng tham dự.")
                .createdAt(now - 5 * DAY)
                .build());
        reviews.add(new ReviewUiModel.Builder()
                .id(eventId * 100 + 3)
                .eventId(eventId)
                .eventTitle(eventTitle)
                .authorName("Thu Hà")
                .rating(5)
                .comment("Rất đáng tiền. Mình sẽ tiếp tục theo dõi các sự kiện tiếp theo "
                        + "của nhà tổ chức này.")
                .createdAt(now - 9 * DAY)
                .build());
        reviews.add(new ReviewUiModel.Builder()
                .id(eventId * 100 + 4)
                .eventId(eventId)
                .eventTitle(eventTitle)
                .authorName("Hoàng Nam")
                .rating(3)
                .comment("Sự kiện ổn nhưng thời gian chờ ở cổng hơi lâu vào giờ cao điểm.")
                .createdAt(now - 14 * DAY)
                .build());
        reviews.add(new ReviewUiModel.Builder()
                .id(eventId * 100 + 5)
                .eventId(eventId)
                .eventTitle(eventTitle)
                .authorName("Lan Chi")
                .rating(4)
                .comment("Thông tin trên ứng dụng rõ ràng, địa điểm dễ tìm.")
                .createdAt(now - 21 * DAY)
                .build());
        return reviews;
    }

    public static ReviewUiModel mine(long eventId, String eventTitle, ReviewUiModel.Status status) {
        return new ReviewUiModel.Builder()
                .id(eventId * 100 + 99)
                .eventId(eventId)
                .eventTitle(eventTitle)
                .authorName("Minh Anh")
                .rating(5)
                .comment("Trải nghiệm rất tốt, ban tổ chức chuẩn bị chu đáo.")
                .createdAt(System.currentTimeMillis() - 3L * DAY)
                .status(status)
                .mine(true)
                .build();
    }

    public static int[] distribution(List<ReviewUiModel> reviews) {
        int[] values = new int[5];
        if (reviews == null) {
            return values;
        }
        for (ReviewUiModel review : reviews) {
            int index = Math.round(review.rating) - 1;
            if (index >= 0 && index < values.length) {
                values[index]++;
            }
        }
        return values;
    }

    public static float average(List<ReviewUiModel> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return 0f;
        }
        float sum = 0f;
        for (ReviewUiModel review : reviews) {
            sum += review.rating;
        }
        return sum / reviews.size();
    }

    public static List<ReviewUiModel> moderationQueue() {
        long now = System.currentTimeMillis();
        List<ReviewUiModel> reviews = new ArrayList<>();
        reviews.add(new ReviewUiModel.Builder()
                .id(9001)
                .eventId(1)
                .eventTitle("Đêm nhạc Indie Sài Gòn 2026")
                .authorName("Tấn Phát")
                .authorCode("USR-4821")
                .rating(5)
                .comment("Bán vé lại giá rẻ, liên hệ Zalo 0909 123 456 để nhận mã.")
                .createdAt(now - 40 * 60 * 1000L)
                .status(ReviewUiModel.Status.HELD)
                .source(ReviewUiModel.Source.HELD)
                .flagReason("Nội dung chứa số điện thoại và dấu hiệu mua bán vé ngoài hệ thống.")
                .evidence("liên hệ Zalo 0909 123 456")
                .severity("HIGH")
                .spamScore(92)
                .toxicityScore(12)
                .build());
        reviews.add(new ReviewUiModel.Builder()
                .id(9002)
                .eventId(2)
                .eventTitle("Hội thảo AI &amp; Tương lai 2026")
                .authorName("Ngọc Mai")
                .authorCode("USR-1109")
                .rating(1)
                .comment("Sự kiện dở tệ, ban tổ chức lừa đảo, đừng ai mua vé.")
                .createdAt(now - 2L * 60 * 60 * 1000L)
                .status(ReviewUiModel.Status.VISIBLE)
                .source(ReviewUiModel.Source.REPORTED)
                .flagReason("Bị 3 tài khoản báo cáo là nội dung xúc phạm, thiếu căn cứ.")
                .evidence("ban tổ chức lừa đảo")
                .severity("MEDIUM")
                .build());
        reviews.add(new ReviewUiModel.Builder()
                .id(9003)
                .eventId(4)
                .eventTitle("Triển lãm Sắc màu Đương đại")
                .authorName("Gia Huy")
                .authorCode("USR-7720")
                .rating(5)
                .comment("Tuyệt vời tuyệt vời tuyệt vời tuyệt vời tuyệt vời tuyệt vời!")
                .createdAt(now - 5L * 60 * 60 * 1000L)
                .status(ReviewUiModel.Status.VISIBLE)
                .source(ReviewUiModel.Source.FLAGGED)
                .flagReason("Nội dung lặp từ và nhiều dấu chấm than, nghi ngờ spam.")
                .evidence("tuyệt vời tuyệt vời tuyệt vời...")
                .severity("MEDIUM")
                .spamScore(78)
                .build());
        reviews.add(new ReviewUiModel.Builder()
                .id(9004)
                .eventId(1)
                .eventTitle("Đêm nhạc Indie Sài Gòn 2026")
                .authorName("Bảo Trân")
                .authorCode("USR-3356")
                .rating(4)
                .comment("Mình bị mất ví ở khu vực ghế ngồi, ai thấy liên hệ giúp mình nhé.")
                .createdAt(now - 26L * 60 * 60 * 1000L)
                .status(ReviewUiModel.Status.HELD)
                .source(ReviewUiModel.Source.HELD)
                .flagReason("Nội dung có thể chứa thông tin cá nhân của người khác.")
                .evidence("mất ví ở khu vực ghế ngồi")
                .severity("LOW")
                .build());
        return reviews;
    }
}
