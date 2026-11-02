package com.eventapp.app.ui.organizer.report;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.organizerai.OrganizerAiDtos;
import com.eventapp.app.data.repository.OrganizerRepository;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.model.AiInsightUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FR-O27 — phân tích một sự kiện qua {@code POST /organizer/events/{id}/ai-analysis}.
 *
 * <p>Toàn bộ số liệu do backend trả (O27-R01), giao diện chỉ định dạng. Hết quota server trả phân
 * tích theo luật với {@code source = RULE}, không phải lỗi.</p>
 */
public class AiInsightViewModel extends ViewModel {

    /** Số lần làm mới do UI giới hạn để tránh gọi liên tục; server có quota riêng. */
    public static final int MAX_REFRESH_PER_DAY = 10;

    private final OrganizerRepository repository = ServiceLocator.get(OrganizerRepository.class);

    private final MutableLiveData<AiInsightUiModel.Analysis> analysis = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> quotaReached = new MutableLiveData<>(false);

    private long eventId = -1L;
    private String eventTitle = "";

    public LiveData<AiInsightUiModel.Analysis> getAnalysis() {
        return analysis;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<Boolean> getQuotaReached() {
        return quotaReached;
    }

    /** Màn phân tích chỉ chạy cho sự kiện được truyền vào; không có danh sách riêng. */
    public List<AiInsightUiModel.EventOption> getEvents() {
        List<AiInsightUiModel.EventOption> values = new ArrayList<>();
        if (eventId > 0L) {
            values.add(new AiInsightUiModel.EventOption(eventId, eventTitle));
        }
        return values;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventTitle(@Nullable String title) {
        eventTitle = title == null ? "" : title;
    }

    public void selectEvent(long id) {
        if (id <= 0L || id == eventId) {
            return;
        }
        eventId = id;
        load();
    }

    public boolean canRefresh() {
        return true;
    }

    public void refresh() {
        if (eventId <= 0L) {
            return;
        }
        load();
    }

    public void retry() {
        load();
    }

    private void load() {
        if (eventId <= 0L) {
            loading.setValue(false);
            error.setValue(true);
            return;
        }
        loading.setValue(true);
        error.setValue(false);
        repository.analyze(eventId, new ApiCallback<OrganizerAiDtos.AnalysisResponse>() {
            @Override
            public void onSuccess(@Nullable OrganizerAiDtos.AnalysisResponse data,
                                  @Nullable JsonElement meta) {
                loading.setValue(false);
                if (data == null) {
                    error.setValue(true);
                    return;
                }
                analysis.setValue(map(data));
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                loading.setValue(false);
                error.setValue(true);
            }
        });
    }

    @NonNull
    private AiInsightUiModel.Analysis map(@NonNull OrganizerAiDtos.AnalysisResponse dto) {
        Map<String, Object> metrics = dto.metrics == null ? null : dto.metrics;

        List<AiInsightUiModel.Metric> mappedMetrics = new ArrayList<>();
        double registrations = number(metrics, "registrations");
        double capacity = number(metrics, "capacity");
        mappedMetrics.add(new AiInsightUiModel.Metric(
                AiInsightUiModel.MetricKey.REGISTRATIONS, registrations,
                capacity > 0 ? PriceTextView.formatPlain(Math.round(capacity)) + " chỗ" : null));
        mappedMetrics.add(new AiInsightUiModel.Metric(
                AiInsightUiModel.MetricKey.FILL_RATE, number(metrics, "fillRate") * 100, null));
        mappedMetrics.add(new AiInsightUiModel.Metric(
                AiInsightUiModel.MetricKey.DAYS_LEFT, number(metrics, "daysToEvent"), null));
        double checkinRate = number(metrics, "checkinRate");
        if (checkinRate > 0 || number(metrics, "checkedIn") > 0) {
            mappedMetrics.add(new AiInsightUiModel.Metric(
                    AiInsightUiModel.MetricKey.CHECKIN_RATE,
                    number(metrics, "checkinRate") * 100,
                    PriceTextView.formatPlain(Math.round(number(metrics, "checkedIn")))
                            + " vé đã check-in"));
        }
        double reviewCount = number(metrics, "reviewCount");
        if (reviewCount > 0 || number(metrics, "avgRating") > 0) {
            mappedMetrics.add(new AiInsightUiModel.Metric(
                    AiInsightUiModel.MetricKey.RATING, number(metrics, "avgRating"),
                    PriceTextView.formatPlain(Math.round(reviewCount)) + " đánh giá"));
        }

        List<AiInsightUiModel.Insight> insights = new ArrayList<>();
        if (dto.insights != null) {
            for (OrganizerAiDtos.Insight insight : dto.insights) {
                if (insight == null || insight.text == null) {
                    continue;
                }
                insights.add(new AiInsightUiModel.Insight(
                        insightType(insight.type),
                        insight.text.trim(),
                        null,
                        confidence(insight.confidence),
                        AiInsightUiModel.ActionTarget.NONE));
            }
        }

        String message = dto.insufficientData
                ? String.format(new Locale("vi", "VN"),
                "Cần thêm dữ liệu bán vé (ít nhất 7 ngày hoặc 20 đăng ký) để phân tích đáng tin cậy. "
                        + "Hiện có %s đăng ký.", PriceTextView.formatPlain(Math.round(registrations)))
                : "";

        return new AiInsightUiModel.Analysis(
                dto.eventId > 0 ? dto.eventId : eventId,
                eventTitle,
                DateTimeUtils.toMillis(dto.generatedAt),
                "AI".equalsIgnoreCase(dto.source)
                        ? AiInsightUiModel.Source.AI : AiInsightUiModel.Source.RULE,
                dto.insufficientData,
                message,
                mappedMetrics,
                dto.summary,
                insights,
                dto.dataLimitations,
                new ArrayList<>(),
                new ArrayList<>(),
                false);
    }

    private static double number(@Nullable Map<String, Object> metrics, @NonNull String key) {
        if (metrics == null) {
            return 0;
        }
        Object value = metrics.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0;
    }

    @NonNull
    private static AiInsightUiModel.Type insightType(@Nullable String raw) {
        return "SUGGESTION".equalsIgnoreCase(raw)
                ? AiInsightUiModel.Type.SUGGESTION
                : AiInsightUiModel.Type.OBSERVATION;
    }

    @NonNull
    private static AiInsightUiModel.Confidence confidence(@Nullable String raw) {
        if (raw == null) {
            return AiInsightUiModel.Confidence.LOW;
        }
        switch (raw.toUpperCase(Locale.ROOT)) {
            case "HIGH":
                return AiInsightUiModel.Confidence.HIGH;
            case "MEDIUM":
                return AiInsightUiModel.Confidence.MEDIUM;
            case "LOW":
            default:
                return AiInsightUiModel.Confidence.LOW;
        }
    }
}
