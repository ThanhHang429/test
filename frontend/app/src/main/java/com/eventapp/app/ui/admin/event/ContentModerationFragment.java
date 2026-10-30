package com.eventapp.app.ui.admin.event;


import com.eventapp.app.ui.model.ModerationUiModel;
import com.eventapp.app.ui.admin.event.dialog.ModerationReasonDialog;
import com.eventapp.app.ui.admin.event.dialog.ModerationReviewSheet;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eventapp.app.R;
import com.eventapp.app.ui.admin.event.adapter.ModerationFlagAdapter;
import com.eventapp.app.ui.common.adapter.ReviewAdapter;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.KpiTileView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.eventapp.app.ui.model.ReviewUiModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

/**
 * FR-A07 + FR-A14 moderation screen. One destination, two lanes: content flags and reported
 * reviews. Actions update the local queue, show an audit Snackbar and offer undo.
 */
public class ContentModerationFragment extends Fragment {

    private ContentModerationViewModel viewModel;
    private ModerationFlagAdapter flagAdapter;
    private ReviewAdapter reviewAdapter;

    private TabLayout tabs;
    private TextView subtitle;
    private KpiTileView kpiOpen;
    private KpiTileView kpiHigh;
    private KpiTileView kpiHandled;
    private View groupModFilters;
    private View groupA14Filters;
    private ChipGroup chipGroupModTarget;
    private ChipGroup chipGroupModSeverity;
    private ChipGroup chipGroupModStatus;
    private ChipGroup chipGroupA14Source;
    private RecyclerView flagsList;
    private RecyclerView reviewsList;
    private SwipeRefreshLayout swipeRefresh;
    private SkeletonLayout skeleton;
    private EmptyStateView emptyState;
    private ErrorStateView errorState;

    private boolean bindingTabs;
    private boolean bindingFilters;
    private boolean firstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_event_content_moderation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ContentModerationViewModel.class);
        bindViews(view);
        setupKpis();
        setupToolbar();
        setupTabs();
        setupLists();
        setupFilters();
        setupStates();
        setupDialogs();
        restoreFilters();
        observe();
    }

    private void bindViews(View view) {
        tabs = view.findViewById(R.id.tabsModeration);
        subtitle = view.findViewById(R.id.tvModSubtitle);
        kpiOpen = view.findViewById(R.id.kpiModOpen);
        kpiHigh = view.findViewById(R.id.kpiModHigh);
        kpiHandled = view.findViewById(R.id.kpiModHandled);
        groupModFilters = view.findViewById(R.id.groupModFilters);
        groupA14Filters = view.findViewById(R.id.groupA14Filters);
        chipGroupModTarget = view.findViewById(R.id.chipGroupModTarget);
        chipGroupModSeverity = view.findViewById(R.id.chipGroupModSeverity);
        chipGroupModStatus = view.findViewById(R.id.chipGroupModStatus);
        chipGroupA14Source = view.findViewById(R.id.chipGroupA14Source);
        flagsList = view.findViewById(R.id.rvModerationFlags);
        reviewsList = view.findViewById(R.id.rvModerationReviews);
        swipeRefresh = view.findViewById(R.id.swipeRefreshModeration);
        skeleton = view.findViewById(R.id.skeletonModeration);
        emptyState = view.findViewById(R.id.emptyModeration);
        errorState = view.findViewById(R.id.errorModeration);
    }

    private void setupKpis() {
        kpiOpen.setLabel(R.string.hc_mod_kpi_open);
        kpiOpen.setCaption(getString(R.string.hc_mod_kpi_open_caption));
        kpiOpen.setTone(KpiTileView.Tone.WARNING);

        kpiHigh.setLabel(R.string.hc_mod_kpi_high);
        kpiHigh.setCaption(getString(R.string.hc_mod_kpi_high_caption));
        kpiHigh.setTone(KpiTileView.Tone.DANGER);

        kpiHandled.setLabel(R.string.hc_mod_kpi_handled);
        kpiHandled.setCaption(getString(R.string.hc_mod_kpi_handled_caption));
        kpiHandled.setTone(KpiTileView.Tone.SUCCESS);
    }

    private void setupToolbar() {
        requireView().findViewById(R.id.btnModBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
    }

    private void setupTabs() {
        tabs.addTab(tabs.newTab().setText(R.string.hc_mod_tab_flags));
        tabs.addTab(tabs.newTab().setText(R.string.hc_mod_tab_reviews));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!bindingTabs) {
                    viewModel.selectTab(tab.getPosition());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupLists() {
        flagAdapter = new ModerationFlagAdapter();
        flagAdapter.setOnFlagActionListener(new ModerationFlagAdapter.OnFlagActionListener() {
            @Override
            public void onConfirm(ModerationUiModel flag) {
                viewModel.confirmFlag(flag.id);
            }

            @Override
            public void onDismiss(ModerationUiModel flag) {
                showReasonDialog(ModerationReasonDialog.ACTION_DISMISS_FLAG, flag.id,
                        R.string.hc_mod_dismiss_title,
                        R.string.hc_mod_dismiss_message,
                        R.string.hc_mod_dismiss_confirm,
                        false);
            }

            @Override
            public void onRescan(ModerationUiModel flag) {
                viewModel.rescanFlag(flag.id);
            }
        });
        flagsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        flagsList.setAdapter(flagAdapter);

        reviewAdapter = new ReviewAdapter(ReviewAdapter.Mode.MODERATION);
        reviewAdapter.setShowEventName(true);
        reviewAdapter.setOnReviewActionListener(new ReviewAdapter.OnReviewActionListener() {
            @Override
            public void onEditReview(ReviewUiModel review) {
            }

            @Override
            public void onReportReview(ReviewUiModel review) {
            }

            @Override
            public void onModerateReview(ReviewUiModel review) {
                openReviewSheet(review);
            }
        });
        reviewsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        reviewsList.setAdapter(reviewAdapter);
    }

    private void setupFilters() {
        chipGroupModTarget.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingFilters || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipModTargetEvent) {
                viewModel.setTargetFilter(ModerationUiModel.TargetType.EVENT_TEXT);
            } else if (id == R.id.chipModTargetImage) {
                viewModel.setTargetFilter(ModerationUiModel.TargetType.EVENT_IMAGE);
            } else if (id == R.id.chipModTargetTag) {
                viewModel.setTargetFilter(ModerationUiModel.TargetType.TAG);
            } else if (id == R.id.chipModTargetReview) {
                viewModel.setTargetFilter(ModerationUiModel.TargetType.REVIEW);
            } else {
                viewModel.setTargetFilter(null);
            }
        });

        chipGroupModSeverity.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingFilters || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipModSeverityHigh) {
                viewModel.setSeverityFilter(ModerationUiModel.Severity.HIGH);
            } else if (id == R.id.chipModSeverityMedium) {
                viewModel.setSeverityFilter(ModerationUiModel.Severity.MEDIUM);
            } else if (id == R.id.chipModSeverityLow) {
                viewModel.setSeverityFilter(ModerationUiModel.Severity.LOW);
            } else {
                viewModel.setSeverityFilter(null);
            }
        });

        chipGroupModStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingFilters || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipModStatusConfirmed) {
                viewModel.setStatusFilter(ContentModerationViewModel.StatusFilter.CONFIRMED);
            } else if (id == R.id.chipModStatusDismissed) {
                viewModel.setStatusFilter(ContentModerationViewModel.StatusFilter.DISMISSED);
            } else if (id == R.id.chipModStatusHandled) {
                viewModel.setStatusFilter(ContentModerationViewModel.StatusFilter.HANDLED);
            } else if (id == R.id.chipModStatusAll) {
                viewModel.setStatusFilter(ContentModerationViewModel.StatusFilter.ALL);
            } else {
                viewModel.setStatusFilter(ContentModerationViewModel.StatusFilter.OPEN);
            }
        });

        chipGroupA14Source.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingFilters || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipA14SourceHeld) {
                viewModel.setReviewSourceFilter(ReviewUiModel.Source.HELD);
            } else if (id == R.id.chipA14SourceFlagged) {
                viewModel.setReviewSourceFilter(ReviewUiModel.Source.FLAGGED);
            } else if (id == R.id.chipA14SourceReported) {
                viewModel.setReviewSourceFilter(ReviewUiModel.Source.REPORTED);
            } else {
                viewModel.setReviewSourceFilter(null);
            }
        });
    }

    private void setupStates() {
        swipeRefresh.setColorSchemeResources(R.color.ds_primary);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());

        emptyState.setIcon(R.drawable.ic_ds_flag);
        emptyState.setOnActionClickListener(v -> {
            Integer tab = viewModel.getSelectedTab().getValue();
            if (tab != null && tab == 1) {
                viewModel.clearReviewSourceFilter();
                selectChip(R.id.chipA14SourceAll);
            } else {
                viewModel.showHandled();
                selectChip(R.id.chipModStatusHandled);
            }
        });

        errorState.setTitle(R.string.hc_mod_error_title);
        errorState.setMessage(R.string.hc_mod_error_message);
        errorState.setRetryText(R.string.hc_retry);
        errorState.setOnRetryClickListener(v -> viewModel.retry());
    }

    private void setupDialogs() {
        getChildFragmentManager().setFragmentResultListener(
                ModerationReasonDialog.REQUEST_KEY, this, (requestKey, result) ->
                        handleReasonResult(result));
        getChildFragmentManager().setFragmentResultListener(
                ModerationReviewSheet.REQUEST_KEY, this, (requestKey, result) ->
                        handleReviewResult(result));
    }

    private void restoreFilters() {
        bindingFilters = true;
        ModerationUiModel.TargetType target = viewModel.getTargetFilter();
        if (target == ModerationUiModel.TargetType.EVENT_TEXT) {
            selectChip(R.id.chipModTargetEvent);
        } else if (target == ModerationUiModel.TargetType.EVENT_IMAGE) {
            selectChip(R.id.chipModTargetImage);
        } else if (target == ModerationUiModel.TargetType.TAG) {
            selectChip(R.id.chipModTargetTag);
        } else if (target == ModerationUiModel.TargetType.REVIEW) {
            selectChip(R.id.chipModTargetReview);
        } else {
            selectChip(R.id.chipModTargetAll);
        }

        ModerationUiModel.Severity severity = viewModel.getSeverityFilter();
        if (severity == ModerationUiModel.Severity.HIGH) {
            selectChip(R.id.chipModSeverityHigh);
        } else if (severity == ModerationUiModel.Severity.MEDIUM) {
            selectChip(R.id.chipModSeverityMedium);
        } else if (severity == ModerationUiModel.Severity.LOW) {
            selectChip(R.id.chipModSeverityLow);
        } else {
            selectChip(R.id.chipModSeverityAll);
        }

        switch (viewModel.getStatusFilter()) {
            case CONFIRMED:
                selectChip(R.id.chipModStatusConfirmed);
                break;
            case DISMISSED:
                selectChip(R.id.chipModStatusDismissed);
                break;
            case HANDLED:
                selectChip(R.id.chipModStatusHandled);
                break;
            case ALL:
                selectChip(R.id.chipModStatusAll);
                break;
            case OPEN:
            default:
                selectChip(R.id.chipModStatusOpen);
                break;
        }

        ReviewUiModel.Source source = viewModel.getReviewSourceFilter();
        if (source == ReviewUiModel.Source.HELD) {
            selectChip(R.id.chipA14SourceHeld);
        } else if (source == ReviewUiModel.Source.FLAGGED) {
            selectChip(R.id.chipA14SourceFlagged);
        } else if (source == ReviewUiModel.Source.REPORTED) {
            selectChip(R.id.chipA14SourceReported);
        } else {
            selectChip(R.id.chipA14SourceAll);
        }
        bindingFilters = false;
    }

    private void selectChip(int id) {
        Chip chip = requireView().findViewById(id);
        if (chip != null && !chip.isChecked()) {
            chip.setChecked(true);
        }
    }

    private void observe() {
        viewModel.getSelectedTab().observe(getViewLifecycleOwner(), this::renderTab);

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            swipeRefresh.setRefreshing(false);
            skeleton.setVisibility(isLoading && firstLoad ? View.VISIBLE : View.GONE);
            if (!isLoading) {
                firstLoad = false;
            }
            renderContent();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> renderContent());

        viewModel.getFlags().observe(getViewLifecycleOwner(), items -> {
            flagAdapter.submitList(items);
            renderContent();
        });

        viewModel.getReviews().observe(getViewLifecycleOwner(), items -> {
            reviewAdapter.submitList(items);
            renderContent();
        });

        viewModel.getOpenCount().observe(getViewLifecycleOwner(), count ->
                kpiOpen.setValue(String.valueOf(count == null ? 0 : count)));
        viewModel.getHighCount().observe(getViewLifecycleOwner(), count ->
                kpiHigh.setValue(String.valueOf(count == null ? 0 : count)));
        viewModel.getHandledToday().observe(getViewLifecycleOwner(), count ->
                kpiHandled.setValue(String.valueOf(count == null ? 0 : count)));

        viewModel.getActionEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) {
                return;
            }
            showAction(event);
            viewModel.consumeActionEvent();
        });
    }

    private void renderTab(Integer tab) {
        int index = tab == null ? 0 : tab;
        bindingTabs = true;
        TabLayout.Tab target = tabs.getTabAt(index);
        if (target != null && !target.isSelected()) {
            target.select();
        }
        bindingTabs = false;
        renderContent();
    }

    private void renderContent() {
        if (firstLoad) {
            return;
        }
        boolean loading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        boolean failed = Boolean.TRUE.equals(viewModel.getError().getValue());
        int tab = viewModel.getSelectedTab().getValue() == null
                ? 0 : viewModel.getSelectedTab().getValue();
        boolean flagsTab = tab == 0;
        int itemCount = flagsTab ? flagAdapter.getItemCount() : reviewAdapter.getItemCount();
        boolean empty = itemCount == 0;

        skeleton.setVisibility(loading && firstLoad ? View.VISIBLE : View.GONE);
        errorState.setVisibility(failed ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(!failed && !loading && empty ? View.VISIBLE : View.GONE);
        swipeRefresh.setVisibility(!failed && !empty ? View.VISIBLE : View.GONE);

        groupModFilters.setVisibility(flagsTab && !failed ? View.VISIBLE : View.GONE);
        groupA14Filters.setVisibility(!flagsTab && !failed ? View.VISIBLE : View.GONE);
        flagsList.setVisibility(flagsTab && !failed && !empty ? View.VISIBLE : View.GONE);
        reviewsList.setVisibility(!flagsTab && !failed && !empty ? View.VISIBLE : View.GONE);

        if (flagsTab) {
            emptyState.setTitle(R.string.hc_mod_empty_title);
            emptyState.setMessage(R.string.hc_mod_empty_message);
            emptyState.setActionText(R.string.hc_mod_empty_action);
        } else {
            emptyState.setTitle(R.string.hc_mod_a14_empty_title);
            emptyState.setMessage(R.string.hc_mod_a14_empty_message);
            emptyState.setActionText(R.string.hc_mod_a14_empty_action);
        }

        Integer open = viewModel.getOpenCount().getValue();
        subtitle.setText(getString(R.string.hc_mod_subtitle,
                open == null ? 0 : open, reviewAdapter.getItemCount()));
    }

    private void showAction(ContentModerationViewModel.ActionEvent event) {
        int messageRes;
        switch (event.action) {
            case CONFIRM:
                messageRes = R.string.hc_mod_action_confirmed;
                break;
            case DISMISS:
                messageRes = R.string.hc_mod_action_dismissed;
                break;
            case RESCAN:
                messageRes = R.string.hc_mod_action_rescanned;
                break;
            case HIDE:
                messageRes = R.string.hc_a14_action_hidden;
                break;
            case KEEP:
                messageRes = R.string.hc_a14_action_kept;
                break;
            case REQUEST_EDIT:
                messageRes = R.string.hc_a14_action_edit;
                break;
            case UNDO:
            default:
                messageRes = R.string.hc_mod_action_undone;
                break;
        }

        String message = getString(messageRes);
        if (event.action != ContentModerationViewModel.Action.UNDO) {
            message = getString(R.string.hc_mod_snackbar, message,
                    getString(R.string.hc_mod_audit_note));
        }
        Snackbar snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG);
        if (event.undoable) {
            snackbar.setAction(R.string.hc_mod_undo, v -> viewModel.undoLast());
        }
        snackbar.show();
    }

    private void openReviewSheet(ReviewUiModel review) {
        ModerationReviewSheet.newInstance(review.id)
                .show(getChildFragmentManager(), "moderation_review");
    }

    private void handleReviewResult(Bundle result) {
        String action = result.getString(ModerationReviewSheet.RESULT_ACTION);
        long reviewId = result.getLong(ModerationReviewSheet.RESULT_REVIEW_ID);
        if (ModerationReviewSheet.ACTION_HIDE.equals(action)) {
            showReasonDialog(ModerationReasonDialog.ACTION_HIDE_REVIEW, reviewId,
                    R.string.hc_mod_hide_title,
                    R.string.hc_mod_hide_message,
                    R.string.hc_mod_hide_confirm,
                    true);
        } else if (ModerationReviewSheet.ACTION_KEEP.equals(action)) {
            showReasonDialog(ModerationReasonDialog.ACTION_KEEP_REVIEW, reviewId,
                    R.string.hc_mod_keep_title,
                    R.string.hc_mod_keep_message,
                    R.string.hc_mod_keep_confirm,
                    false);
        } else if (ModerationReviewSheet.ACTION_REQUEST_EDIT.equals(action)) {
            showReasonDialog(ModerationReasonDialog.ACTION_REQUEST_EDIT_REVIEW, reviewId,
                    R.string.hc_mod_edit_title,
                    R.string.hc_mod_edit_message,
                    R.string.hc_mod_edit_confirm,
                    true);
        }
    }

    private void handleReasonResult(Bundle result) {
        String action = result.getString(ModerationReasonDialog.RESULT_ACTION);
        long itemId = result.getLong(ModerationReasonDialog.RESULT_ITEM_ID);
        String code = result.getString(ModerationReasonDialog.RESULT_CODE);
        String note = result.getString(ModerationReasonDialog.RESULT_NOTE);
        if (ModerationReasonDialog.ACTION_DISMISS_FLAG.equals(action)) {
            viewModel.dismissFlag(itemId, note);
        } else if (ModerationReasonDialog.ACTION_HIDE_REVIEW.equals(action)) {
            viewModel.hideReview(itemId, code, note);
        } else if (ModerationReasonDialog.ACTION_KEEP_REVIEW.equals(action)) {
            viewModel.keepReview(itemId, note);
        } else if (ModerationReasonDialog.ACTION_REQUEST_EDIT_REVIEW.equals(action)) {
            viewModel.requestEditReview(itemId, code, note);
        }
    }

    private void showReasonDialog(String action, long itemId, @StringRes int titleRes,
                                  @StringRes int messageRes, @StringRes int confirmRes,
                                  boolean requireCode) {
        ModerationReasonDialog.newInstance(action, itemId, titleRes, messageRes, confirmRes,
                        requireCode)
                .show(getChildFragmentManager(), "moderation_reason");
    }
}
