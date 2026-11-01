package com.eventapp.app.ui.admin.dashboard;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.eventapp.app.ui.admin.event.ContentModerationFragment;
import com.eventapp.app.ui.common.view.KpiTileView;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.common.view.SimpleBarChartView;
import com.eventapp.app.ui.fake.SampleAdminDashboardData;
import com.eventapp.app.ui.fake.SampleModerationData;
import com.eventapp.app.ui.fake.SampleReviewData;
import com.eventapp.app.ui.model.AdminOverviewUiModel;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Tổng quan khu quản trị.
 *
 * <p>Bố cục và màu bám màn Admin Dashboard của Stitch: header có nhãn HOST, dải báo cáo điều hành,
 * bộ lọc thời gian dạng segmented, lưới KPI 2×2, biểu đồ GMV 7 ngày, rồi hai nhóm lối vào các màn
 * của module. FR-A01 (số liệu thật) vẫn thuộc phần của thành viên khác — ở đây dùng fixture.</p>
 */
public class AdminDashboardFragment extends Fragment {

    private static final long LOAD_DELAY_MS = 320L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private KpiTileView gmvTile;
    private KpiTileView ticketsTile;
    private KpiTileView eventsTile;
    private KpiTileView orgsTile;
    private SimpleBarChartView chart;
    private TextView chartCaption;
    private TextView updatedView;
    private TextView alertsView;
    private Chip[] rangeChips;

    private int range = SampleAdminDashboardData.RANGE_TODAY;
    private boolean bindingRange;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupHeader(view);
        setupRange(view);
        setupShortcuts(view);
        render(SampleAdminDashboardData.create(range));
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private void bindViews(View view) {
        gmvTile = view.findViewById(R.id.tileAdminGmv);
        ticketsTile = view.findViewById(R.id.tileAdminTickets);
        eventsTile = view.findViewById(R.id.tileAdminEvents);
        orgsTile = view.findViewById(R.id.tileAdminOrgs);
        chart = view.findViewById(R.id.chartAdminGmv);
        chartCaption = view.findViewById(R.id.tvAdminChartCaption);
        updatedView = view.findViewById(R.id.tvAdminUpdated);
        alertsView = view.findViewById(R.id.tvAdminAlerts);
        rangeChips = new Chip[]{
                view.findViewById(R.id.chipAdminRangeToday),
                view.findViewById(R.id.chipAdminRangeWeek),
                view.findViewById(R.id.chipAdminRangeMonth),
                view.findViewById(R.id.chipAdminRangeYear)
        };
    }

    private void setupHeader(View view) {
        ((TextView) view.findViewById(R.id.tvAdminToday)).setText(
                getString(R.string.hc_adm_today,
                        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date())));

        List<?> flags = SampleModerationData.createFlags();
        List<?> reviews = SampleReviewData.moderationQueue();
        alertsView.setText(getString(R.string.hc_adm_alerts, flags.size()));
        ((TextView) view.findViewById(R.id.tvAdminFlagsCount)).setText(String.valueOf(flags.size()));
        ((TextView) view.findViewById(R.id.tvAdminReviewsCount))
                .setText(String.valueOf(reviews.size()));

        ((SectionHeaderView) view.findViewById(R.id.headerAdminHubModeration))
                .setTitle(R.string.hc_adminhub_section_moderation);
        ((SectionHeaderView) view.findViewById(R.id.headerAdminHubConfig))
                .setTitle(R.string.hc_adminhub_section_config);
    }

    private void setupRange(View view) {
        for (int i = 0; i < rangeChips.length; i++) {
            final int index = i;
            rangeChips[i].setOnClickListener(v -> selectRange(index));
        }
    }

    private void selectRange(int index) {
        if (bindingRange || index == range) {
            return;
        }
        range = index;
        bindingRange = true;
        for (int i = 0; i < rangeChips.length; i++) {
            rangeChips[i].setChecked(i == index);
        }
        bindingRange = false;
        load();
    }

    private void load() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            if (isAdded()) {
                render(SampleAdminDashboardData.create(range));
            }
        }, LOAD_DELAY_MS);
    }

    private void render(AdminOverviewUiModel model) {
        KpiTileView[] tiles = {gmvTile, ticketsTile, eventsTile, orgsTile};
        int[] labels = {
                R.string.hc_adm_kpi_gmv,
                R.string.hc_adm_kpi_tickets,
                R.string.hc_adm_kpi_events,
                R.string.hc_adm_kpi_orgs
        };

        for (int i = 0; i < tiles.length && i < model.kpis.size(); i++) {
            AdminOverviewUiModel.Kpi kpi = model.kpis.get(i);
            tiles[i].setLabel(labels[i]);
            tiles[i].setValue(kpi.value);
            tiles[i].setDelta(kpi.delta, toneOf(kpi.tone));
            tiles[i].setCaption(kpi.caption);
            tiles[i].setTone(KpiTileView.Tone.DEFAULT);
        }

        chart.setData(model.trend, model.trendLabels);
        chartCaption.setText(getString(R.string.hc_adm_chart_caption,
                model.peakLabel, model.peakValue));
        updatedView.setText(getString(R.string.hc_adm_updated, model.updatedAt));
    }

    private static KpiTileView.Tone toneOf(AdminOverviewUiModel.Tone tone) {
        switch (tone) {
            case SUCCESS:
                return KpiTileView.Tone.SUCCESS;
            case WARNING:
                return KpiTileView.Tone.WARNING;
            case DANGER:
                return KpiTileView.Tone.DANGER;
            default:
                return KpiTileView.Tone.DEFAULT;
        }
    }

    private void setupShortcuts(View view) {
        view.findViewById(R.id.cardAdminHubFlags)
                .setOnClickListener(v -> openModeration(v, 0));
        view.findViewById(R.id.cardAdminHubReviews)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.review_moderationFragment));
        view.findViewById(R.id.cardAdminHubCategories)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.category_manageFragment));
        view.findViewById(R.id.cardAdminHubIntegrations)
                .setOnClickListener(v -> Navigation.findNavController(v)
                        .navigate(R.id.integration_healthFragment));
    }

    private void openModeration(View view, int startTab) {
        Bundle args = new Bundle();
        args.putInt(ContentModerationFragment.ARG_START_TAB, startTab);
        Navigation.findNavController(view).navigate(R.id.content_moderationFragment, args);
    }
}
