package com.eventapp.app.ui.user.home;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eventapp.app.R;
import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.ui.common.adapter.EventAdapter;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;

/** FR-U04 discovery home: quick filters, AI suggestions and curated event sections. */
public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private SwipeRefreshLayout swipeRefresh;
    private NestedScrollView scrollView;
    private SkeletonLayout skeleton;
    private EmptyStateView emptyState;
    private ErrorStateView errorState;
    private TextView aiCount;
    private ChipGroup quickGroup;
    private ChipGroup categoryGroup;
    private Chip quickAll;
    private Chip quickNear;
    private EventAdapter aiAdapter;
    private EventAdapter featuredAdapter;
    private EventAdapter upcomingAdapter;

    private boolean bindingQuick;
    private boolean bindingCategory;
    private boolean firstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        swipeRefresh = view.findViewById(R.id.swipeRefreshHome);
        scrollView = view.findViewById(R.id.scrollHome);
        skeleton = view.findViewById(R.id.skeletonHome);
        emptyState = view.findViewById(R.id.emptyHome);
        errorState = view.findViewById(R.id.errorHome);
        aiCount = view.findViewById(R.id.tvHomeAiCount);
        quickGroup = view.findViewById(R.id.chipGroupHomeQuick);
        categoryGroup = view.findViewById(R.id.chipGroupHomeCategories);
        quickAll = view.findViewById(R.id.chipHomeQuickAll);
        quickNear = view.findViewById(R.id.chipHomeQuickNear);

        setupHeader(view);
        setupSearch(view);
        setupQuickFilters();
        setupCategories();
        setupSectionHeaders();
        setupLists(view);
        setupStates();
        observeData();
        registerFilterResult();
    }

    private void setupHeader(View view) {
        view.findViewById(R.id.btnHomeFavorites).setOnClickListener(v ->
                navigate(R.id.favoritesFragment));
        view.findViewById(R.id.btnHomeNotifications).setOnClickListener(v ->
                navigate(R.id.notification_listFragment));
        view.findViewById(R.id.btnHomeProfile).setOnClickListener(v ->
                navigate(R.id.profileFragment));
    }

    private void setupSearch(View view) {
        view.findViewById(R.id.layoutHomeSearch).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean(EventListFragment.ARG_OPEN_SEARCH, true);
            openEventList(args);
        });
        view.findViewById(R.id.btnHomeFilter).setOnClickListener(v -> openFilter());
    }

    private void setupQuickFilters() {
        quickNear.setEnabled(false);
        quickNear.setAlpha(0.5f);
        quickNear.setText(R.string.hc_home_quick_near_soon);
        quickNear.setContentDescription(getString(R.string.hc_coming_soon));

        quickGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingQuick) {
                return;
            }
            if (checkedIds.isEmpty()) {
                resetQuickSelection();
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipHomeQuickAll) {
                openEventList(null);
                return;
            }
            EventFilterCriteria criteria = new EventFilterCriteria();
            if (id == R.id.chipHomeQuickToday) {
                criteria.timePreset = EventFilterCriteria.TimePreset.TODAY;
            } else if (id == R.id.chipHomeQuickWeekend) {
                applyWeekendRange(criteria);
            } else if (id == R.id.chipHomeQuickFree) {
                criteria.priceType = EventFilterCriteria.PriceType.FREE;
            } else {
                resetQuickSelection();
                return;
            }
            Bundle args = new Bundle();
            args.putSerializable(EventListFragment.ARG_CRITERIA, criteria);
            openEventList(args);
            resetQuickSelection();
        });
    }

    private void setupCategories() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (EventCategory category : EventCategory.values()) {
            Chip chip = (Chip) inflater.inflate(R.layout.view_ds_filter_chip, categoryGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(EventCategoryUi.label(requireContext(), category));
            chip.setTag(category);
            chip.setChipIconResource(EventCategoryUi.iconRes(category));
            chip.setChipIconVisible(true);
            chip.setChipIconSize(dp(16));
            ColorStateList iconTint = AppCompatResources.getColorStateList(requireContext(),
                    R.color.ds_chip_text);
            chip.setChipIconTint(iconTint);
            categoryGroup.addView(chip);
        }
        categoryGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingCategory || checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip == null || !(chip.getTag() instanceof EventCategory)) {
                return;
            }
            Bundle args = new Bundle();
            args.putLong(EventListFragment.ARG_CATEGORY_ID,
                    EventCategoryUi.id((EventCategory) chip.getTag()));
            openEventList(args);
            bindingCategory = true;
            group.clearCheck();
            bindingCategory = false;
        });
    }

    private void setupSectionHeaders() {
        SectionHeaderView categories = requireView().findViewById(R.id.headerHomeCategories);
        categories.setTitle(R.string.hc_home_categories_title);

        SectionHeaderView featured = requireView().findViewById(R.id.headerHomeFeatured);
        featured.setTitle(R.string.hc_home_featured_title);
        featured.setActionText(R.string.hc_home_see_all);
        featured.setOnActionClickListener(v -> {
            EventFilterCriteria criteria = new EventFilterCriteria();
            criteria.sort = EventFilterCriteria.Sort.POPULAR;
            Bundle args = new Bundle();
            args.putSerializable(EventListFragment.ARG_CRITERIA, criteria);
            openEventList(args);
        });

        SectionHeaderView upcoming = requireView().findViewById(R.id.headerHomeUpcoming);
        upcoming.setTitle(R.string.hc_home_upcoming_title);
        upcoming.setSubtitle(R.string.hc_home_upcoming_subtitle);
        upcoming.setActionText(R.string.hc_home_see_all);
        upcoming.setOnActionClickListener(v -> openEventList(null));
    }

    private void setupLists(View view) {
        RecyclerView aiList = view.findViewById(R.id.rvHomeAi);
        aiList.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        aiList.setHasFixedSize(true);
        aiAdapter = new EventAdapter(EventAdapter.Mode.AI);
        aiAdapter.setOnEventActionListener(eventActions);
        aiList.setAdapter(aiAdapter);

        RecyclerView featuredList = view.findViewById(R.id.rvHomeFeatured);
        featuredList.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        featuredList.setHasFixedSize(true);
        featuredAdapter = new EventAdapter(EventAdapter.Mode.FEATURED);
        featuredAdapter.setOnEventActionListener(eventActions);
        featuredList.setAdapter(featuredAdapter);

        RecyclerView upcomingList = view.findViewById(R.id.rvHomeUpcoming);
        upcomingList.setLayoutManager(new LinearLayoutManager(requireContext()));
        upcomingList.setNestedScrollingEnabled(false);
        upcomingList.setItemAnimator(null);
        upcomingAdapter = new EventAdapter(EventAdapter.Mode.LIST);
        upcomingAdapter.setOnEventActionListener(eventActions);
        upcomingList.setAdapter(upcomingAdapter);

        view.findViewById(R.id.btnHomeAiSeeAll).setOnClickListener(v ->
                navigate(R.id.recommendFragment));
    }

    private final EventAdapter.OnEventActionListener eventActions =
            new EventAdapter.OnEventActionListener() {
                @Override
                public void onEventClick(EventUiModel event) {
                    Bundle args = new Bundle();
                    args.putLong(EventListFragment.ARG_EVENT_ID, event.id);
                    args.putString(EventListFragment.ARG_EVENT_TITLE, event.title);
                    Navigation.findNavController(requireView())
                            .navigate(R.id.event_detailFragment, args);
                }

                @Override
                public void onFavoriteClick(EventUiModel event, int position) {
                    Snackbar.make(requireView(),
                            event.favorite ? R.string.hc_event_saved : R.string.hc_event_unsaved,
                            Snackbar.LENGTH_SHORT).show();
                }
            };

    private void setupStates() {
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        swipeRefresh.setColorSchemeResources(R.color.ds_primary);

        emptyState.setTitle(R.string.hc_home_empty_title);
        emptyState.setMessage(R.string.hc_home_empty_message);
        emptyState.setActionText(R.string.hc_refresh);
        emptyState.setOnActionClickListener(v -> viewModel.refresh());

        errorState.setTitle(R.string.hc_error_title);
        errorState.setMessage(R.string.hc_error_default);
        errorState.setOnRetryClickListener(v -> viewModel.refresh());
    }

    private void observeData() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            swipeRefresh.setRefreshing(false);
            if (isLoading && firstLoad) {
                skeleton.setVisibility(View.VISIBLE);
                scrollView.setVisibility(View.INVISIBLE);
                emptyState.setVisibility(View.GONE);
            } else {
                skeleton.setVisibility(View.GONE);
                if (!isLoading) {
                    firstLoad = false;
                }
                updateEmptyState();
            }
        });

        viewModel.getAiEvents().observe(getViewLifecycleOwner(), events -> {
            aiAdapter.submitList(events);
            int count = events == null ? 0 : events.size();
            aiCount.setText(getString(R.string.hc_home_ai_count, count));
            updateEmptyState();
        });

        viewModel.getFeaturedEvents().observe(getViewLifecycleOwner(), events -> {
            featuredAdapter.submitList(events);
            updateEmptyState();
        });

        viewModel.getUpcomingEvents().observe(getViewLifecycleOwner(), events -> {
            upcomingAdapter.submitList(events);
            updateEmptyState();
        });
    }

    private void updateEmptyState() {
        if (firstLoad || Boolean.TRUE.equals(viewModel.getLoading().getValue())) {
            return;
        }
        boolean empty = aiAdapter.getItemCount() == 0
                && featuredAdapter.getItemCount() == 0
                && upcomingAdapter.getItemCount() == 0;
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        scrollView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void registerFilterResult() {
        getParentFragmentManager().setFragmentResultListener(
                EventFilterFragment.REQUEST_KEY, getViewLifecycleOwner(), (key, bundle) -> {
                    Object value = bundle.getSerializable(EventFilterFragment.RESULT_CRITERIA);
                    if (value instanceof EventFilterCriteria) {
                        Bundle args = new Bundle();
                        args.putSerializable(EventListFragment.ARG_CRITERIA,
                                (EventFilterCriteria) value);
                        openEventList(args);
                    }
                });
    }

    private void openFilter() {
        Bundle args = new Bundle();
        args.putSerializable(EventFilterFragment.ARG_CRITERIA, new EventFilterCriteria());
        Navigation.findNavController(requireView())
                .navigate(R.id.event_filterFragment, args);
    }

    private void openEventList(@Nullable Bundle args) {
        NavController navController = Navigation.findNavController(requireView());
        if (args == null) {
            navController.navigate(R.id.event_listFragment);
        } else {
            navController.navigate(R.id.event_listFragment, args);
        }
    }

    private void navigate(int destinationId) {
        Navigation.findNavController(requireView()).navigate(destinationId);
    }

    private void resetQuickSelection() {
        bindingQuick = true;
        quickAll.setChecked(true);
        bindingQuick = false;
    }

    private void applyWeekendRange(EventFilterCriteria criteria) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar start = (Calendar) today.clone();
        int day = start.get(Calendar.DAY_OF_WEEK);
        if (day != Calendar.SATURDAY && day != Calendar.SUNDAY) {
            int daysUntilSaturday = (Calendar.SATURDAY - day + 7) % 7;
            start.add(Calendar.DAY_OF_YEAR, daysUntilSaturday);
        }
        Calendar end = (Calendar) start.clone();
        // Saturday + Sunday, or the remaining Sunday when today is Sunday.
        end.add(Calendar.DAY_OF_YEAR, day == Calendar.SUNDAY ? 0 : 1);
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        criteria.timePreset = EventFilterCriteria.TimePreset.CUSTOM;
        criteria.dateFrom = start.getTimeInMillis();
        criteria.dateTo = end.getTimeInMillis();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
