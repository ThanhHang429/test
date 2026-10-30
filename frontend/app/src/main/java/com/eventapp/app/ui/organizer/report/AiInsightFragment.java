package com.eventapp.app.ui.organizer.report;


import com.eventapp.app.ui.model.AiInsightUiModel;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eventapp.app.R;
import com.eventapp.app.ui.organizer.report.adapter.AiInsightAdapter;
import com.eventapp.app.ui.common.view.AiBadge;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.KpiTileView;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.common.view.SimpleBarChartView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.eventapp.app.ui.common.view.StatusChip;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * FR-O27 analysis screen: event picker, backend metrics, daily chart, AI summary and linked
 * suggestions. No control here changes price, capacity or sales settings (O27-R03).
 */
public class AiInsightFragment extends Fragment {

    public static final String ARG_EVENT_ID = "arg_event_id";
    public static final String ARG_EVENT_TITLE = "arg_event_title";

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm dd/MM", new Locale("vi", "VN"));
    private final List<AiInsightUiModel.EventOption> eventOptions = new ArrayList<>();

    private AiInsightViewModel viewModel;
    private AiInsightAdapter adapter;
    private ArrayAdapter<String> eventAdapter;

    private SwipeRefreshLayout swipeRefresh;
    private View scrollContent;
    private SkeletonLayout skeleton;
    private ErrorStateView errorState;
    private MaterialAutoCompleteTextView eventDropdown;
    private TextView eventSubtitle;
    private TextView generatedAt;
    private TextView cachedNote;
    private TextView quotaNote;
    private StatusChip sourceChip;
    private View insufficientCard;
    private TextView insufficientMessage;
    private KpiTileView registrationsTile;
    private KpiTileView fillTile;
    private KpiTileView daysLeftTile;
    private KpiTileView checkinTile;
    private KpiTileView ratingTile;
    private MaterialCardView chartCard;
    private SimpleBarChartView chart;
    private MaterialCardView summaryCard;
    private AiBadge summaryBadge;
    private LinearLayout summaryLayout;
    private TextView ruleNotice;
    private View limitationsCard;
    private LinearLayout limitationsLayout;
    private SectionHeaderView insightsHeader;
    private RecyclerView insightsList;
    private TextView insightsEmpty;

    private AiInsightUiModel.Analysis current;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_report_ai_insight, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AiInsightViewModel.class);
        bindViews(view);
        setupToolbar();
        setupEventPicker();
        setupList();
        setupStates();

        Bundle args = getArguments();
        long eventId = args == null ? -1L : args.getLong(ARG_EVENT_ID, -1L);
        String eventTitle = args == null ? null : args.getString(ARG_EVENT_TITLE);
        if (eventTitle != null && !eventTitle.isEmpty()) {
            eventSubtitle.setText(eventTitle);
        }
        viewModel.selectEvent(eventId);
        observe();
    }

    private void bindViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefreshInsight);
        scrollContent = view.findViewById(R.id.scrollInsight);
        skeleton = view.findViewById(R.id.skeletonInsight);
        errorState = view.findViewById(R.id.errorInsight);
        eventDropdown = view.findViewById(R.id.dropdownInsightEvent);
        eventSubtitle = view.findViewById(R.id.tvInsightEventSubtitle);
        generatedAt = view.findViewById(R.id.tvInsightGeneratedAt);
        cachedNote = view.findViewById(R.id.tvInsightCachedNote);
        quotaNote = view.findViewById(R.id.tvInsightQuota);
        sourceChip = view.findViewById(R.id.chipInsightSource);
        insufficientCard = view.findViewById(R.id.cardInsightInsufficient);
        insufficientMessage = view.findViewById(R.id.tvInsightInsufficientMessage);
        registrationsTile = view.findViewById(R.id.tileInsightRegistrations);
        fillTile = view.findViewById(R.id.tileInsightFill);
        daysLeftTile = view.findViewById(R.id.tileInsightDaysLeft);
        checkinTile = view.findViewById(R.id.tileInsightCheckin);
        ratingTile = view.findViewById(R.id.tileInsightRating);
        chartCard = view.findViewById(R.id.cardInsightChart);
        chart = view.findViewById(R.id.chartInsightDaily);
        summaryCard = view.findViewById(R.id.cardInsightSummary);
        summaryBadge = view.findViewById(R.id.badgeInsightSummary);
        summaryLayout = view.findViewById(R.id.layoutInsightSummary);
        ruleNotice = view.findViewById(R.id.tvInsightRuleNotice);
        limitationsCard = view.findViewById(R.id.cardInsightLimitations);
        limitationsLayout = view.findViewById(R.id.layoutInsightLimitations);
        insightsHeader = view.findViewById(R.id.headerInsightList);
        insightsList = view.findViewById(R.id.rvInsightItems);
        insightsEmpty = view.findViewById(R.id.tvInsightEmptyItems);
    }

    private void setupToolbar() {
        requireView().findViewById(R.id.btnInsightBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
        requireView().findViewById(R.id.btnInsightRefresh).setOnClickListener(v -> {
            if (!viewModel.canRefresh()) {
                Snackbar.make(requireView(), R.string.hc_insight_quota, Snackbar.LENGTH_LONG)
                        .show();
                return;
            }
            viewModel.refresh();
            Snackbar.make(requireView(), R.string.hc_insight_refreshed, Snackbar.LENGTH_SHORT)
                    .show();
        });
    }

    private void setupEventPicker() {
        eventOptions.clear();
        eventOptions.addAll(viewModel.getEvents());
        List<String> titles = new ArrayList<>();
        for (AiInsightUiModel.EventOption option : eventOptions) {
            titles.add(option.title);
        }
        eventAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1,
                titles);
        eventDropdown.setAdapter(eventAdapter);
        eventDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= eventOptions.size()) {
                return;
            }
            viewModel.selectEvent(eventOptions.get(position).id);
        });
    }

    private void setupList() {
        adapter = new AiInsightAdapter();
        adapter.setOnInsightActionListener(insight -> openInsightAction(insight.action));
        insightsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        insightsList.setItemAnimator(null);
        insightsList.setAdapter(adapter);

        insightsHeader.setTitle(R.string.hc_insight_list_title);
        insightsHeader.showAction(false);
    }

    private void setupStates() {
        swipeRefresh.setColorSchemeResources(R.color.ds_primary);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());

        errorState.setTitle(R.string.hc_insight_error_title);
        errorState.setMessage(R.string.hc_insight_error_message);
        errorState.setOnRetryClickListener(v -> viewModel.retry());

        summaryLayout.setVisibility(View.VISIBLE);
        ruleNotice.setVisibility(View.GONE);
    }

    private void observe() {
        viewModel.getAnalysis().observe(getViewLifecycleOwner(), analysis -> {
            current = analysis;
            render();
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            swipeRefresh.setRefreshing(false);
            render();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> render());

        viewModel.getQuotaReached().observe(getViewLifecycleOwner(), quota -> {
            boolean reached = Boolean.TRUE.equals(quota);
            quotaNote.setVisibility(reached ? View.VISIBLE : View.GONE);
            requireView().findViewById(R.id.btnInsightRefresh).setEnabled(!reached);
            requireView().findViewById(R.id.btnInsightRefresh).setAlpha(reached ? 0.4f : 1f);
        });
    }

    private void render() {
        boolean loading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        boolean failed = Boolean.TRUE.equals(viewModel.getError().getValue());

        skeleton.setVisibility(loading && current == null ? View.VISIBLE : View.GONE);
        errorState.setVisibility(failed ? View.VISIBLE : View.GONE);
        scrollContent.setVisibility(!failed && current != null ? View.VISIBLE : View.GONE);

        if (failed || current == null) {
            return;
        }

        AiInsightUiModel.Analysis analysis = current;
        eventSubtitle.setText(analysis.eventTitle);
        eventDropdown.setText(analysis.eventTitle, false);

        generatedAt.setText(getString(R.string.hc_insight_generated,
                timeFormat.format(new Date(analysis.generatedAt))));
        boolean aiSource = analysis.source == AiInsightUiModel.Source.AI;
        sourceChip.setStatus(
                getString(aiSource ? R.string.hc_insight_source_ai : R.string.hc_insight_source_rule),
                aiSource ? StatusChip.Tone.INFO : StatusChip.Tone.WARNING);

        cachedNote.setVisibility(analysis.fromCache ? View.VISIBLE : View.GONE);
        if (analysis.fromCache) {
            cachedNote.setText(getString(R.string.hc_insight_cached,
                    timeFormat.format(new Date(analysis.generatedAt))));
        }

        insufficientCard.setVisibility(analysis.insufficientData ? View.VISIBLE : View.GONE);
        insufficientMessage.setText(analysis.insufficientMessage);

        bindMetrics(analysis);
        bindChart(analysis);
        bindSummary(analysis);
        bindLimitations(analysis);
        bindInsights(analysis);
    }

    private void bindMetrics(AiInsightUiModel.Analysis analysis) {
        bindMetric(registrationsTile, analysis.metric(AiInsightUiModel.MetricKey.REGISTRATIONS),
                R.string.hc_insight_metric_registrations);
        bindMetric(fillTile, analysis.metric(AiInsightUiModel.MetricKey.FILL_RATE),
                R.string.hc_insight_metric_fill);
        bindMetric(daysLeftTile, analysis.metric(AiInsightUiModel.MetricKey.DAYS_LEFT),
                R.string.hc_insight_metric_days_left);
        bindMetric(checkinTile, analysis.metric(AiInsightUiModel.MetricKey.CHECKIN_RATE),
                R.string.hc_insight_metric_checkin);
        bindMetric(ratingTile, analysis.metric(AiInsightUiModel.MetricKey.RATING),
                R.string.hc_insight_metric_rating);
    }

    private void bindMetric(KpiTileView tile, @Nullable AiInsightUiModel.Metric metric,
                            int labelRes) {
        tile.setLabel(labelRes);
        tile.clearError();
        tile.setTone(KpiTileView.Tone.DEFAULT);

        if (metric == null) {
            // O27-R01: never render a number the backend did not send.
            tile.setVisibility(View.GONE);
            return;
        }
        tile.setVisibility(View.VISIBLE);
        tile.setValue(formatMetric(metric));
        tile.setCaption(metric.caption);
        if (metric.key == AiInsightUiModel.MetricKey.RATING) {
            tile.setTone(KpiTileView.Tone.SUCCESS);
        }
        if (metric.key == AiInsightUiModel.MetricKey.DAYS_LEFT) {
            tile.setTone(KpiTileView.Tone.WARNING);
        }
    }

    private String formatMetric(AiInsightUiModel.Metric metric) {
        double value = metric.value;
        switch (metric.key) {
            case FILL_RATE:
                return getString(R.string.hc_insight_metric_fill_value, oneDecimal(value));
            case CHECKIN_RATE:
                return getString(R.string.hc_insight_metric_fill_value, oneDecimal(value));
            case DAYS_LEFT:
                return getString(R.string.hc_insight_metric_days_value, (int) Math.round(value));
            case RATING:
                return oneDecimal(value);
            case REGISTRATIONS:
            default:
                return PriceTextView.formatPlain(Math.round(value));
        }
    }

    private void bindChart(AiInsightUiModel.Analysis analysis) {
        List<SimpleBarChartView.Bar> bars = new ArrayList<>();
        for (int i = 0; i < analysis.dailyRegistrations.size(); i++) {
            String label = i < analysis.dailyLabels.size() ? analysis.dailyLabels.get(i) : "";
            bars.add(new SimpleBarChartView.Bar(label, analysis.dailyRegistrations.get(i)));
        }
        chart.setData(bars);
        chartCard.setVisibility(bars.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void bindSummary(AiInsightUiModel.Analysis analysis) {
        boolean hasSummary = !analysis.summary.isEmpty();
        summaryCard.setVisibility(hasSummary ? View.VISIBLE : View.GONE);
        if (!hasSummary) {
            return;
        }
        boolean aiSource = analysis.source == AiInsightUiModel.Source.AI;
        summaryBadge.setVisibility(aiSource ? View.VISIBLE : View.GONE);
        if (aiSource) {
            summaryBadge.setReason(getString(R.string.hc_insight_generated,
                    timeFormat.format(new Date(analysis.generatedAt))));
        }
        ruleNotice.setVisibility(aiSource ? View.GONE : View.VISIBLE);

        summaryLayout.removeAllViews();
        for (String paragraph : analysis.summary) {
            addLine(summaryLayout, paragraph, R.style.TextAppearance_EventApp_Ds_BodyM,
                    R.color.ds_text);
        }
    }

    private void bindLimitations(AiInsightUiModel.Analysis analysis) {
        boolean hasLimitations = !analysis.limitations.isEmpty();
        limitationsCard.setVisibility(hasLimitations ? View.VISIBLE : View.GONE);
        limitationsLayout.removeAllViews();
        if (!hasLimitations) {
            return;
        }
        for (String limitation : analysis.limitations) {
            addLine(limitationsLayout,
                    getString(R.string.hc_insight_limitation_bullet, limitation),
                    R.style.TextAppearance_EventApp_Ds_Caption, R.color.ds_warning_dark);
        }
    }

    private void bindInsights(AiInsightUiModel.Analysis analysis) {
        boolean showList = !analysis.insufficientData;
        insightsHeader.setVisibility(showList ? View.VISIBLE : View.GONE);
        insightsList.setVisibility(showList ? View.VISIBLE : View.GONE);

        List<AiInsightUiModel.Insight> insights = showList
                ? analysis.insights
                : new ArrayList<>();
        adapter.submitList(insights);
        insightsHeader.setSubtitle(getString(R.string.hc_insight_list_subtitle, insights.size()));
        boolean empty = showList && insights.isEmpty();
        insightsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void addLine(LinearLayout parent, CharSequence text, @StyleRes int styleRes,
                         int colorRes) {
        TextView line = new TextView(requireContext());
        line.setText(text);
        line.setTextAppearance(styleRes);
        line.setTextColor(requireContext().getColor(colorRes));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(6);
        line.setLayoutParams(params);
        parent.addView(line);
    }

    private void openInsightAction(AiInsightUiModel.ActionTarget action) {
        if (action == null || action == AiInsightUiModel.ActionTarget.NONE) {
            return;
        }
        int destination;
        switch (action) {
            case EDIT_TICKETS:
                destination = R.id.ticket_type_listFragment;
                break;
            case VIEW_ORDERS:
                destination = R.id.order_listFragment;
                break;
            case EDIT_EVENT:
            default:
                destination = R.id.event_formFragment;
                break;
        }
        try {
            Navigation.findNavController(requireView()).navigate(destination);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Snackbar.make(requireView(), R.string.hc_coming_soon, Snackbar.LENGTH_SHORT).show();
        }
    }

    private String oneDecimal(double value) {
        return String.format(new Locale("vi", "VN"), "%.1f", value);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
