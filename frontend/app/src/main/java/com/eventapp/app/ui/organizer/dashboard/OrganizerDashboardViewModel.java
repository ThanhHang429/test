package com.eventapp.app.ui.organizer.dashboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.core.session.SessionManager;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.dashboard.DashboardDtos;
import com.eventapp.app.data.repository.OrganizerRepository;
import com.eventapp.app.ui.model.DashboardUiModel;
import com.google.gson.JsonElement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * FR-O03 — số liệu lấy từ {@code GET /organizer/dashboard}.
 *
 * <p>Mọi con số là tổng hợp của server: client chỉ định dạng lại. Mỗi khối giữ trạng thái riêng
 * (O03-R05) nên một khối lỗi không làm trắng cả dashboard.</p>
 */
public class OrganizerDashboardViewModel extends ViewModel {

    public enum Range {
        LAST_7_DAYS,
        LAST_30_DAYS,
        CUSTOM
    }

    public static final int MAX_RANGE_DAYS = 366;
    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private final OrganizerRepository repository = ServiceLocator.get(OrganizerRepository.class);

    private final MutableLiveData<DashboardUiModel.Snapshot> snapshot = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Range> range = new MutableLiveData<>(Range.LAST_30_DAYS);

    private final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private long customFrom;
    private long customTo;

    public OrganizerDashboardViewModel() {
        dayFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        load();
    }

    public LiveData<DashboardUiModel.Snapshot> getSnapshot() {
        return snapshot;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<Range> getRange() {
        return range;
    }

    public void load() {
        loading.setValue(true);
        error.setValue(false);

        Range current = range.getValue() == null ? Range.LAST_30_DAYS : range.getValue();
        String rangeParam;
        String from = null;
        String to = null;
        if (current == Range.LAST_7_DAYS) {
            rangeParam = "7d";
        } else if (current == Range.CUSTOM) {
            rangeParam = "custom";
            from = customFrom > 0 ? dayFormat.format(new Date(customFrom)) : null;
            to = customTo > 0 ? dayFormat.format(new Date(customTo)) : null;
        } else {
            rangeParam = "30d";
        }

        repository.dashboard(rangeParam, from, to,
                new ApiCallback<DashboardDtos.Dashboard>() {
                    @Override
                    public void onSuccess(@Nullable DashboardDtos.Dashboard data,
                                          @Nullable JsonElement meta) {
                        if (data == null) {
                            error.setValue(true);
                            return;
                        }
                        snapshot.setValue(map(data));
                        loading.setValue(false);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        loading.setValue(false);
                        error.setValue(true);
                    }
                });
    }

    public void refresh() {
        load();
    }

    public void setRange(Range value) {
        Range next = value == null ? Range.LAST_30_DAYS : value;
        if (next == range.getValue() && snapshot.getValue() != null) {
            return;
        }
        range.setValue(next);
        if (next != Range.CUSTOM) {
            load();
        }
    }

    /**
     * Stores a custom range. Returns false when the span exceeds 366 days (SRS: 400
     * VALIDATION_ERROR on the server, blocked here first).
     */
    public boolean setCustomRange(long from, long to) {
        if (from > 0 && to > 0) {
            long start = Math.min(from, to);
            long end = Math.max(from, to);
            long days = (end - start) / DAY_MILLIS + 1;
            if (days > MAX_RANGE_DAYS) {
                return false;
            }
            customFrom = start;
            customTo = end;
        } else {
            if (from > 0) {
                customFrom = from;
            }
            if (to > 0) {
                customTo = to;
            }
        }
        range.setValue(Range.CUSTOM);
        if (customFrom > 0 && customTo > 0) {
            load();
        }
        return true;
    }

    public long getCustomFrom() {
        return customFrom;
    }

    public long getCustomTo() {
        return customTo;
    }

    /** Dashboard là một lời gọi duy nhất nên thử lại khối lỗi = tải lại toàn bộ. */
    public void retryMetric(DashboardUiModel.MetricKey key) {
        load();
    }

    public int rangeDays() {
        if (range.getValue() == Range.LAST_7_DAYS) {
            return 7;
        }
        if (range.getValue() == Range.CUSTOM && customFrom > 0 && customTo > 0) {
            long days = (customTo - customFrom) / DAY_MILLIS + 1;
            return (int) Math.min(MAX_RANGE_DAYS, Math.max(1L, days));
        }
        return 30;
    }

    // ------------------------------------------------------------------ mapping

    @NonNull
    private DashboardUiModel.Snapshot map(@NonNull DashboardDtos.Dashboard dto) {
        return new DashboardUiModel.Snapshot(
                organizerName(),
                finance(dto.finance),
                metrics(dto),
                statusCounts(dto.events),
                tasks(dto.tasks),
                topEvents(dto.topEvents),
                DateTimeUtils.toMillis(dto.asOf),
                false,
                false);
    }

    @NonNull
    private String organizerName() {
        AuthSession session = SessionManager.get().getSession();
        if (session == null || session.fullName == null) {
            return "";
        }
        String name = session.fullName.trim();
        int space = name.lastIndexOf(' ');
        return space > 0 ? name.substring(space + 1) : name;
    }

    @NonNull
    private DashboardUiModel.Finance finance(@Nullable DashboardDtos.FinanceBlock block) {
        if (block == null) {
            return new DashboardUiModel.Finance(0, 0, 0, 0, 0, null,
                    DashboardUiModel.BlockStatus.ERROR);
        }
        return new DashboardUiModel.Finance(
                Math.round(block.verifiedAmount),
                Math.round(block.pendingAmount),
                Math.round(block.refundedAmount),
                Math.round(block.feeAmount),
                Math.round(block.netAmount),
                null,
                blockStatus(block.meta));
    }

    @NonNull
    private List<DashboardUiModel.Metric> metrics(@NonNull DashboardDtos.Dashboard dto) {
        List<DashboardUiModel.Metric> values = new ArrayList<>();
        DashboardDtos.RegistrationBlock reg = dto.registrations;
        DashboardDtos.CheckinBlock checkin = dto.checkin;
        DashboardDtos.ReviewBlock reviews = dto.reviews;

        values.add(new DashboardUiModel.Metric(
                DashboardUiModel.MetricKey.REGISTRATIONS,
                reg == null ? 0 : reg.confirmed,
                reg == null ? "" : "Đăng ký đã xác nhận",
                null,
                reg == null ? DashboardUiModel.BlockStatus.ERROR : blockStatus(reg.meta)));
        values.add(new DashboardUiModel.Metric(
                DashboardUiModel.MetricKey.TICKETS,
                reg == null ? 0 : reg.ticketsIssued,
                reg == null ? "" : "Vé đã phát hành",
                null,
                reg == null ? DashboardUiModel.BlockStatus.ERROR : blockStatus(reg.meta)));
        values.add(new DashboardUiModel.Metric(
                DashboardUiModel.MetricKey.CHECKIN,
                checkin == null ? 0 : checkin.checkedIn,
                checkin == null ? "" : Math.round(checkin.checkinRate * 100) + "% đã check-in",
                null,
                checkin == null ? DashboardUiModel.BlockStatus.ERROR : blockStatus(checkin.meta)));
        values.add(new DashboardUiModel.Metric(
                DashboardUiModel.MetricKey.RATING,
                reviews == null ? 0 : reviews.averageRating,
                reviews == null ? "" : reviews.totalVisible + " đánh giá hiển thị",
                null,
                reviews == null ? DashboardUiModel.BlockStatus.ERROR : blockStatus(reviews.meta)));
        return values;
    }

    @NonNull
    private List<DashboardUiModel.StatusCount> statusCounts(
            @Nullable DashboardDtos.EventStatusBlock block) {
        List<DashboardUiModel.StatusCount> values = new ArrayList<>();
        if (block == null || block.counts == null) {
            return values;
        }
        Map<String, Long> counts = block.counts;
        addReview(values, counts, "DRAFT", DashboardUiModel.ReviewStage.DRAFT);
        addReview(values, counts, "PENDING_REVIEW", DashboardUiModel.ReviewStage.PENDING_REVIEW);
        addReview(values, counts, "REVISION_REQUIRED", DashboardUiModel.ReviewStage.REVISION_REQUIRED);
        addReview(values, counts, "REJECTED", DashboardUiModel.ReviewStage.REJECTED);
        addReview(values, counts, "APPROVED_UNPUBLISHED", DashboardUiModel.ReviewStage.APPROVED);
        addLifecycle(values, counts, "ON_SALE", DashboardUiModel.LifecycleStage.ON_SALE);
        addLifecycle(values, counts, "UPCOMING", DashboardUiModel.LifecycleStage.UPCOMING);
        addLifecycle(values, counts, "COMPLETED", DashboardUiModel.LifecycleStage.COMPLETED);
        addLifecycle(values, counts, "SUSPENDED", DashboardUiModel.LifecycleStage.SUSPENDED);
        addLifecycle(values, counts, "CANCELLED", DashboardUiModel.LifecycleStage.CANCELLED);
        return values;
    }

    private static void addReview(@NonNull List<DashboardUiModel.StatusCount> target,
                                  @NonNull Map<String, Long> counts, @NonNull String key,
                                  @NonNull DashboardUiModel.ReviewStage stage) {
        Long value = counts.get(key);
        if (value != null && value > 0L) {
            target.add(DashboardUiModel.StatusCount.review(stage, value.intValue()));
        }
    }

    private static void addLifecycle(@NonNull List<DashboardUiModel.StatusCount> target,
                                     @NonNull Map<String, Long> counts, @NonNull String key,
                                     @NonNull DashboardUiModel.LifecycleStage stage) {
        Long value = counts.get(key);
        if (value != null && value > 0L) {
            target.add(DashboardUiModel.StatusCount.lifecycle(stage, value.intValue()));
        }
    }

    @NonNull
    private List<DashboardUiModel.Task> tasks(@Nullable List<DashboardDtos.Task> source) {
        List<DashboardUiModel.Task> values = new ArrayList<>();
        if (source == null) {
            return values;
        }
        for (DashboardDtos.Task dto : source) {
            if (dto == null) {
                continue;
            }
            values.add(new DashboardUiModel.Task(
                    taskKind(dto.type),
                    urgency(dto.priority),
                    dto.title,
                    dto.detail,
                    dto.eventId,
                    1));
        }
        return values;
    }

    @NonNull
    private List<DashboardUiModel.TopEvent> topEvents(
            @Nullable List<DashboardDtos.TopEvent> source) {
        List<DashboardUiModel.TopEvent> values = new ArrayList<>();
        if (source == null) {
            return values;
        }
        for (DashboardDtos.TopEvent dto : source) {
            if (dto == null) {
                continue;
            }
            values.add(new DashboardUiModel.TopEvent(
                    dto.eventId,
                    dto.title,
                    Math.round(dto.verifiedAmount),
                    (int) dto.registrations,
                    null,
                    null));
        }
        return values;
    }

    @NonNull
    private static DashboardUiModel.BlockStatus blockStatus(
            @Nullable DashboardDtos.BlockMeta meta) {
        if (meta == null) {
            return DashboardUiModel.BlockStatus.OK;
        }
        if ("ERROR".equalsIgnoreCase(meta.status)) {
            return DashboardUiModel.BlockStatus.ERROR;
        }
        return DashboardUiModel.BlockStatus.OK;
    }

    @NonNull
    private static DashboardUiModel.TaskKind taskKind(@Nullable String raw) {
        if (raw == null) {
            return DashboardUiModel.TaskKind.EVENT_STARTING_SOON;
        }
        switch (raw.toUpperCase(Locale.ROOT)) {
            case "REFUND_PENDING":
                return DashboardUiModel.TaskKind.REFUND_PENDING;
            case "EVENT_REVISION":
                return DashboardUiModel.TaskKind.EVENT_REVISION;
            case "EVENT_REJECTED":
                return DashboardUiModel.TaskKind.EVENT_REJECTED;
            case "TICKET_SALE_ENDING":
                return DashboardUiModel.TaskKind.TICKET_SALE_ENDING;
            case "EVENT_STARTING_SOON":
            default:
                return DashboardUiModel.TaskKind.EVENT_STARTING_SOON;
        }
    }

    @NonNull
    private static DashboardUiModel.Urgency urgency(int priority) {
        if (priority <= 1) {
            return DashboardUiModel.Urgency.NOW;
        }
        if (priority <= 3) {
            return DashboardUiModel.Urgency.SOON;
        }
        return DashboardUiModel.Urgency.LATER;
    }
}
