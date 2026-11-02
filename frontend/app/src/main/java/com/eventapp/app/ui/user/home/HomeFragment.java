package com.eventapp.app.ui.user.home;

import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eventapp.app.R;
import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.data.model.EventFilterCriteria;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.eventapp.app.ui.mapper.EventMapper;
import com.eventapp.app.ui.common.adapter.EventAdapter;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.NavBarSpacing;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.eventapp.app.ui.model.EventCategoryUi;
import com.eventapp.app.ui.model.EventUiModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/** FR-U04 discovery home: quick filters, AI suggestions and curated event sections. */
public class HomeFragment extends Fragment {

    /** Trang Khám phá chỉ hiển thị vài gợi ý AI; xem đầy đủ ở màn "Dành cho bạn". */
    private static final int AI_SECTION_LIMIT = 3;
    private static final int SEARCH_HEIGHT_EXPANDED_DP = 48;
    private static final int SEARCH_HEIGHT_COMPACT_DP = 40;
    private static final long COLLAPSE_DURATION_MS = 180L;

    private HomeViewModel viewModel;
    private SwipeRefreshLayout swipeRefresh;
    private NestedScrollView scrollView;
    private SkeletonLayout skeleton;
    private EmptyStateView emptyState;
    private ErrorStateView errorState;
    private TextView aiCount;
    private TextView aiHeader;
    private TextView aiSubheader;
    private TextView filterCount;
    private View header;
    private View searchBox;
    private View filterBox;
    private ChipGroup quickGroup;
    private Chip quickAll;
    private Chip quickNear;
    private LinearLayout categoryRow;
    private EventAdapter aiAdapter;
    private EventAdapter heroAdapter;
    private EventAdapter trendingAdapter;
    private EventAdapter upcomingAdapter;

    private int headerHeight;
    private boolean headerHidden;
    private boolean searchCompact;
    private Long selectedCategoryId;

    private boolean bindingQuick;
    private boolean firstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_home, container, false);
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
        aiHeader = view.findViewById(R.id.tvHomeAiHeader);
        aiSubheader = view.findViewById(R.id.tvHomeAiSubheader);
        filterCount = view.findViewById(R.id.tvHomeFilterCount);
        header = view.findViewById(R.id.layoutHomeHeader);
        searchBox = view.findViewById(R.id.layoutHomeSearch);
        filterBox = view.findViewById(R.id.layoutHomeFilter);
        quickGroup = view.findViewById(R.id.chipGroupHomeQuick);
        quickAll = view.findViewById(R.id.chipHomeQuickAll);
        quickNear = view.findViewById(R.id.chipHomeQuickNear);
        categoryRow = view.findViewById(R.id.layoutHomeCategories);

        setupHeader(view);
        setupSearch(view);
        setupQuickFilters();
        setupCategories();
        setupSectionHeaders();
        setupLists(view);
        setupStates();
        observeData();
        registerFilterResult();
        setupScrollBehavior();
        applyNavBarClearance();
    }

    private void setupHeader(View view) {
        // Thông báo chưa có API → ẩn; tài khoản/đăng xuất nằm ở tab "Tài khoản".
        view.findViewById(R.id.btnHomeNotifications).setVisibility(View.GONE);
        view.findViewById(R.id.btnHomeLocation).setOnClickListener(v -> showComingSoon());
        view.findViewById(R.id.btnHomeSearchVoice).setOnClickListener(v -> showComingSoon());
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
            updateFilterBadge();
            if (bindingQuick) {
                return;
            }
            if (checkedIds.isEmpty()) {
                bindingQuick = true;
                quickAll.setChecked(true);
                bindingQuick = false;
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
                return;
            }
            Bundle args = new Bundle();
            args.putSerializable(EventListFragment.ARG_CRITERIA, criteria);
            openEventList(args);
        });
    }

    /** Danh mục lấy từ backend (id 9–24) thay vì danh sách hardcode cũ. */
    private void setupCategories() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), this::renderCategories);
    }

    private void renderCategories(@Nullable List<CatalogDtos.CategoryNode> nodes) {
        categoryRow.removeAllViews();
        if (nodes == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (CatalogDtos.CategoryNode node : nodes) {
            EventCategory category = EventMapper.categoryOf(
                    Collections.singletonList(node.name), null);
            View item = inflater.inflate(R.layout.item_home_category, categoryRow, false);
            ImageView icon = item.findViewById(R.id.ivCategoryIcon);
            TextView label = item.findViewById(R.id.tvCategoryLabel);
            View circle = item.findViewById(R.id.layoutCategoryCircle);

            icon.setImageResource(EventCategoryUi.iconRes(category));
            label.setText(node.name);
            item.setContentDescription(node.name);
            item.setTag(node.id);
            boolean selected = selectedCategoryId != null && selectedCategoryId == node.id;
            circle.setBackground(categoryCircle(category, selected));

            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) item.getLayoutParams();
            params.setMarginEnd(dp(8));
            item.setLayoutParams(params);

            item.setOnClickListener(v -> onCategoryClick(node.id));
            categoryRow.addView(item);
        }
    }

    private void onCategoryClick(long categoryId) {
        selectedCategoryId = selectedCategoryId != null && selectedCategoryId == categoryId
                ? null : categoryId;
        refreshCategorySelection();
        updateFilterBadge();
        if (selectedCategoryId == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong(EventListFragment.ARG_CATEGORY_ID, selectedCategoryId);
        openEventList(args);
    }

    private void refreshCategorySelection() {
        for (int i = 0; i < categoryRow.getChildCount(); i++) {
            View child = categoryRow.getChildAt(i);
            Object tag = child.getTag();
            if (!(tag instanceof Long)) {
                continue;
            }
            View circle = child.findViewById(R.id.layoutCategoryCircle);
            boolean selected = selectedCategoryId != null && selectedCategoryId.equals(tag);
            EventCategory category = EventMapper.categoryOf(
                    Collections.singletonList(((TextView) child.findViewById(R.id.tvCategoryLabel))
                            .getText().toString()), null);
            circle.setBackground(categoryCircle(category, selected));
            child.setSelected(selected);
        }
    }

    private GradientDrawable categoryCircle(EventCategory category, boolean selected) {
        GradientDrawable circle = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{EventCategoryUi.gradientStart(requireContext(), category),
                        EventCategoryUi.gradientEnd(requireContext(), category)});
        circle.setShape(GradientDrawable.OVAL);
        if (selected) {
            circle.setStroke(dp(3), ContextCompat.getColor(requireContext(), R.color.ds_text));
        }
        return circle;
    }

    private void setupSectionHeaders() {
        SectionHeaderView upcoming = requireView().findViewById(R.id.headerHomeUpcoming);
        upcoming.setTitle(R.string.hc_home_upcoming_title);
        upcoming.setSubtitle(R.string.hc_home_upcoming_subtitle);
        upcoming.setActionText(R.string.hc_home_see_all);
        upcoming.setOnActionClickListener(v -> openEventList(null));

        SectionHeaderView trending = requireView().findViewById(R.id.headerHomeTrending);
        trending.setTitle(R.string.hc_home_trending_title);
        trending.setActionText(R.string.hc_home_see_all);
        trending.setOnActionClickListener(v -> {
            EventFilterCriteria criteria = new EventFilterCriteria();
            criteria.sort = EventFilterCriteria.Sort.POPULAR;
            Bundle args = new Bundle();
            args.putSerializable(EventListFragment.ARG_CRITERIA, criteria);
            openEventList(args);
        });
    }

    private void setupLists(View view) {
        RecyclerView aiList = view.findViewById(R.id.rvHomeAi);
        aiList.setLayoutManager(new LinearLayoutManager(requireContext()));
        aiList.setNestedScrollingEnabled(false);
        aiAdapter = new EventAdapter(EventAdapter.Mode.AI);
        aiAdapter.setOnEventActionListener(eventActions);
        aiList.setAdapter(aiAdapter);

        RecyclerView heroList = view.findViewById(R.id.rvHomeHero);
        heroList.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        new PagerSnapHelper().attachToRecyclerView(heroList);
        heroAdapter = new EventAdapter(EventAdapter.Mode.HERO);
        heroAdapter.setOnEventActionListener(eventActions);
        heroList.setAdapter(heroAdapter);

        RecyclerView trendingList = view.findViewById(R.id.rvHomeTrending);
        trendingList.setLayoutManager(new LinearLayoutManager(requireContext()));
        trendingList.setNestedScrollingEnabled(false);
        trendingList.setItemAnimator(null);
        trendingAdapter = new EventAdapter(EventAdapter.Mode.TRENDING);
        trendingAdapter.setOnEventActionListener(eventActions);
        trendingList.setAdapter(trendingAdapter);

        RecyclerView upcomingList = view.findViewById(R.id.rvHomeUpcoming);
        upcomingList.setLayoutManager(new LinearLayoutManager(requireContext()));
        upcomingList.setNestedScrollingEnabled(false);
        upcomingList.setItemAnimator(null);
        upcomingAdapter = new EventAdapter(EventAdapter.Mode.LIST);
        upcomingAdapter.setOnEventActionListener(eventActions);
        upcomingList.setAdapter(upcomingAdapter);

        view.findViewById(R.id.btnHomeAiSeeAll).setOnClickListener(v ->
                navigate(R.id.recommendFragment));
        view.findViewById(R.id.btnHomeAiRefresh).setOnClickListener(v -> viewModel.refresh());
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
                    showMessage(event.favorite
                            ? R.string.hc_event_saved
                            : R.string.hc_event_unsaved);
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
            List<EventUiModel> visible = events == null
                    ? Collections.emptyList()
                    : events.subList(0, Math.min(AI_SECTION_LIMIT, events.size()));
            aiAdapter.submitList(visible);
            int count = events == null ? 0 : events.size();
            aiCount.setText(getString(R.string.hc_home_ai_count, count));
            bindAiHeading(events);
            updateEmptyState();
        });

        viewModel.getFeaturedEvents().observe(getViewLifecycleOwner(), events -> {
            heroAdapter.submitList(events);
            updateEmptyState();
        });

        viewModel.getTrendingEvents().observe(getViewLifecycleOwner(), events -> {
            trendingAdapter.submitList(events);
            updateEmptyState();
        });

        viewModel.getUpcomingEvents().observe(getViewLifecycleOwner(), events -> {
            upcomingAdapter.submitList(events);
            updateEmptyState();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), exception ->
                errorState.setVisibility(exception == null ? View.GONE : View.VISIBLE));
    }

    /**
     * FR-U07: chỉ gọi là "cá nhân hoá" khi có lý do gợi ý thật; ngược lại dùng tiêu đề
     * "Phổ biến sắp diễn ra" để không ảo định là đã cá nhân hoá.
     */
    private void bindAiHeading(@Nullable List<EventUiModel> events) {
        boolean personalized = false;
        if (events != null) {
            for (EventUiModel event : events) {
                if (event.aiReason != null && !event.aiReason.trim().isEmpty()) {
                    personalized = true;
                    break;
                }
            }
        }
        aiHeader.setText(personalized
                ? R.string.hc_home_ai_header
                : R.string.hc_home_ai_header_popular);
        aiSubheader.setText(personalized
                ? R.string.hc_home_ai_subheader
                : R.string.hc_home_ai_subheader_popular);
    }

    private void updateEmptyState() {
        if (firstLoad || Boolean.TRUE.equals(viewModel.getLoading().getValue())) {
            return;
        }
        boolean empty = aiAdapter.getItemCount() == 0
                && heroAdapter.getItemCount() == 0
                && trendingAdapter.getItemCount() == 0
                && upcomingAdapter.getItemCount() == 0;
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        scrollView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /** Header thu gọn khi cuộn xuống, bung ra khi cuộn lên; search bar thu nhỏ khi đã cuộn. */
    private void setupScrollBehavior() {
        header.post(() -> headerHeight = header.getHeight());
        scrollView.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldX, oldY) -> {
                    if (headerHeight == 0) {
                        headerHeight = header.getHeight();
                    }
                    int dy = scrollY - oldY;
                    if (scrollY <= 0) {
                        expandHeader();
                    } else if (dy > dp(4)) {
                        collapseHeader();
                    } else if (dy < -dp(4)) {
                        expandHeader();
                    }
                    setSearchCompact(scrollY > dp(12));
                });
    }

    private void collapseHeader() {
        if (headerHidden || headerHeight == 0) {
            return;
        }
        headerHidden = true;
        animateHeight(header, headerHeight, 0);
        header.animate().alpha(0f).setDuration(COLLAPSE_DURATION_MS).start();
    }

    private void expandHeader() {
        if (!headerHidden) {
            return;
        }
        headerHidden = false;
        animateHeight(header, 0, headerHeight);
        header.animate().alpha(1f).setDuration(COLLAPSE_DURATION_MS).start();
    }

    private void setSearchCompact(boolean compact) {
        if (searchCompact == compact) {
            return;
        }
        searchCompact = compact;
        int target = dp(compact ? SEARCH_HEIGHT_COMPACT_DP : SEARCH_HEIGHT_EXPANDED_DP);
        animateHeight(searchBox, searchBox.getHeight(), target);
        animateHeight(filterBox, filterBox.getHeight(), target);
    }

    private void animateHeight(View view, int from, int to) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(COLLAPSE_DURATION_MS);
        animator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = (int) animation.getAnimatedValue();
            view.setLayoutParams(params);
        });
        animator.start();
    }

    /** Badge trên nút lọc = số bộ lọc nhanh đang áp dụng. */
    private void updateFilterBadge() {
        int count = 0;
        if (quickGroup.getCheckedChipId() != View.NO_ID
                && quickGroup.getCheckedChipId() != R.id.chipHomeQuickAll) {
            count++;
        }
        if (selectedCategoryId != null) {
            count++;
        }
        if (count == 0) {
            filterCount.setVisibility(View.GONE);
        } else {
            filterCount.setText(String.valueOf(count));
            filterCount.setVisibility(View.VISIBLE);
        }
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

    private void applyNavBarClearance() {
        NavBarSpacing.padBottomForNavBar(requireView(), scrollView, 16);
    }

    private void showComingSoon() {
        showMessage(R.string.hc_coming_soon);
    }

    /** Snackbar neo lên thanh điều hướng nổi để không che mất các mục chính. */
    private void showMessage(int messageRes) {
        Snackbar snackbar = Snackbar.make(requireView(), messageRes, Snackbar.LENGTH_SHORT);
        View navBar = requireActivity().findViewById(R.id.nav_bar_container);
        if (navBar != null) {
            snackbar.setAnchorView(navBar);
        }
        snackbar.show();
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
