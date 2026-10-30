package com.eventapp.app.ui.fake;


import com.eventapp.app.ui.model.DashboardUiModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic API-17 fixture for FR-O03. Values are the aggregates a backend would return;
 * nothing here is derived from other values so the screen never computes money locally (O03-R03).
 */
public final class SampleDashboardData {

    private SampleDashboardData() {
    }

    /**
     * @param rangeDays  requested range, used to pick one of the three fixtures
     * @param checkinOk  false keeps the check-in block in its ERROR state until the user retries
     */
    public static DashboardUiModel.Snapshot create(int rangeDays, boolean checkinOk) {
        long now = System.currentTimeMillis();
        DashboardUiModel.Finance finance = financeFor(rangeDays);
        List<DashboardUiModel.Metric> metrics = new ArrayList<>();
        metrics.add(new DashboardUiModel.Metric(DashboardUiModel.MetricKey.REGISTRATIONS,
                registrationsFor(rangeDays), null, registrationsDeltaFor(rangeDays),
                DashboardUiModel.BlockStatus.OK));
        metrics.add(new DashboardUiModel.Metric(DashboardUiModel.MetricKey.TICKETS,
                ticketsFor(rangeDays), null, ticketsDeltaFor(rangeDays),
                DashboardUiModel.BlockStatus.OK));
        metrics.add(new DashboardUiModel.Metric(DashboardUiModel.MetricKey.CHECKIN,
                checkinOk ? 712 : 0, "712/842 vé đã phát hành", null,
                checkinOk ? DashboardUiModel.BlockStatus.OK : DashboardUiModel.BlockStatus.ERROR));
        metrics.add(new DashboardUiModel.Metric(DashboardUiModel.MetricKey.RATING,
                4.8, "128 đánh giá", null, DashboardUiModel.BlockStatus.OK));

        return new DashboardUiModel.Snapshot(
                "Minh Anh",
                finance,
                metrics,
                statusCounts(),
                tasks(),
                topEvents(),
                now,
                false,
                false);
    }

    private static DashboardUiModel.Finance financeFor(int rangeDays) {
        if (rangeDays <= 7) {
            return new DashboardUiModel.Finance(42_800_000L, 9_450_000L, 1_200_000L,
                    1_498_000L, 40_102_000L, 12, DashboardUiModel.BlockStatus.OK);
        }
        if (rangeDays <= 30) {
            return new DashboardUiModel.Finance(186_450_000L, 24_700_000L, 6_750_000L,
                    6_525_750L, 173_174_250L, 18, DashboardUiModel.BlockStatus.OK);
        }
        return new DashboardUiModel.Finance(604_230_000L, 51_300_000L, 18_400_000L,
                21_148_050L, 564_681_950L, 24, DashboardUiModel.BlockStatus.OK);
    }

    private static double registrationsFor(int rangeDays) {
        return rangeDays <= 7 ? 246 : rangeDays <= 30 ? 842 : 2_318;
    }

    private static Integer registrationsDeltaFor(int rangeDays) {
        return rangeDays <= 7 ? 9 : rangeDays <= 30 ? 14 : 21;
    }

    private static double ticketsFor(int rangeDays) {
        return rangeDays <= 7 ? 318 : rangeDays <= 30 ? 1_000 : 2_640;
    }

    private static Integer ticketsDeltaFor(int rangeDays) {
        return rangeDays <= 7 ? 6 : rangeDays <= 30 ? 11 : 17;
    }

    private static List<DashboardUiModel.StatusCount> statusCounts() {
        List<DashboardUiModel.StatusCount> counts = new ArrayList<>();
        counts.add(DashboardUiModel.StatusCount.review(DashboardUiModel.ReviewStage.DRAFT, 1));
        counts.add(DashboardUiModel.StatusCount.review(
                DashboardUiModel.ReviewStage.PENDING_REVIEW, 2));
        counts.add(DashboardUiModel.StatusCount.review(
                DashboardUiModel.ReviewStage.REVISION_REQUIRED, 1));
        counts.add(DashboardUiModel.StatusCount.review(
                DashboardUiModel.ReviewStage.REJECTED, 1));
        counts.add(DashboardUiModel.StatusCount.review(
                DashboardUiModel.ReviewStage.APPROVED, 6));
        counts.add(DashboardUiModel.StatusCount.lifecycle(
                DashboardUiModel.LifecycleStage.ON_SALE, 3));
        counts.add(DashboardUiModel.StatusCount.lifecycle(
                DashboardUiModel.LifecycleStage.UPCOMING, 2));
        counts.add(DashboardUiModel.StatusCount.lifecycle(
                DashboardUiModel.LifecycleStage.COMPLETED, 4));
        counts.add(DashboardUiModel.StatusCount.lifecycle(
                DashboardUiModel.LifecycleStage.UNPUBLISHED, 1));
        counts.add(DashboardUiModel.StatusCount.lifecycle(
                DashboardUiModel.LifecycleStage.SUSPENDED, 1));
        return counts;
    }

    private static List<DashboardUiModel.Task> tasks() {
        List<DashboardUiModel.Task> tasks = new ArrayList<>();
        tasks.add(new DashboardUiModel.Task(DashboardUiModel.TaskKind.EVENT_STARTING_SOON,
                DashboardUiModel.Urgency.NOW,
                "Đêm nhạc Indie Sài Gòn 2026",
                "Bắt đầu sau 2 ngày • 128/200 vé đã bán", 1L, 1));
        tasks.add(new DashboardUiModel.Task(DashboardUiModel.TaskKind.REFUND_PENDING,
                DashboardUiModel.Urgency.SOON,
                "Yêu cầu hoàn tiền chờ xử lý",
                "3 yêu cầu • 1.200.000 đ", 2L, 3));
        tasks.add(new DashboardUiModel.Task(DashboardUiModel.TaskKind.EVENT_REVISION,
                DashboardUiModel.Urgency.SOON,
                "Hội thảo AI & Tương lai 2026",
                "Cần bổ sung ảnh bìa và chính sách hủy", 2L, 1));
        tasks.add(new DashboardUiModel.Task(DashboardUiModel.TaskKind.TICKET_SALE_ENDING,
                DashboardUiModel.Urgency.LATER,
                "Giải chạy Marathon Quốc tế",
                "Hạng vé Early Bird kết thúc sau 36 giờ", 3L, 1));
        tasks.add(new DashboardUiModel.Task(DashboardUiModel.TaskKind.EVENT_REJECTED,
                DashboardUiModel.Urgency.LATER,
                "Workshop Gốm cuối tuần",
                "Bị từ chối: mô tả thiếu thông tin bắt buộc", 5L, 1));
        return tasks;
    }

    private static List<DashboardUiModel.TopEvent> topEvents() {
        List<DashboardUiModel.TopEvent> events = new ArrayList<>();
        events.add(new DashboardUiModel.TopEvent(1L, "Đêm nhạc Indie Sài Gòn 2026",
                72_000_000L, 412, DashboardUiModel.ReviewStage.APPROVED,
                DashboardUiModel.LifecycleStage.ON_SALE));
        events.add(new DashboardUiModel.TopEvent(2L, "Hội thảo AI & Tương lai 2026",
                54_600_000L, 268, DashboardUiModel.ReviewStage.APPROVED,
                DashboardUiModel.LifecycleStage.UPCOMING));
        events.add(new DashboardUiModel.TopEvent(3L, "Giải chạy Marathon Quốc tế",
                38_250_000L, 214, DashboardUiModel.ReviewStage.APPROVED,
                DashboardUiModel.LifecycleStage.ON_SALE));
        events.add(new DashboardUiModel.TopEvent(4L, "Triển lãm Sắc màu Đương đại",
                12_400_000L, 96, DashboardUiModel.ReviewStage.PENDING_REVIEW,
                DashboardUiModel.LifecycleStage.UNPUBLISHED));
        events.add(new DashboardUiModel.TopEvent(5L, "Workshop Gốm cuối tuần",
                9_200_000L, 52, DashboardUiModel.ReviewStage.REJECTED,
                DashboardUiModel.LifecycleStage.CANCELLED));
        return events;
    }
}
