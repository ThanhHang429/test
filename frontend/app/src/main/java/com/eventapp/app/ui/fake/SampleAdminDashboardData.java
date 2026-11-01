package com.eventapp.app.ui.fake;

import com.eventapp.app.ui.model.AdminOverviewUiModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fixture cho màn Tổng quan khu quản trị. Bốn khoảng thời gian trả về bốn bộ số khác nhau để
 * thấy rõ bộ lọc có tác dụng; không giá trị nào được suy ra từ giá trị khác.
 */
public final class SampleAdminDashboardData {

    public static final int RANGE_TODAY = 0;
    public static final int RANGE_WEEK = 1;
    public static final int RANGE_MONTH = 2;
    public static final int RANGE_YEAR = 3;

    private static final String[] LABELS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private SampleAdminDashboardData() {
    }

    public static AdminOverviewUiModel create(int range) {
        List<AdminOverviewUiModel.Kpi> kpis = new ArrayList<>();
        float[] trend;

        switch (range) {
            case RANGE_WEEK:
                kpis.add(new AdminOverviewUiModel.Kpi("98,2 tỷ", "+9,6%",
                        "So với tuần trước", AdminOverviewUiModel.Tone.DEFAULT));
                kpis.add(new AdminOverviewUiModel.Kpi("918K", "87% trả phí",
                        "Tuần này", AdminOverviewUiModel.Tone.DEFAULT));
                kpis.add(new AdminOverviewUiModel.Kpi("512", "21 chờ duyệt",
                        "", AdminOverviewUiModel.Tone.WARNING));
                kpis.add(new AdminOverviewUiModel.Kpi("148", "5 hồ sơ KYC",
                        "", AdminOverviewUiModel.Tone.DEFAULT));
                trend = new float[]{9.1f, 12.4f, 10.8f, 14.2f, 18.6f, 20.1f, 13.0f};
                break;
            case RANGE_MONTH:
                kpis.add(new AdminOverviewUiModel.Kpi("412,6 tỷ", "+12,8%",
                        "So với tháng trước", AdminOverviewUiModel.Tone.DEFAULT));
                kpis.add(new AdminOverviewUiModel.Kpi("3,9M", "88% trả phí",
                        "Tháng này", AdminOverviewUiModel.Tone.DEFAULT));
                kpis.add(new AdminOverviewUiModel.Kpi("1.204", "34 chờ duyệt",
                        "", AdminOverviewUiModel.Tone.WARNING));
                kpis.add(new AdminOverviewUiModel.Kpi("312", "8 hồ sơ KYC",
                        "", AdminOverviewUiModel.Tone.DEFAULT));
                trend = new float[]{42f, 55f, 48f, 61f, 74f, 88f, 44f};
                break;
            case RANGE_YEAR:
                kpis.add(new AdminOverviewUiModel.Kpi("4.182 tỷ", "+24,1%",
                        "So với năm trước", AdminOverviewUiModel.Tone.DEFAULT));
                kpis.add(new AdminOverviewUiModel.Kpi("38,4M", "86% trả phí",
                        "Cả năm 2026", AdminOverviewUiModel.Tone.DEFAULT));
                kpis.add(new AdminOverviewUiModel.Kpi("6.840", "112 chờ duyệt",
                        "", AdminOverviewUiModel.Tone.WARNING));
                kpis.add(new AdminOverviewUiModel.Kpi("1.905", "24 hồ sơ KYC",
                        "", AdminOverviewUiModel.Tone.DEFAULT));
                trend = new float[]{420f, 512f, 486f, 604f, 738f, 812f, 610f};
                break;
            case RANGE_TODAY:
            default:
                kpis.add(new AdminOverviewUiModel.Kpi("14,85 tỷ", "+18,4%",
                        "So với tháng trước", AdminOverviewUiModel.Tone.DEFAULT));
                kpis.add(new AdminOverviewUiModel.Kpi("142,5K", "88% trả phí",
                        "Hôm nay", AdminOverviewUiModel.Tone.DEFAULT));
                kpis.add(new AdminOverviewUiModel.Kpi("382", "34 chờ duyệt",
                        "", AdminOverviewUiModel.Tone.WARNING));
                kpis.add(new AdminOverviewUiModel.Kpi("126", "8 hồ sơ KYC",
                        "", AdminOverviewUiModel.Tone.DEFAULT));
                trend = new float[]{1.2f, 1.5f, 1.1f, 1.6f, 2.4f, 3.2f, 3.0f};
                break;
        }

        int peak = 0;
        for (int i = 1; i < trend.length; i++) {
            if (trend[i] > trend[peak]) {
                peak = i;
            }
        }

        return new AdminOverviewUiModel(
                kpis,
                trend,
                LABELS.clone(),
                LABELS[peak],
                formatPeak(trend[peak], range),
                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
    }

    private static String formatPeak(float value, int range) {
        if (range == RANGE_YEAR) {
            return Math.round(value) + " tỷ";
        }
        return String.format(Locale.US, "%.1f", value).replace('.', ',') + " tỷ";
    }
}
