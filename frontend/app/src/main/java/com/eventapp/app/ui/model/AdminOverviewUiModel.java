package com.eventapp.app.ui.model;

import java.util.List;

/**
 * Ảnh chụp tổng quan khu quản trị (màn Tổng quan).
 *
 * <p>Số liệu do backend tổng hợp; app chỉ hiển thị, không tự tính lại (tránh lệch số giữa các
 * màn). Mọi trường đều có thể rỗng để màn hình hiện được trạng thái thiếu dữ liệu.</p>
 */
public final class AdminOverviewUiModel {

    public enum Tone {
        DEFAULT,
        SUCCESS,
        WARNING,
        DANGER
    }

    /** Một ô KPI: giá trị lớn, nhãn phụ và sắc thái màu. */
    public static final class Kpi {

        public final String value;
        public final String delta;
        public final String caption;
        public final Tone tone;

        public Kpi(String value, String delta, String caption, Tone tone) {
            this.value = value;
            this.delta = delta;
            this.caption = caption;
            this.tone = tone;
        }
    }

    public final List<Kpi> kpis;
    public final float[] trend;
    public final String[] trendLabels;
    public final String peakLabel;
    public final String peakValue;
    public final String updatedAt;

    public AdminOverviewUiModel(List<Kpi> kpis, float[] trend, String[] trendLabels,
                                String peakLabel, String peakValue, String updatedAt) {
        this.kpis = kpis;
        this.trend = trend;
        this.trendLabels = trendLabels;
        this.peakLabel = peakLabel;
        this.peakValue = peakValue;
        this.updatedAt = updatedAt;
    }
}
