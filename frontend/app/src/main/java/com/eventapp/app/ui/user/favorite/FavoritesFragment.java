package com.eventapp.app.ui.user.favorite;


import com.eventapp.app.ui.model.FavoriteUiModel;
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
import com.eventapp.app.ui.common.adapter.EventAdapter;
import com.eventapp.app.ui.user.favorite.adapter.FavoriteAdapter;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.NavBarSpacing;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.eventapp.app.ui.user.home.EventListFragment;
import com.eventapp.app.ui.model.EventUiModel;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/** FR-U06 saved events: reminder switch, upcoming/past filters and a "you may like" rail. */
public class FavoritesFragment extends Fragment {

    private FavoritesViewModel viewModel;
    private FavoriteAdapter adapter;
    private EventAdapter suggestionAdapter;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private RecyclerView suggestionList;
    private View suggestionHeader;
    private TextView count;
    private ChipGroup filterGroup;
    private MaterialSwitch alertsSwitch;
    private SkeletonLayout skeleton;
    private EmptyStateView emptyState;
    private ErrorStateView errorState;

    private boolean bindingFilter;
    private boolean bindingSwitch;
    private boolean firstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        bindViews(view);
        setupToolbar();
        setupLists();
        setupFilters();
        setupStates();
        observe();
        NavBarSpacing.padBottomForNavBar(view, view.findViewById(R.id.scrollFavorites), 8);
    }

    private void bindViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefreshFavorites);
        recyclerView = view.findViewById(R.id.rvFavorites);
        suggestionList = view.findViewById(R.id.rvFavSuggestions);
        suggestionHeader = view.findViewById(R.id.headerFavSuggestions);
        count = view.findViewById(R.id.tvFavCount);
        filterGroup = view.findViewById(R.id.chipGroupFavFilter);
        alertsSwitch = view.findViewById(R.id.switchFavAlerts);
        skeleton = view.findViewById(R.id.skeletonFavorites);
        emptyState = view.findViewById(R.id.emptyFavorites);
        errorState = view.findViewById(R.id.errorFavorites);
    }

    private void setupToolbar() {
        ((SectionHeaderView) suggestionHeader).setTitle(R.string.hc_fav_suggestions_title);
        ((SectionHeaderView) suggestionHeader).setActionText(R.string.hc_home_see_all);
        ((SectionHeaderView) suggestionHeader).setOnActionClickListener(v ->
                openEventList());
    }

    private void setupLists() {
        adapter = new FavoriteAdapter();
        adapter.setOnFavoriteActionListener(new FavoriteAdapter.OnFavoriteActionListener() {
            @Override
            public void onFavoriteOpen(FavoriteUiModel item) {
                openEventDetail(item.event);
            }

            @Override
            public void onFavoriteRemove(FavoriteUiModel item) {
                removeSaved(item);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(adapter);

        suggestionAdapter = new EventAdapter(EventAdapter.Mode.FEATURED);
        suggestionAdapter.setOnEventActionListener(new EventAdapter.OnEventActionListener() {
            @Override
            public void onEventClick(EventUiModel event) {
                openEventDetail(event);
            }

            @Override
            public void onFavoriteClick(EventUiModel event, int position) {
                if (event.favorite) {
                    viewModel.addToFavorites(event);
                } else {
                    viewModel.removeEvent(event.id);
                }
                Snackbar.make(requireView(),
                        event.favorite ? R.string.hc_event_saved : R.string.hc_event_unsaved,
                        Snackbar.LENGTH_SHORT).show();
            }
        });
        suggestionList.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        suggestionList.setAdapter(suggestionAdapter);
    }

    private void setupFilters() {
        filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingFilter || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipFavUpcoming) {
                viewModel.setFilter(FavoritesViewModel.Filter.UPCOMING);
            } else if (id == R.id.chipFavPast) {
                viewModel.setFilter(FavoritesViewModel.Filter.PAST);
            } else {
                viewModel.setFilter(FavoritesViewModel.Filter.ALL);
            }
        });

        alertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingSwitch) {
                return;
            }
            viewModel.setAlertsEnabled(isChecked);
            Snackbar.make(requireView(),
                    isChecked ? R.string.hc_fav_alerts_on : R.string.hc_fav_alerts_off,
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    private void setupStates() {
        swipeRefresh.setColorSchemeResources(R.color.ds_primary);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());

        emptyState.setIcon(R.drawable.ic_ds_heart);
        emptyState.setTitle(R.string.hc_fav_empty_title);
        emptyState.setMessage(R.string.hc_fav_empty_message);
        emptyState.setActionText(R.string.hc_fav_empty_action);
        emptyState.setOnActionClickListener(v -> openEventList());

        errorState.setTitle(R.string.hc_fav_error_title);
        errorState.setMessage(R.string.hc_error_default);
        errorState.setOnRetryClickListener(v -> viewModel.refresh());
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

        viewModel.getAlertsEnabled().observe(getViewLifecycleOwner(), enabled -> {
            bindingSwitch = true;
            alertsSwitch.setChecked(Boolean.TRUE.equals(enabled));
            bindingSwitch = false;
        });

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            count.setText(getString(R.string.hc_fav_count, items == null ? 0 : items.size()));
            renderContent();
        });

        viewModel.getSuggestions().observe(getViewLifecycleOwner(), events -> {
            suggestionAdapter.submitList(events);
            renderContent();
        });
    }

    private void renderContent() {
        if (firstLoad) {
            return;
        }
        boolean loading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        boolean failed = Boolean.TRUE.equals(viewModel.getError().getValue());
        boolean empty = adapter.getItemCount() == 0;
        View scroll = requireView().findViewById(R.id.scrollFavorites);

        skeleton.setVisibility(View.GONE);
        errorState.setVisibility(failed ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(!failed && !loading && empty ? View.VISIBLE : View.GONE);
        scroll.setVisibility(failed || empty ? View.GONE : View.VISIBLE);

        boolean showSuggestions = !empty && suggestionAdapter.getItemCount() > 0;
        suggestionHeader.setVisibility(showSuggestions ? View.VISIBLE : View.GONE);
        suggestionList.setVisibility(showSuggestions ? View.VISIBLE : View.GONE);
    }

    private void removeSaved(FavoriteUiModel item) {
        viewModel.remove(item);
        Snackbar.make(requireView(), R.string.hc_fav_removed, Snackbar.LENGTH_LONG)
                .setAction(R.string.hc_fav_undo, v -> viewModel.restore(item))
                .show();
    }

    private void openEventDetail(@Nullable EventUiModel event) {
        if (event == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong(EventListFragment.ARG_EVENT_ID, event.id);
        args.putString(EventListFragment.ARG_EVENT_TITLE, event.title);
        Navigation.findNavController(requireView())
                .navigate(R.id.event_detailFragment, args);
    }

    private void openEventList() {
        Navigation.findNavController(requireView()).navigate(R.id.event_listFragment);
    }
}
