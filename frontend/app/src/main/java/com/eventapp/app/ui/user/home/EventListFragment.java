package com.eventapp.app.ui.user.home;


import com.eventapp.app.data.local.prefs.SearchHistoryStore;
import com.eventapp.app.data.model.EventFilterCriteria;
import com.eventapp.app.ui.model.EventUiModel;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.adapter.EventAdapter;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Locale;

/**
 * FR-U04 event list: search (400 ms debounce, min 2 chars), filters, sort and pagination.
 * The whole screen is backed by {@link EventListViewModel}; this fragment only renders state.
 */
public class EventListFragment extends Fragment {

    public static final String ARG_QUERY = "arg_query";
    public static final String ARG_CATEGORY_ID = "arg_category_id";
    public static final String ARG_CRITERIA = "arg_criteria";
    public static final String ARG_OPEN_SEARCH = "arg_open_search";
    public static final String ARG_SORT = "arg_sort";

    /** Shared with EventDetailFragment (U05). Keep the keys stable for deep links. */
    public static final String ARG_EVENT_ID = "arg_event_id";
    public static final String ARG_EVENT_TITLE = "arg_event_title";

    private static final long SEARCH_DEBOUNCE_MS = 400L;
    private static final int MIN_QUERY_LENGTH = 2;

    private EventListViewModel viewModel;
    private EventAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private TextInputLayout searchLayout;
    private TextInputEditText searchInput;
    private ChipGroup historyGroup;
    private View historyPanel;
    private TextView historyEmpty;
    private TextView filterBadge;
    private TextView clearFilters;
    private TextView resultCount;
    private TextView sortLabel;
    private SkeletonLayout skeleton;
    private EmptyStateView emptyState;
    private ErrorStateView errorState;

    private SearchHistoryStore historyStore;
    private EventFilterCriteria criteria = new EventFilterCriteria();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private String committedQuery = "";
    private boolean firstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_home_event_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(EventListViewModel.class);
        historyStore = new SearchHistoryStore(requireContext());

        bindViews(view);
        readArguments();
        setupToolbar();
        setupLists();
        setupSearch();
        setupSort();
        setupStates();
        observeData();
        registerFilterResult();
        bindCriteriaViews();
        updateSortLabel();

        viewModel.applyFilters(criteria, committedQuery);
        if (getArguments() != null && getArguments().getBoolean(ARG_OPEN_SEARCH, false)) {
            searchInput.requestFocus();
            showKeyboard();
        }
    }

    private void bindViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefreshList);
        recyclerView = view.findViewById(R.id.rvEventList);
        searchLayout = view.findViewById(R.id.tilListSearch);
        searchInput = view.findViewById(R.id.etListSearch);
        historyGroup = view.findViewById(R.id.chipGroupListHistory);
        historyPanel = view.findViewById(R.id.layoutListHistory);
        historyEmpty = view.findViewById(R.id.tvListHistoryEmpty);
        filterBadge = view.findViewById(R.id.tvListFilterBadge);
        clearFilters = view.findViewById(R.id.btnListClearFilters);
        resultCount = view.findViewById(R.id.tvListResultCount);
        sortLabel = view.findViewById(R.id.tvListSort);
        skeleton = view.findViewById(R.id.skeletonList);
        emptyState = view.findViewById(R.id.emptyList);
        errorState = view.findViewById(R.id.errorList);
    }

    private void readArguments() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        if (args.containsKey(ARG_CRITERIA)) {
            Object value = args.getSerializable(ARG_CRITERIA);
            if (value instanceof EventFilterCriteria) {
                criteria = ((EventFilterCriteria) value).copy();
            }
        }
        long categoryId = args.getLong(ARG_CATEGORY_ID, 0L);
        if (categoryId > 0L) {
            criteria.categoryIds.add(categoryId);
        }
        committedQuery = normalizeQuery(args.getString(ARG_QUERY, ""));
        if (committedQuery.length() < MIN_QUERY_LENGTH) {
            committedQuery = "";
        }
        String sortName = args.getString(ARG_SORT);
        if (sortName != null) {
            try {
                criteria.sort = EventFilterCriteria.Sort.valueOf(sortName);
            } catch (IllegalArgumentException ignored) {
                // Unknown sort from a stale deep link; keep the default.
            }
        }
    }

    private void setupToolbar() {
        requireView().findViewById(R.id.btnListBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
        requireView().findViewById(R.id.btnListFilter).setOnClickListener(v -> openFilter());
        clearFilters.setOnClickListener(v -> {
            criteria = new EventFilterCriteria();
            committedQuery = "";
            searchInput.setText("");
            bindCriteriaViews();
            updateSortLabel();
            viewModel.applyFilters(criteria, "");
        });
    }

    private void setupLists() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);
        adapter = new EventAdapter(EventAdapter.Mode.LIST);
        adapter.setOnEventActionListener(new EventAdapter.OnEventActionListener() {
            @Override
            public void onEventClick(EventUiModel event) {
                Bundle args = new Bundle();
                args.putLong(ARG_EVENT_ID, event.id);
                args.putString(ARG_EVENT_TITLE, event.title);
                Navigation.findNavController(requireView())
                        .navigate(R.id.event_detailFragment, args);
            }

            @Override
            public void onFavoriteClick(EventUiModel event, int position) {
                Snackbar.make(requireView(),
                        event.favorite ? R.string.hc_event_saved : R.string.hc_event_unsaved,
                        Snackbar.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) {
                    return;
                }
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                if (lastVisible >= adapter.getItemCount() - 3) {
                    viewModel.loadNextPage();
                }
            }
        });
    }

    private void setupSearch() {
        searchInput.setText(committedQuery);
        if (!committedQuery.isEmpty()) {
            searchInput.setSelection(committedQuery.length());
        }
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onQueryChanged(s == null ? "" : s.toString());
            }
        });
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && textOf(searchInput).trim().isEmpty()) {
                showHistory();
            }
        });
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean searchAction = actionId == EditorInfo.IME_ACTION_SEARCH;
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (searchAction || enter) {
                submitSearchNow(textOf(searchInput));
                return true;
            }
            return false;
        });
        requireView().findViewById(R.id.btnListHistoryClear).setOnClickListener(v -> {
            historyStore.clear();
            renderHistory();
        });
    }

    private void onQueryChanged(String raw) {
        String value = raw == null ? "" : raw;
        String trimmed = value.trim();
        handler.removeCallbacksAndMessages(null);
        if (trimmed.isEmpty()) {
            searchLayout.setHelperText(null);
            committedQuery = "";
            bindCriteriaViews();
            showHistory();
            viewModel.applyFilters(criteria, "");
            return;
        }
        hideHistory();
        if (trimmed.length() < MIN_QUERY_LENGTH) {
            searchLayout.setHelperText(getString(R.string.hc_list_search_min));
            return;
        }
        searchLayout.setHelperText(null);
        handler.postDelayed(() -> applyQuery(trimmed, false), SEARCH_DEBOUNCE_MS);
    }

    private void submitSearchNow(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        handler.removeCallbacksAndMessages(null);
        hideKeyboard();
        searchInput.clearFocus();
        hideHistory();
        applyQuery(trimmed, true);
    }

    private void applyQuery(String query, boolean remember) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() == 1) {
            searchLayout.setHelperText(getString(R.string.hc_list_search_min));
            return;
        }
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        committedQuery = trimmed;
        bindCriteriaViews();
        if (remember && trimmed.length() >= MIN_QUERY_LENGTH) {
            historyStore.add(trimmed);
            renderHistory();
        }
        viewModel.applyFilters(criteria, trimmed);
    }

    private void showHistory() {
        renderHistory();
        historyPanel.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        skeleton.setVisibility(View.GONE);
    }

    private void hideHistory() {
        historyPanel.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.VISIBLE);
    }

    private void renderHistory() {
        historyGroup.removeAllViews();
        List<String> items = historyStore.getQueries();
        historyEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        historyGroup.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String item : items) {
            Chip chip = (Chip) inflater.inflate(R.layout.view_ds_filter_chip, historyGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(item);
            chip.setChipIconResource(R.drawable.ic_ds_clock);
            chip.setChipIconVisible(true);
            chip.setChipIconSize(dp(16));
            chip.setChipIconTint(ContextCompat.getColorStateList(requireContext(),
                    R.color.ds_chip_text));
            chip.setOnClickListener(v -> {
                searchInput.setText(item);
                searchInput.setSelection(item.length());
                handler.removeCallbacksAndMessages(null);
                hideHistory();
                applyQuery(item, true);
                hideKeyboard();
            });
            historyGroup.addView(chip);
        }
    }

    private void setupSort() {
        requireView().findViewById(R.id.btnListSort).setOnClickListener(anchor -> {
            PopupMenu popup = new PopupMenu(requireContext(), anchor);
            popup.getMenu().add(0, 1, 0, R.string.hc_filter_sort_start);
            popup.getMenu().add(0, 2, 1, R.string.hc_filter_sort_newest);
            popup.getMenu().add(0, 3, 2, R.string.hc_filter_sort_price_asc);
            popup.getMenu().add(0, 4, 3, R.string.hc_filter_sort_price_desc);
            popup.getMenu().add(0, 5, 4, R.string.hc_filter_sort_popular);
            popup.setOnMenuItemClickListener(item -> {
                EventFilterCriteria.Sort sort;
                switch (item.getItemId()) {
                    case 2:
                        sort = EventFilterCriteria.Sort.NEWEST;
                        break;
                    case 3:
                        sort = EventFilterCriteria.Sort.PRICE_ASC;
                        break;
                    case 4:
                        sort = EventFilterCriteria.Sort.PRICE_DESC;
                        break;
                    case 5:
                        sort = EventFilterCriteria.Sort.POPULAR;
                        break;
                    case 1:
                    default:
                        sort = EventFilterCriteria.Sort.START_TIME_ASC;
                        break;
                }
                criteria.sort = sort;
                updateSortLabel();
                viewModel.applyFilters(criteria, committedQuery);
                return true;
            });
            popup.show();
        });
    }

    private void setupStates() {
        swipeRefresh.setColorSchemeResources(R.color.ds_primary);
        swipeRefresh.setOnRefreshListener(() ->
                viewModel.applyFilters(criteria, committedQuery));

        emptyState.setTitle(R.string.hc_list_empty_title);
        emptyState.setMessage(R.string.hc_list_empty_message);
        emptyState.setIcon(R.drawable.ic_ds_search);
        emptyState.setActionText(R.string.hc_list_clear_filters);
        emptyState.setOnActionClickListener(v -> {
            criteria = new EventFilterCriteria();
            committedQuery = "";
            searchInput.setText("");
            bindCriteriaViews();
            updateSortLabel();
            viewModel.applyFilters(criteria, "");
        });

        errorState.setTitle(R.string.hc_list_error_title);
        errorState.setMessage(R.string.hc_list_error_message);
        errorState.setOnRetryClickListener(v -> viewModel.retry());
    }

    private void observeData() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            swipeRefresh.setRefreshing(false);
            if (isLoading && firstLoad) {
                skeleton.setVisibility(View.VISIBLE);
                swipeRefresh.setVisibility(View.INVISIBLE);
                emptyState.setVisibility(View.GONE);
                errorState.setVisibility(View.GONE);
            } else {
                skeleton.setVisibility(View.GONE);
                if (!isLoading) {
                    firstLoad = false;
                }
                updateContentState();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), hasError -> {
            boolean error = Boolean.TRUE.equals(hasError);
            errorState.setVisibility(error ? View.VISIBLE : View.GONE);
            if (error) {
                swipeRefresh.setVisibility(View.GONE);
                emptyState.setVisibility(View.GONE);
                skeleton.setVisibility(View.GONE);
            } else {
                updateContentState();
            }
        });

        viewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            adapter.submitList(events);
            int count = events == null ? 0 : events.size();
            resultCount.setText(getString(R.string.hc_list_result_count, count));
            updateContentState();
        });

        viewModel.getLoadingMore().observe(getViewLifecycleOwner(), loadingMore ->
                adapter.setLoadingMore(Boolean.TRUE.equals(loadingMore)));

        viewModel.getHasMore().observe(getViewLifecycleOwner(), hasMore -> {
            // Pagination is driven by the scroll listener; this observer keeps the flag warm.
        });
    }

    private void updateContentState() {
        if (firstLoad || historyPanel.getVisibility() == View.VISIBLE) {
            return;
        }
        boolean loading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        boolean error = Boolean.TRUE.equals(viewModel.getError().getValue());
        if (loading || error) {
            return;
        }
        boolean empty = adapter.getItemCount() == 0;
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        emptyState.showAction(!criteria.isEmpty() || !committedQuery.isEmpty());
        swipeRefresh.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void bindCriteriaViews() {
        int active = criteria.activeCount();
        if (active > 0) {
            filterBadge.setText(String.format(Locale.US, "%d", active));
            filterBadge.setVisibility(View.VISIBLE);
        } else {
            filterBadge.setVisibility(View.GONE);
        }
        clearFilters.setVisibility(active > 0 || !committedQuery.isEmpty()
                ? View.VISIBLE : View.GONE);
    }

    private void updateSortLabel() {
        if (sortLabel == null) {
            return;
        }
        int labelRes;
        switch (criteria.sort) {
            case NEWEST:
                labelRes = R.string.hc_filter_sort_newest;
                break;
            case PRICE_ASC:
                labelRes = R.string.hc_filter_sort_price_asc;
                break;
            case PRICE_DESC:
                labelRes = R.string.hc_filter_sort_price_desc;
                break;
            case POPULAR:
                labelRes = R.string.hc_filter_sort_popular;
                break;
            case START_TIME_ASC:
            default:
                labelRes = R.string.hc_filter_sort_start;
                break;
        }
        sortLabel.setText(getString(R.string.hc_list_sort_prefix, getString(labelRes)));
    }

    private void registerFilterResult() {
        getParentFragmentManager().setFragmentResultListener(
                EventFilterFragment.REQUEST_KEY, getViewLifecycleOwner(), (key, bundle) -> {
                    Object value = bundle.getSerializable(EventFilterFragment.RESULT_CRITERIA);
                    if (!(value instanceof EventFilterCriteria)) {
                        return;
                    }
                    criteria = ((EventFilterCriteria) value).copy();
                    bindCriteriaViews();
                    updateSortLabel();
                    viewModel.applyFilters(criteria, committedQuery);
                });
    }

    private void openFilter() {
        Bundle args = new Bundle();
        args.putSerializable(EventFilterFragment.ARG_CRITERIA, criteria.copy());
        Navigation.findNavController(requireView())
                .navigate(R.id.event_filterFragment, args);
    }

    private void showKeyboard() {
        InputMethodManager manager = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null && searchInput != null) {
            searchInput.post(() -> manager.showSoftInput(searchInput,
                    InputMethodManager.SHOW_IMPLICIT));
        }
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null && searchInput != null) {
            manager.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    private static String normalizeQuery(String value) {
        return value == null ? "" : value.trim();
    }

    private static String textOf(TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
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
