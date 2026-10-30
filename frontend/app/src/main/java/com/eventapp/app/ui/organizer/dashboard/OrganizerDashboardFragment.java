package com.eventapp.app.ui.organizer.dashboard;


import com.eventapp.app.ui.model.DashboardUiModel;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eventapp.app.R;
import com.eventapp.app.ui.model.DashboardStatusLabels;
import com.eventapp.app.ui.organizer.dashboard.adapter.DashboardTaskAdapter;
import com.eventapp.app.ui.organizer.dashboard.adapter.TopEventAdapter;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.KpiTileView;
import com.eventapp.app.ui.common.view.OfflineBanner;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.organizer.report.AiInsightFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * FR-O03 dashboard: cached-first snapshot with independently failing blocks, a range selector and
 * task/top-event lists that navigate to the owning module.
 */
public class OrganizerDashboardFragment extends Fragment {

    private static final String TAG_PICK_FROM = "dash_pick_from";
    private static final String TAG_PICK_TO = "dash_pick_to";

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", new Locale("vi", "VN"));
    private final SimpleDateFormat utcDayFormat =
            new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));

    private OrganizerDashboardViewModel viewModel;
    private DashboardTaskAdapter taskAdapter;
    private TopEventAdapter topAdapter;

    private SwipeRefreshLayout swipeRefresh;
    private View scrollContent;
    private SkeletonLayout skeleton;
    private EmptyStateView emptyState;
    private ErrorStateView errorState;
    private OfflineBanner offlineBanner;
    private TextView greeting;
    private TextView updatedAt;
    private TextView suspendedBanner;
    private StatusChip readOnlyChip;
    private ChipGroup rangeGroup;
    private View customRangeRow;
    private MaterialButton rangeFrom;
    private MaterialButton rangeTo;
    private View financeBody;
    private View financeError;
    private TextView financeValue;
    private TextView pendingValue;
    private TextView refundedValue;
    private TextView feeValue;
    private TextView netValue;
    private StatusChip financeChange;
    private TextView reviewGroupLabel;
    private TextView lifecycleGroupLabel;
    private ChipGroup reviewChips;
    private ChipGroup lifecycleChips;
    private KpiTileView registrationsTile;
    private KpiTileView ticketsTile;
    private KpiTileView checkinTile;
    private KpiTileView ratingTile;
    private RecyclerView taskList;
    private TextView tasksEmpty;
    private RecyclerView topEventList;
    private TextView topEventsEmpty;
    private MaterialButton aiInsightButton;
    private SectionHeaderView statusHeader;
    private SectionHeaderView tasksHeader;
    private SectionHeaderView topEventsHeader;

    private boolean bindingRange;
    private DashboardUiModel.Snapshot current;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_dashboard, container,
                false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        utcDayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        viewModel = new ViewModelProvider(this).get(OrganizerDashboardViewModel.class);
        bindViews(view);
        setupHeaders();
        setupLists();
        setupRange();
        setupStates();
        observe();
    }

    private void bindViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefreshDashboard);
        scrollContent = view.findViewById(R.id.scrollDashboard);
        skeleton = view.findViewById(R.id.skeletonDashboard);
        emptyState = view.findViewById(R.id.emptyDashboard);
        errorState = view.findViewById(R.id.errorDashboard);
        offlineBanner = view.findViewById(R.id.offlineDashboard);
        greeting = view.findViewById(R.id.tvDashboardGreeting);
        updatedAt = view.findViewById(R.id.tvDashboardUpdatedAt);
        suspendedBanner = view.findViewById(R.id.bannerDashboardSuspended);
        readOnlyChip = view.findViewById(R.id.chipDashboardReadOnly);
        rangeGroup = view.findViewById(R.id.chipGroupDashboardRange);
        customRangeRow = view.findViewById(R.id.layoutDashCustomRange);
        rangeFrom = view.findViewById(R.id.btnDashRangeFrom);
        rangeTo = view.findViewById(R.id.btnDashRangeTo);
        financeBody = view.findViewById(R.id.layoutDashFinanceBody);
        financeError = view.findViewById(R.id.layoutDashFinanceError);
        financeValue = view.findViewById(R.id.tvDashFinanceValue);
        pendingValue = view.findViewById(R.id.tvDashPending);
        refundedValue = view.findViewById(R.id.tvDashRefunded);
        feeValue = view.findViewById(R.id.tvDashFee);
        netValue = view.findViewById(R.id.tvDashFinanceNet);
        financeChange = view.findViewById(R.id.chipDashFinanceChange);
        reviewGroupLabel = view.findViewById(R.id.tvDashReviewGroup);
        lifecycleGroupLabel = view.findViewById(R.id.tvDashLifeGroup);
        reviewChips = view.findViewById(R.id.chipGroupDashReview);
        lifecycleChips = view.findViewById(R.id.chipGroupDashLife);
        registrationsTile = view.findViewById(R.id.tileDashRegistrations);
        ticketsTile = view.findViewById(R.id.tileDashTickets);
        checkinTile = view.findViewById(R.id.tileDashCheckin);
        ratingTile = view.findViewById(R.id.tileDashRating);
        taskList = view.findViewById(R.id.rvDashTasks);
        tasksEmpty = view.findViewById(R.id.tvDashTasksEmpty);
        topEventList = view.findViewById(R.id.rvDashTopEvents);
        topEventsEmpty = view.findViewById(R.id.tvDashTopEmpty);
        aiInsightButton = view.findViewById(R.id.btnDashAiInsight);
        statusHeader = view.findViewById(R.id.headerDashStatus);
        tasksHeader = view.findViewById(R.id.headerDashTasks);
        topEventsHeader = view.findViewById(R.id.headerDashTopEvents);
    }

    private void setupHeaders() {
        statusHeader.setTitle(R.string.hc_odash_status_title);
        statusHeader.setSubtitle(R.string.hc_odash_status_subtitle);
        statusHeader.setActionText(R.string.hc_odash_events_action);
        statusHeader.setOnActionClickListener(v -> navigate(R.id.my_eventsFragment, null));

        tasksHeader.setTitle(R.string.hc_odash_tasks_title);
        tasksHeader.setSubtitle(R.string.hc_odash_tasks_subtitle);
        tasksHeader.showAction(false);

        topEventsHeader.setTitle(R.string.hc_odash_top_title);
        topEventsHeader.setSubtitle(R.string.hc_odash_top_subtitle);
        topEventsHeader.showAction(false);

        requireView().findViewById(R.id.btnDashNotifications).setOnClickListener(v ->
                Snackbar.make(requireView(), R.string.hc_coming_soon, Snackbar.LENGTH_SHORT).show());
        aiInsightButton.setOnClickListener(v -> openFirstEventInsight());
    }

    private void setupLists() {
        taskAdapter = new DashboardTaskAdapter();
        taskAdapter.setOnTaskClickListener(task -> openTask(task.kind));
        taskList.setLayoutManager(new LinearLayoutManager(requireContext()));
        taskList.setItemAnimator(null);
        taskList.setAdapter(taskAdapter);

        topAdapter = new TopEventAdapter();
        topAdapter.setOnTopEventClickListener(event ->
                openInsight(event.id, event.title));
        topEventList.setLayoutManager(new LinearLayoutManager(requireContext()));
        topEventList.setItemAnimator(null);
        topEventList.setAdapter(topAdapter);
    }

    private void setupRange() {
        rangeGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingRange || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipDashRange7) {
                viewModel.setRange(OrganizerDashboardViewModel.Range.LAST_7_DAYS);
            } else if (id == R.id.chipDashRange30) {
                viewModel.setRange(OrganizerDashboardViewModel.Range.LAST_30_DAYS);
            } else {
                customRangeRow.setVisibility(View.VISIBLE);
                if (viewModel.getCustomFrom() <= 0 || viewModel.getCustomTo() <= 0) {
                    pickRangeDate(viewModel.getCustomFrom() <= 0);
                }
            }
        });

        rangeFrom.setOnClickListener(v -> pickRangeDate(true));
        rangeTo.setOnClickListener(v -> pickRangeDate(false));
    }

    private void setupStates() {
        swipeRefresh.setColorSchemeResources(R.color.ds_primary);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());

        offlineBanner.setOnRetryClickListener(v -> viewModel.refresh());

        emptyState.setIcon(R.drawable.ic_ds_event);
        emptyState.setTitle(R.string.hc_odash_empty_title);
        emptyState.setMessage(R.string.hc_odash_empty_message);
        emptyState.setActionText(R.string.hc_odash_empty_action);
        emptyState.setOnActionClickListener(v -> navigate(R.id.event_formFragment, null));

        errorState.setTitle(R.string.hc_odash_error_title);
        errorState.setMessage(R.string.hc_odash_error_message);
        errorState.setOnRetryClickListener(v -> viewModel.refresh());

        requireView().findViewById(R.id.btnDashFinanceRetry).setOnClickListener(v ->
                viewModel.refresh());
    }

    private void observe() {
        viewModel.getRange().observe(getViewLifecycleOwner(), range -> {
            bindingRange = true;
            if (range == OrganizerDashboardViewModel.Range.LAST_7_DAYS) {
                rangeGroup.check(R.id.chipDashRange7);
            } else if (range == OrganizerDashboardViewModel.Range.CUSTOM) {
                rangeGroup.check(R.id.chipDashRangeCustom);
            } else {
                rangeGroup.check(R.id.chipDashRange30);
            }
            bindingRange = false;
            customRangeRow.setVisibility(
                    range == OrganizerDashboardViewModel.Range.CUSTOM ? View.VISIBLE : View.GONE);
            renderCustomRange();
        });

        viewModel.getSnapshot().observe(getViewLifecycleOwner(), snapshot -> {
            current = snapshot;
            render();
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            swipeRefresh.setRefreshing(Boolean.TRUE.equals(loading));
            render();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> render());
    }

    private void render() {
        boolean loading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        boolean failed = Boolean.TRUE.equals(viewModel.getError().getValue());
        boolean empty = current != null && !current.hasEvents();
        boolean showSkeleton = loading && (current == null || empty);

        skeleton.setVisibility(showSkeleton && !failed ? View.VISIBLE : View.GONE);
        errorState.setVisibility(failed ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(!failed && empty && !loading ? View.VISIBLE : View.GONE);
        scrollContent.setVisibility(!failed && !empty ? View.VISIBLE : View.GONE);

        if (failed || empty || current == null) {
            return;
        }

        DashboardUiModel.Snapshot snapshot = current;
        greeting.setText(snapshot.organizerName.isEmpty()
                ? getString(R.string.hc_odash_title)
                : getString(R.string.hc_odash_greeting, snapshot.organizerName));
        suspendedBanner.setVisibility(snapshot.readOnly ? View.VISIBLE : View.GONE);
        readOnlyChip.setStatus(getString(R.string.hc_odash_read_only), StatusChip.Tone.WARNING);
        readOnlyChip.setVisibility(snapshot.readOnly ? View.VISIBLE : View.GONE);
        updatedAt.setText(getString(R.string.hc_odash_updated_at,
                timeFormat.format(new Date(snapshot.asOf))));

        offlineBanner.setVisibility(snapshot.fromCache ? View.VISIBLE : View.GONE);
        if (snapshot.fromCache) {
            offlineBanner.setCachedAt(timeFormat.format(new Date(snapshot.asOf)));
        }

        bindFinance(snapshot.finance);
        bindStatus(snapshot.statusCounts);
        bindMetrics(snapshot);

        taskAdapter.submitList(snapshot.tasks);
        tasksEmpty.setVisibility(snapshot.tasks.isEmpty() ? View.VISIBLE : View.GONE);
        tasksHeader.setSubtitle(snapshot.tasks.isEmpty()
                ? getString(R.string.hc_odash_tasks_subtitle)
                : getString(R.string.hc_odash_tasks_count, snapshot.tasks.size()));

        topAdapter.submitList(snapshot.topEvents);
        topEventsEmpty.setVisibility(snapshot.topEvents.isEmpty() ? View.VISIBLE : View.GONE);
        aiInsightButton.setEnabled(!snapshot.topEvents.isEmpty());
        aiInsightButton.setAlpha(snapshot.topEvents.isEmpty() ? 0.5f : 1f);
    }

    private void bindFinance(DashboardUiModel.Finance finance) {
        boolean available = finance != null && finance.isAvailable();
        financeBody.setVisibility(available ? View.VISIBLE : View.GONE);
        financeError.setVisibility(available ? View.GONE : View.VISIBLE);
        financeChange.setVisibility(View.GONE);
        if (!available) {
            return;
        }
        financeValue.setText(PriceTextView.formatVnd(finance.verifiedAmount));
        pendingValue.setText(PriceTextView.formatVnd(finance.pendingAmount));
        refundedValue.setText(PriceTextView.formatVnd(finance.refundedAmount));
        feeValue.setText(PriceTextView.formatVnd(finance.feeAmount));
        netValue.setText(getString(R.string.hc_odash_finance_net,
                PriceTextView.formatVnd(finance.netAmount)));
        if (finance.changePercent != null) {
            financeChange.setStatus(
                    getString(R.string.hc_odash_finance_change, finance.changePercent),
                    StatusChip.Tone.SUCCESS);
            financeChange.setVisibility(View.VISIBLE);
        }
    }

    private void bindStatus(List<DashboardUiModel.StatusCount> counts) {
        reviewChips.removeAllViews();
        lifecycleChips.removeAllViews();
        int reviewCount = 0;
        int lifecycleCount = 0;
        for (DashboardUiModel.StatusCount item : counts) {
            StatusChip chip = new StatusChip(requireContext());
            String label;
            StatusChip.Tone tone;
            if (item.isReview()) {
                label = DashboardStatusLabels.reviewLabel(requireContext(), item.reviewStage);
                tone = DashboardStatusLabels.reviewTone(item.reviewStage);
                reviewChips.addView(chip);
                reviewCount++;
            } else {
                label = DashboardStatusLabels.lifecycleLabel(requireContext(), item.lifecycleStage);
                tone = DashboardStatusLabels.lifecycleTone(item.lifecycleStage);
                lifecycleChips.addView(chip);
                lifecycleCount++;
            }
            chip.setStatus(getString(R.string.hc_odash_status_count, label, item.count), tone);
        }
        reviewGroupLabel.setVisibility(reviewCount == 0 ? View.GONE : View.VISIBLE);
        reviewChips.setVisibility(reviewCount == 0 ? View.GONE : View.VISIBLE);
        lifecycleGroupLabel.setVisibility(lifecycleCount == 0 ? View.GONE : View.VISIBLE);
        lifecycleChips.setVisibility(lifecycleCount == 0 ? View.GONE : View.VISIBLE);
    }

    private void bindMetrics(DashboardUiModel.Snapshot snapshot) {
        bindMetric(registrationsTile, snapshot.metric(DashboardUiModel.MetricKey.REGISTRATIONS),
                DashboardUiModel.MetricKey.REGISTRATIONS, R.string.hc_odash_metric_registrations);
        bindMetric(ticketsTile, snapshot.metric(DashboardUiModel.MetricKey.TICKETS),
                DashboardUiModel.MetricKey.TICKETS, R.string.hc_odash_metric_tickets);
        bindMetric(checkinTile, snapshot.metric(DashboardUiModel.MetricKey.CHECKIN),
                DashboardUiModel.MetricKey.CHECKIN, R.string.hc_odash_metric_checkin);
        bindMetric(ratingTile, snapshot.metric(DashboardUiModel.MetricKey.RATING),
                DashboardUiModel.MetricKey.RATING, R.string.hc_odash_metric_rating);
    }

    private void bindMetric(KpiTileView tile, @Nullable DashboardUiModel.Metric metric,
                            DashboardUiModel.MetricKey key, int labelRes) {
        tile.setLabel(labelRes);
        tile.clearError();
        tile.setTone(key == DashboardUiModel.MetricKey.RATING
                ? KpiTileView.Tone.SUCCESS
                : KpiTileView.Tone.DEFAULT);

        if (metric == null || !metric.isAvailable()) {
            tile.setDelta(null, KpiTileView.Tone.DEFAULT);
            tile.setCaption(null);
            tile.showError(R.string.hc_odash_metric_error, v -> viewModel.retryMetric(key));
            return;
        }

        tile.setValue(formatMetric(metric));
        tile.setCaption(metric.caption);
        if (metric.deltaPercent == null) {
            tile.setDelta(null, KpiTileView.Tone.DEFAULT);
        } else {
            tile.setDelta(getString(R.string.hc_odash_metric_delta, metric.deltaPercent),
                    KpiTileView.Tone.SUCCESS);
        }
    }

    private String formatMetric(DashboardUiModel.Metric metric) {
        if (metric.key == DashboardUiModel.MetricKey.RATING) {
            return String.format(new Locale("vi", "VN"), "%.1f", metric.value);
        }
        return PriceTextView.formatPlain(Math.round(metric.value));
    }

    private void pickRangeDate(boolean from) {
        long selection = from ? viewModel.getCustomFrom() : viewModel.getCustomTo();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(from
                        ? R.string.hc_odash_range_pick_from
                        : R.string.hc_odash_range_pick_to)
                .setSelection(selection > 0 ? selection
                        : MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        picker.addOnPositiveButtonClickListener(value -> applyRangeDate(from, value));
        picker.addOnCancelListener(dialog -> resetRangeChipIfUnset());
        picker.show(getParentFragmentManager(), from ? TAG_PICK_FROM : TAG_PICK_TO);
    }

    private void applyRangeDate(boolean from, long utcMillis) {
        long fromMillis = from ? utcMillis : viewModel.getCustomFrom();
        long toMillis = from ? viewModel.getCustomTo() : utcMillis;
        if (!viewModel.setCustomRange(fromMillis, toMillis)) {
            Snackbar.make(requireView(), R.string.hc_odash_range_invalid,
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        renderCustomRange();
        if (from && viewModel.getCustomTo() <= 0) {
            pickRangeDate(false);
        }
    }

    /** Cancelling the first custom picker leaves the previous range checked. */
    private void resetRangeChipIfUnset() {
        if (viewModel.getCustomFrom() > 0 || viewModel.getCustomTo() > 0) {
            return;
        }
        OrganizerDashboardViewModel.Range range = viewModel.getRange().getValue();
        if (range == null || range == OrganizerDashboardViewModel.Range.CUSTOM) {
            return;
        }
        bindingRange = true;
        rangeGroup.check(range == OrganizerDashboardViewModel.Range.LAST_7_DAYS
                ? R.id.chipDashRange7
                : R.id.chipDashRange30);
        bindingRange = false;
        customRangeRow.setVisibility(View.GONE);
    }

    private void renderCustomRange() {
        rangeFrom.setText(viewModel.getCustomFrom() > 0
                ? getString(R.string.hc_odash_range_from,
                utcDayFormat.format(new Date(viewModel.getCustomFrom())))
                : getString(R.string.hc_odash_range_from_any));
        rangeTo.setText(viewModel.getCustomTo() > 0
                ? getString(R.string.hc_odash_range_to,
                utcDayFormat.format(new Date(viewModel.getCustomTo())))
                : getString(R.string.hc_odash_range_to_any));
    }

    private void openTask(DashboardUiModel.TaskKind kind) {
        int destination;
        switch (kind) {
            case REFUND_PENDING:
                destination = R.id.organizer_refundsFragment;
                break;
            case TICKET_SALE_ENDING:
                destination = R.id.ticket_type_listFragment;
                break;
            case EVENT_STARTING_SOON:
            case EVENT_REVISION:
            case EVENT_REJECTED:
            default:
                destination = R.id.my_eventsFragment;
                break;
        }
        navigate(destination, null);
    }

    private void openFirstEventInsight() {
        if (current == null || current.topEvents.isEmpty()) {
            return;
        }
        DashboardUiModel.TopEvent event = current.topEvents.get(0);
        openInsight(event.id, event.title);
    }

    private void openInsight(long eventId, String eventTitle) {
        Bundle args = new Bundle();
        args.putLong(AiInsightFragment.ARG_EVENT_ID, eventId);
        args.putString(AiInsightFragment.ARG_EVENT_TITLE, eventTitle);
        navigate(R.id.ai_insightFragment, args);
    }

    private void navigate(int destination, @Nullable Bundle args) {
        try {
            if (args == null) {
                Navigation.findNavController(requireView()).navigate(destination);
            } else {
                Navigation.findNavController(requireView()).navigate(destination, args);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Destination belongs to another module or is already on top.
            Snackbar.make(requireView(), R.string.hc_coming_soon, Snackbar.LENGTH_SHORT).show();
        }
    }
}
