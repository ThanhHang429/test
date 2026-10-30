package com.eventapp.app.ui.organizer.report;

import com.eventapp.app.ui.common.view.PriceTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * API-18 fixtures for FR-O27. Three profiles cover the spec branches: a full AI analysis, the
 * rule-based fallback (AI unavailable) and the insufficient-data branch.
 */
public final class SampleInsightData {

    private SampleInsightData() {
    }

    public static List<AiInsightUiModel.EventOption> createEvents() {
        List<AiInsightUiModel.EventOption> events = new ArrayList<>();
        events.add(new AiInsightUiModel.EventOption(1L, "Đêm nhạc Indie Sài Gòn 2026"));
        events.add(new AiInsightUiModel.EventOption(2L, "Hội thảo AI & Tương lai 2026"));
        events.add(new AiInsightUiModel.EventOption(3L, "Giải chạy Marathon Quốc tế"));
        events.add(new AiInsightUiModel.EventOption(5L, "Workshop Gốm cuối tuần"));
        return events;
    }

    public static AiInsightUiModel.Analysis create(long eventId) {
        if (eventId == 5L) {
            return workshop();
        }
        if (eventId == 2L) {
            return ruleFallback();
        }
        return indie();
    }

    private static AiInsightUiModel.Analysis indie() {
        long now = System.currentTimeMillis();
        double registrations = 842;
        double fillRate = 72.4;
        int daysLeft = 4;
        double rating = 4.8;

        List<AiInsightUiModel.Metric> metrics = new ArrayList<>();
        metrics.add(new AiInsightUiModel.Metric(AiInsightUiModel.MetricKey.REGISTRATIONS,
                registrations, "1.163 chỗ"));
        metrics.add(new AiInsightUiModel.Metric(AiInsightUiModel.MetricKey.FILL_RATE,
                fillRate, null));
        metrics.add(new AiInsightUiModel.Metric(AiInsightUiModel.MetricKey.DAYS_LEFT,
                daysLeft, null));
        metrics.add(new AiInsightUiModel.Metric(AiInsightUiModel.MetricKey.RATING,
                rating, "128 đánh giá"));

        List<String> summary = new ArrayList<>();
        summary.add(String.format(new Locale("vi", "VN"),
                "Sự kiện đã có %s đăng ký, đạt %s%% sức chứa và còn %d ngày trước giờ bắt đầu. "
                        + "Nhu cầu tăng rõ trong những ngày gần đây.",
                PriceTextView.formatPlain(Math.round(registrations)),
                oneDecimal(fillRate), daysLeft));
        summary.add(String.format(new Locale("vi", "VN"),
                "Điểm đánh giá hiện là %s với 128 lượt, cao hơn mặt bằng các sự kiện cùng danh mục. "
                        + "Tỷ lệ tham dự thực tế thường thấp hơn số đăng ký nên cần dự phòng chỗ trống.",
                oneDecimal(rating)));

        List<AiInsightUiModel.Insight> insights = new ArrayList<>();
        insights.add(new AiInsightUiModel.Insight(AiInsightUiModel.Type.OBSERVATION,
                "Tốc độ đăng ký trong ba ngày gần nhất cao hơn mặt bằng hai tuần trước đó.",
                AiInsightUiModel.MetricKey.FILL_RATE, AiInsightUiModel.Confidence.HIGH,
                AiInsightUiModel.ActionTarget.NONE));
        insights.add(new AiInsightUiModel.Insight(AiInsightUiModel.Type.SUGGESTION,
                "Cân nhắc mở thêm một đợt vé Standard cho khách đến muộn, vì hạng vé này đang được "
                        + "chọn nhiều nhất.",
                AiInsightUiModel.MetricKey.FILL_RATE, AiInsightUiModel.Confidence.MEDIUM,
                AiInsightUiModel.ActionTarget.EDIT_TICKETS));
        insights.add(new AiInsightUiModel.Insight(AiInsightUiModel.Type.SUGGESTION,
                "Gửi nhắc nhở trước ngày diễn ra hai ngày để giảm tỷ lệ khách không đến.",
                AiInsightUiModel.MetricKey.DAYS_LEFT, AiInsightUiModel.Confidence.LOW,
                AiInsightUiModel.ActionTarget.VIEW_ORDERS));
        insights.add(new AiInsightUiModel.Insight(AiInsightUiModel.Type.OBSERVATION,
                "Điểm đánh giá cao giúp tăng tỷ lệ chuyển đổi ở lần mở bán sau.",
                AiInsightUiModel.MetricKey.RATING, AiInsightUiModel.Confidence.MEDIUM,
                AiInsightUiModel.ActionTarget.NONE));

        List<String> limitations = new ArrayList<>();
        limitations.add("Chưa có dữ liệu check-in vì sự kiện chưa diễn ra.");
        limitations.add("Số liệu doanh thu theo từng hạng vé chưa được tổng hợp cho kỳ này.");

        return new AiInsightUiModel.Analysis(1L, "Đêm nhạc Indie Sài Gòn 2026", now,
                AiInsightUiModel.Source.AI, false, "", metrics, summary, insights, limitations,
                series(30, 12f, 48f), labels(30), false);
    }

    private static AiInsightUiModel.Analysis ruleFallback() {
        long now = System.currentTimeMillis();
        List<AiInsightUiModel.Metric> metrics = new ArrayList<>();
        metrics.add(new AiInsightUiModel.Metric(AiInsightUiModel.MetricKey.REGISTRATIONS,
                268, "500 chỗ"));
        metrics.add(new AiInsightUiModel.Metric(AiInsightUiModel.MetricKey.FILL_RATE,
                53.6, null));
        metrics.add(new AiInsightUiModel.Metric(AiInsightUiModel.MetricKey.DAYS_LEFT,
                9, null));
        metrics.add(new AiInsightUiModel.Metric(AiInsightUiModel.MetricKey.RATING,
                4.7, "64 đánh giá"));

        List<String> summary = new ArrayList<>();
        summary.add("Chưa thể tạo tóm tắt bằng AI. Các nhận xét dưới đây dựa trên luật so sánh "
                + "với dữ liệu bán vé hiện có, nên chỉ mang tính tham khảo.");

        List<AiInsightUiModel.Insight> insights = new ArrayList<>();
        insights.add(new AiInsightUiModel.Insight(AiInsightUiModel.Type.OBSERVATION,
                "Lượng đăng ký tăng chậm hơn so với tuần đầu mở bán.",
                AiInsightUiModel.MetricKey.REGISTRATIONS, AiInsightUiModel.Confidence.LOW,
                AiInsightUiModel.ActionTarget.NONE));
        insights.add(new AiInsightUiModel.Insight(AiInsightUiModel.Type.SUGGESTION,
                "Kiểm tra lại mô tả và ảnh bìa vì tỷ lệ xem chi tiết còn thấp so với lượt hiển thị.",
                AiInsightUiModel.MetricKey.FILL_RATE, AiInsightUiModel.Confidence.LOW,
                AiInsightUiModel.ActionTarget.EDIT_EVENT));

        List<String> limitations = new ArrayList<>();
        limitations.add("Trợ lý AI không phản hồi nên kết quả được tạo theo luật.");
        limitations.add("Chưa có số liệu tài chính nên phần doanh thu bị bỏ qua.");

        return new AiInsightUiModel.Analysis(2L, "Hội thảo AI & Tương lai 2026", now,
                AiInsightUiModel.Source.RULE, false, "", metrics, summary, insights, limitations,
                series(30, 4f, 22f), labels(30), false);
    }

    private static AiInsightUiModel.Analysis workshop() {
        long now = System.currentTimeMillis();
        int registrations = 6;
        int daysLeft = 2;

        List<AiInsightUiModel.Metric> metrics = new ArrayList<>();
        metrics.add(new AiInsightUiModel.Metric(AiInsightUiModel.MetricKey.REGISTRATIONS,
                registrations, "30 chỗ"));
        metrics.add(new AiInsightUiModel.Metric(AiInsightUiModel.MetricKey.DAYS_LEFT,
                daysLeft, null));

        String message = String.format(new Locale("vi", "VN"),
                "Cần ít nhất 7 ngày mở bán hoặc 20 đăng ký hợp lệ. Hiện có %d đăng ký sau %d ngày.",
                registrations, daysLeft);

        List<String> limitations = new ArrayList<>();
        limitations.add("Kết luận sẽ không đáng tin khi dữ liệu còn quá ít.");

        return new AiInsightUiModel.Analysis(5L, "Workshop Gốm cuối tuần", now,
                AiInsightUiModel.Source.RULE, true, message, metrics, new ArrayList<>(),
                new ArrayList<>(), limitations, series(4, 1f, 3f), labels(4), false);
    }

    private static String oneDecimal(double value) {
        return String.format(new Locale("vi", "VN"), "%.1f", value);
    }

    /** Deterministic 30-point series; a real API-18 response supplies this array. */
    private static List<Float> series(int days, float base, float peak) {
        List<Float> values = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            float ratio = days <= 1 ? 1f : i / (float) (days - 1);
            float value = base + (peak - base) * ratio * ratio;
            values.add((float) Math.round(value));
        }
        return values;
    }

    private static List<String> labels(int days) {
        List<String> values = new ArrayList<>();
        SimpleDateFormat format = new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1));
        for (int i = 0; i < days; i++) {
            values.add(format.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return values;
    }
}
