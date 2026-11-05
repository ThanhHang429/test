package com.eventapp.app.ui.user;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eventapp.app.R;
import com.eventapp.app.core.session.SessionManager;
import com.eventapp.app.ui.model.CategoryNodeUiModel;
import com.eventapp.app.databinding.FragmentUserDiscoveryBinding;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.model.EventUiModel;
import com.eventapp.app.ui.user.adapter.EventAdapter;
import com.eventapp.app.ui.user.model.UserFilters;
import com.eventapp.app.ui.user.viewmodel.DiscoveryViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tab Khám phá: tìm kiếm, bộ lọc nâng cao, sự kiện nổi bật và danh sách sắp diễn ra. */
public class DiscoveryFragment extends Fragment {

    private static final long SEARCH_DEBOUNCE_MS = 320L;

    private FragmentUserDiscoveryBinding binding;
    private EventAdapter featuredAdapter;
    private EventAdapter listAdapter;

    private DiscoveryViewModel viewModel;
    private final List<CategoryNodeUiModel> categories = new ArrayList<>();
    private final List<String> cities = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final UserFilters filters = new UserFilters();

    private String query = "";
    private boolean firstResume = true;
    private Runnable pendingSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUserDiscoveryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        NavContentInset.apply(binding.getRoot());
        binding.tvGreeting.setText(greeting());

        EventAdapter.Listener listener = new EventAdapter.Listener() {
            @Override
            public void onOpen(@NonNull EventUiModel event) {
                EventDetailActivity.start(requireContext(), event.id);
            }

            @Override
            public void onToggleFavorite(@NonNull EventUiModel event) {
                toggleFavorite(event);
            }
        };

        featuredAdapter = new EventAdapter(true, listener);
        binding.rvFeatured.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvFeatured.setAdapter(featuredAdapter);

        listAdapter = new EventAdapter(false, listener);
        binding.rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEvents.setAdapter(listAdapter);

        viewModel = new ViewModelProvider(this).get(DiscoveryViewModel.class);
        observe();

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s == null ? "" : s.toString().trim();
                scheduleSearch();
            }
        });

        binding.btnFilter.setOnClickListener(v -> openFilterSheet());
        binding.btnReset.setOnClickListener(v -> resetFilters());
        binding.tvSeeAllFeatured.setOnClickListener(v -> binding.scroll.smoothScrollTo(0,
                binding.upcomingSection.getTop()));

        binding.scroll.setOnScrollChangeListener(
                (androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                        (v, x, scrollY, oldX, oldY) -> {
                            View child = binding.scroll.getChildAt(0);
                            if (child == null
                                    || Boolean.TRUE.equals(viewModel.getLoading().getValue())
                                    || !Boolean.TRUE.equals(viewModel.getHasNext().getValue())) {
                                return;
                            }
                            if (scrollY >= child.getMeasuredHeight()
                                    - binding.scroll.getMeasuredHeight() - dp(320)) {
                                viewModel.loadNextPage();
                            }
                        });

        viewModel.loadCategories();
        viewModel.loadCities();
        viewModel.search(filters, query);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Quay lại tab sau khi Admin sửa danh mục: tải lại để chip lọc thấy danh mục mới.
        if (firstResume) {
            firstResume = false;
        } else if (binding != null) {
            viewModel.loadCategories();
        }
    }

    private void observe() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), data -> {
            if (binding == null) {
                return;
            }
            categories.clear();
            categories.addAll(data);
            if (filters.categoryId != null && !containsCategory(filters.categoryId)) {
                filters.categoryId = null;
            }
            buildChips();
            renderAppliedFilters();
        });
        viewModel.getCities().observe(getViewLifecycleOwner(), data -> {
            cities.clear();
            cities.addAll(data);
        });
        viewModel.getEvents().observe(getViewLifecycleOwner(), data -> render());
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) {
                return;
            }
            boolean empty = viewModel.getEvents().getValue() == null
                    || viewModel.getEvents().getValue().isEmpty();
            binding.progress.setVisibility(Boolean.TRUE.equals(loading) && empty
                    ? View.VISIBLE : View.GONE);
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (binding == null || error == null) {
                return;
            }
            binding.tvError.setText(error.getUserMessage());
            binding.tvError.setVisibility(View.VISIBLE);
        });
        viewModel.getFavoriteError().observe(getViewLifecycleOwner(), error -> {
            if (binding == null || error == null) {
                return;
            }
            viewModel.consumeFavoriteError();
            SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(112));
        });
    }

    private boolean containsCategory(long id) {
        for (CategoryNodeUiModel node : categories) {
            if (node.id == id) {
                return true;
            }
        }
        return false;
    }

    private void buildChips() {
        binding.chipRow.removeAllViews();
        addChip(getString(R.string.user_cat_all), null, false);
        for (CategoryNodeUiModel node : categories) {
            addChip(node.name, node.id, false);
        }
        addChip(getString(R.string.user_cat_free), null, true);
    }

    private void addChip(String label, @Nullable Long id, boolean free) {
        TextView chip = new TextView(new ContextThemeWrapper(requireContext(),
                R.style.Widget_EventApp_User_Chip));
        chip.setText(label);
        chip.setSelected(sameFilter(id, free));
        chip.setOnClickListener(v -> {
            filters.categoryId = id;
            filters.priceType = free ? "FREE" : null;
            filters.priceMin = null;
            filters.priceMax = null;
            buildChips();
            renderAppliedFilters();
            refresh();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        params.setMarginEnd(dp(8));
        binding.chipRow.addView(chip, params);
    }

    private boolean sameFilter(@Nullable Long id, boolean free) {
        if (free) {
            return "FREE".equals(filters.priceType);
        }
        if (filters.priceType != null) {
            return false;
        }
        return id == null ? filters.categoryId == null : id.equals(filters.categoryId);
    }

    // ----------------------------------------------------------------- filter

    private void openFilterSheet() {
        UserFilterSheet sheet = new UserFilterSheet(requireContext(), filters, categories, cities);
        sheet.setListener(applied -> {
            copyInto(filters, applied);
            buildChips();
            renderAppliedFilters();
            refresh();
        });
        sheet.show();
    }

    private void copyInto(UserFilters target, UserFilters source) {
        target.dateFrom = source.dateFrom;
        target.dateTo = source.dateTo;
        target.priceType = source.priceType;
        target.priceMin = source.priceMin;
        target.priceMax = source.priceMax;
        target.city = source.city;
        target.categoryId = source.categoryId;
        target.sort = source.sort;
    }

    /** Chip tóm tắt các điều kiện đang bật, bấm X để bỏ nhanh từng điều kiện. */
    private void renderAppliedFilters() {
        if (binding == null) {
            return;
        }
        binding.filterSummaryRow.removeAllViews();
        int count = filters.activeCount();

        if (filters.dateFrom != null || filters.dateTo != null) {
            addSummaryChip(dateSummary(), () -> {
                filters.dateFrom = null;
                filters.dateTo = null;
            });
        }
        if (filters.priceType != null || filters.priceMin != null || filters.priceMax != null) {
            addSummaryChip(priceSummary(), () -> {
                filters.priceType = null;
                filters.priceMin = null;
                filters.priceMax = null;
            });
        }
        if (filters.city != null) {
            addSummaryChip(filters.city, () -> filters.city = null);
        }
        if (filters.categoryId != null) {
            addSummaryChip(categoryName(filters.categoryId), () -> filters.categoryId = null);
        }

        boolean hasChips = binding.filterSummaryRow.getChildCount() > 0;
        binding.filterSummaryScroll.setVisibility(hasChips ? View.VISIBLE : View.GONE);

        binding.tvFilterBadge.setText(count > 9 ? "9+" : String.valueOf(count));
        binding.tvFilterBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }

    private void addSummaryChip(String label, Runnable onRemove) {
        TextView chip = new TextView(requireContext());
        chip.setText(label);
        chip.setTextSize(12f);
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.user_primary));
        chip.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(
                requireContext(), R.font.plus_jakarta_sans),
                android.graphics.Typeface.BOLD);
        chip.setBackgroundResource(R.drawable.bg_user_filter_summary);
        chip.setPadding(dp(12), 0, dp(10), 0);
        chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        chip.setCompoundDrawablePadding(dp(6));
        chip.setMinHeight(dp(32));
        chip.setHeight(dp(32));

        Drawable close = ContextCompat.getDrawable(requireContext(), R.drawable.ic_user_close);
        if (close != null) {
            close.setBounds(0, 0, dp(13), dp(13));
            close.setTint(ContextCompat.getColor(requireContext(), R.color.user_primary));
            chip.setCompoundDrawablesRelative(null, null, close, null);
        }
        chip.setContentDescription(getString(R.string.user_filter_remove, label));
        chip.setOnClickListener(v -> {
            onRemove.run();
            buildChips();
            renderAppliedFilters();
            refresh();
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
        params.setMarginEnd(dp(8));
        binding.filterSummaryRow.addView(chip, params);
    }

    private String dateSummary() {
        SimpleDateFormat display = new SimpleDateFormat("dd/MM", Locale.getDefault());
        SimpleDateFormat api = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            String from = filters.dateFrom == null ? "…" : display.format(api.parse(filters.dateFrom));
            String to = filters.dateTo == null ? "…" : display.format(api.parse(filters.dateTo));
            return from + " – " + to;
        } catch (java.text.ParseException ignored) {
            return getString(R.string.user_filter_section_time);
        }
    }

    private String priceSummary() {
        if ("FREE".equals(filters.priceType)) {
            return getString(R.string.user_filter_price_free);
        }
        if (filters.priceMin != null && filters.priceMax == null) {
            return "≥ " + formatMoney(filters.priceMin);
        }
        if (filters.priceMin == null && filters.priceMax != null) {
            return "≤ " + formatMoney(filters.priceMax);
        }
        if (filters.priceMin != null) {
            return formatMoney(filters.priceMin) + " – " + formatMoney(filters.priceMax);
        }
        return getString(R.string.user_filter_section_price);
    }

    private String formatMoney(long value) {
        if (value >= 1_000_000L) {
            long millions = value / 1_000_000L;
            return millions + "tr";
        }
        return (value / 1_000L) + "K";
    }

    private String categoryName(long id) {
        for (CategoryNodeUiModel node : categories) {
            if (node.id == id) {
                return node.name;
            }
        }
        return getString(R.string.user_filter_section_category);
    }

    private void resetFilters() {
        filters.clear();
        query = "";
        binding.etSearch.setText("");
        buildChips();
        renderAppliedFilters();
        refresh();
    }

    // ----------------------------------------------------------------- search

    private void scheduleSearch() {
        if (pendingSearch != null) {
            handler.removeCallbacks(pendingSearch);
        }
        pendingSearch = () -> refresh();
        handler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }

    private void refresh() {
        if (binding == null) {
            return;
        }
        binding.tvError.setVisibility(View.GONE);
        viewModel.search(filters, query);
    }

    private void render() {
        if (binding == null) {
            return;
        }
        List<EventUiModel> items = viewModel.getEvents().getValue();
        if (items == null) {
            items = new ArrayList<>();
        }
        listAdapter.setItems(items);
        boolean empty = items.isEmpty();
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.tvResultCount.setText(getString(R.string.user_event_count, items.size()));

        boolean simpleView = !filters.hasAny() && query.isEmpty();
        binding.featuredSection.setVisibility(simpleView ? View.VISIBLE : View.GONE);
        if (simpleView) {
            List<EventUiModel> featured = new ArrayList<>();
            for (EventUiModel event : items) {
                if (featured.size() >= 4) {
                    break;
                }
                featured.add(event);
            }
            featuredAdapter.setItems(featured);
        }
    }

    private void toggleFavorite(EventUiModel event) {
        boolean target = !event.favorite;
        // ViewModel cập nhật lạc quan và tự hoàn tác nếu server từ chối.
        viewModel.toggleFavorite(event);
        SnackPill.show(binding.getRoot(), getString(
                target ? R.string.user_saved_toast : R.string.user_unsaved_toast), dp(112));
    }

    private String greeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int res;
        if (hour < 12) {
            res = R.string.user_greeting_morning;
        } else if (hour < 18) {
            res = R.string.user_greeting_afternoon;
        } else {
            res = R.string.user_greeting_evening;
        }
        String name = SessionManager.get().getSession() == null
                ? null
                : SessionManager.get().getSession().fullName;
        if (name == null || name.trim().isEmpty()) {
            return getString(res, getString(R.string.user_greeting_fallback));
        }
        String[] parts = name.trim().split("\\s+");
        return getString(res, parts[parts.length - 1]);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        if (pendingSearch != null) {
            handler.removeCallbacks(pendingSearch);
        }
        super.onDestroyView();
        binding = null;
    }
}
