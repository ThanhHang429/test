package com.eventapp.app.ui.user.home;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eventapp.app.R;
import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.ui.common.adapter.RecommendationAdapter;
import com.eventapp.app.ui.common.view.AiBadge;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** FR-U07 full suggestion list with reason, preference chips and refresh quota. */
public class RecommendFragment extends Fragment {

    private static final long REFRESH_LOCK_MS = 60_000L;

    private RecommendViewModel viewModel;
    private RecommendationAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private AiBadge aiBadge;
    private TextView title;
    private TextView meta;
    private TextView fallback;
    private TextView footer;
    private ChipGroup preferences;
    private SkeletonLayout skeleton;
    private EmptyStateView emptyState;
    private ErrorStateView errorState;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private RecommendViewModel.Meta metaValue;
    private boolean firstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_recommend, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RecommendViewModel.class);
        bindViews(view);
        setupToolbar();
        setupHeader();
        setupList();
        setupStates();
        observe();
    }

    private void bindViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefreshRecommend);
        recyclerView = view.findViewById(R.id.rvRecommend);
        aiBadge = view.findViewById(R.id.aiBadgeRecommend);
        title = view.findViewById(R.id.tvRecTitle);
        meta = view.findViewById(R.id.tvRecMeta);
        fallback = view.findViewById(R.id.tvRecFallback);
        footer = view.findViewById(R.id.tvRecFooter);
        preferences = view.findViewById(R.id.chipGroupRecPrefs);
        skeleton = view.findViewById(R.id.skeletonRecommend);
        emptyState = view.findViewById(R.id.emptyRecommend);
        errorState = view.findViewById(R.id.errorRecommend);
    }

    private void setupToolbar() {
        requireView().findViewById(R.id.btnRecBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
        requireView().findViewById(R.id.btnRecRefresh).setOnClickListener(v -> requestRefresh());
    }

    private void setupHeader() {
        aiBadge.setScore(null);
        aiBadge.setReason(R.string.hc_rec_rationale);
        aiBadge.showReason(true);

        SectionHeaderView header = requireView().findViewById(R.id.headerRecPrefs);
        header.setTitle(R.string.hc_rec_prefs_title);
        header.setActionText(R.string.hc_rec_change_prefs);
        header.setOnActionClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.preferencesFragment));
    }

    private void setupList() {
        adapter = new RecommendationAdapter();
        adapter.setOnRecommendationActionListener(new RecommendationAdapter
                .OnRecommendationActionListener() {
            @Override
            public void onRecommendationOpen(RecommendationUiModel item) {
                Bundle args = new Bundle();
                args.putLong(EventListFragment.ARG_EVENT_ID, item.event.id);
                args.putString(EventListFragment.ARG_EVENT_TITLE, item.event.title);
                Navigation.findNavController(requireView())
                        .navigate(R.id.event_detailFragment, args);
            }

            @Override
            public void onRecommendationDismiss(RecommendationUiModel item) {
                viewModel.dismiss(item);
                Snackbar.make(requireView(), R.string.hc_rec_dismissed, Snackbar.LENGTH_LONG)
                        .setAction(R.string.hc_fav_undo, v -> viewModel.undoDismiss())
                        .show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(adapter);
    }

    private void setupStates() {
        swipeRefresh.setColorSchemeResources(R.color.ds_primary);
        swipeRefresh.setOnRefreshListener(() -> {
            if (!viewModel.refresh()) {
                swipeRefresh.setRefreshing(false);
                Snackbar.make(requireView(), R.string.hc_rec_cooldown, Snackbar.LENGTH_SHORT)
                        .show();
            }
        });

        emptyState.setIcon(R.drawable.ic_ds_ai_sparkle);
        emptyState.setTitle(R.string.hc_rec_empty_title);
        emptyState.setMessage(R.string.hc_rec_empty_message);
        emptyState.setActionText(R.string.hc_rec_empty_action);
        emptyState.setOnActionClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.event_listFragment));

        errorState.setTitle(R.string.hc_rec_error_title);
        errorState.setMessage(R.string.hc_rec_error_message);
        errorState.setOnRetryClickListener(v -> viewModel.load());
    }

    private void observe() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            swipeRefresh.setRefreshing(false);
            skeleton.setVisibility(isLoading && firstLoad ? View.VISIBLE : View.GONE);
            if (!isLoading) {
                firstLoad = false;
            }
            renderContent();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            errorState.setVisibility(Boolean.TRUE.equals(error) ? View.VISIBLE : View.GONE);
            renderContent();
        });

        viewModel.getMeta().observe(getViewLifecycleOwner(), this::bindMeta);

        viewModel.getPreferences().observe(getViewLifecycleOwner(), this::bindPreferences);

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            updateMetaText();
            renderContent();
        });
    }

    private void bindMeta(RecommendViewModel.Meta value) {
        if (value == null) {
            return;
        }
        metaValue = value;
        title.setText(value.personalized
                ? R.string.hc_rec_personalized_title : R.string.hc_rec_popular_title);
        aiBadge.setReason(value.personalized
                ? R.string.hc_rec_rationale : R.string.hc_rec_popular_rationale);
        fallback.setVisibility(value.fallback ? View.VISIBLE : View.GONE);
        updateMetaText();
    }

    private void updateMetaText() {
        if (metaValue == null) {
            return;
        }
        String time = new SimpleDateFormat("HH:mm", new Locale("vi", "VN"))
                .format(new Date(metaValue.generatedAt));
        meta.setText(getString(R.string.hc_rec_meta, time, adapter.getItemCount()));
    }

    private void bindPreferences(List<EventCategory> categories) {
        preferences.removeAllViews();
        if (categories == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (EventCategory category : categories) {
            Chip chip = (Chip) inflater.inflate(R.layout.view_ds_tag_chip, preferences, false);
            chip.setCloseIconVisible(false);
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setChipIconResource(EventCategoryUi.iconRes(category));
            chip.setChipIconVisible(true);
            chip.setChipIconSize(dp(14));
            chip.setText(EventCategoryUi.label(requireContext(), category));
            preferences.addView(chip);
        }
    }

    private void renderContent() {
        if (firstLoad) {
            return;
        }
        boolean loading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        boolean failed = Boolean.TRUE.equals(viewModel.getError().getValue());
        boolean empty = adapter.getItemCount() == 0;
        View scroll = requireView().findViewById(R.id.scrollRecommend);

        skeleton.setVisibility(View.GONE);
        errorState.setVisibility(failed ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(!failed && !loading && empty ? View.VISIBLE : View.GONE);
        scroll.setVisibility(failed || empty ? View.GONE : View.VISIBLE);
        footer.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void requestRefresh() {
        if (viewModel.refresh()) {
            Snackbar.make(requireView(), R.string.hc_rec_refreshed, Snackbar.LENGTH_SHORT).show();
            lockRefreshButton();
        } else {
            Snackbar.make(requireView(), R.string.hc_rec_cooldown, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void lockRefreshButton() {
        View button = requireView().findViewById(R.id.btnRecRefresh);
        button.setEnabled(false);
        button.setAlpha(0.45f);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }
            button.setEnabled(true);
            button.setAlpha(1f);
        }, REFRESH_LOCK_MS);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }
}
