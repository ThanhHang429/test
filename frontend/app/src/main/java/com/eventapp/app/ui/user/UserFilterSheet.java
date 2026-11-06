package com.eventapp.app.ui.user;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.R;
import com.eventapp.app.ui.model.CategoryNodeUiModel;
import com.eventapp.app.databinding.SheetUserFilterBinding;
import com.eventapp.app.ui.user.model.UserFilters;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Bottom sheet lọc sự kiện của tab Khám phá: sắp xếp, danh mục, thời gian, khoảng giá, địa điểm.
 *
 * <p>Sheet làm việc trên một bản nháp của {@link UserFilters}; chỉ khi bấm «Xem kết quả» mới trả
 * về màn hình, nhờ đó đóng sheet giữa đường không làm thay đổi danh sách đang xem.</p>
 */
public class UserFilterSheet extends BottomSheetDialog {

    public interface Listener {
        void onFiltersApplied(@NonNull UserFilters filters);
    }

    private static final String PRICE_FREE = "FREE";
    private static final String PRICE_UNDER_200 = "0-200000";
    private static final String PRICE_200_500 = "200000-500000";
    private static final String PRICE_500_1M = "500000-1000000";
    private static final String PRICE_OVER_1M = "1000000-";

    private final SheetUserFilterBinding binding;
    private final UserFilters draft;
    private final List<CategoryNodeUiModel> categories;
    private final List<String> cities;
    private final SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    private Listener listener;

    public UserFilterSheet(@NonNull Context context,
                           @NonNull UserFilters current,
                           @NonNull List<CategoryNodeUiModel> categories,
                           @NonNull List<String> cities) {
        super(context);
        this.draft = current.copy();
        this.categories = categories;
        this.cities = cities;
        binding = SheetUserFilterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        bindActions();
        renderGroups();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onStart() {
        super.onStart();
        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
    }

    private void bindActions() {
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnClear.setOnClickListener(v -> {
            draft.clear();
            renderGroups();
        });
        binding.btnApply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFiltersApplied(draft.copy());
            }
            dismiss();
        });
        binding.btnDateFrom.setOnClickListener(v -> pickDate(true));
        binding.btnDateTo.setOnClickListener(v -> pickDate(false));
    }

    // ---------------------------------------------------------------- render

    private void renderGroups() {
        renderSort();
        renderCategories();
        renderTime();
        renderPrice();
        renderCities();
        updateDateFields();
    }

    private void renderSort() {
        binding.groupSort.removeAllViews();
        addOption(binding.groupSort, getString(R.string.user_filter_sort_relevance),
                draft.sort == null,
                () -> setSort(UserFilters.SORT_AUTO),
                () -> setSort(UserFilters.SORT_AUTO));
        addOption(binding.groupSort, getString(R.string.user_filter_sort_upcoming),
                UserFilters.SORT_UPCOMING.equals(draft.sort),
                () -> setSort(UserFilters.SORT_UPCOMING),
                () -> setSort(UserFilters.SORT_AUTO));
        addOption(binding.groupSort, getString(R.string.user_filter_sort_newest),
                UserFilters.SORT_NEWEST.equals(draft.sort),
                () -> setSort(UserFilters.SORT_NEWEST),
                () -> setSort(UserFilters.SORT_AUTO));
        addOption(binding.groupSort, getString(R.string.user_filter_sort_price_asc),
                UserFilters.SORT_PRICE_ASC.equals(draft.sort),
                () -> setSort(UserFilters.SORT_PRICE_ASC),
                () -> setSort(UserFilters.SORT_AUTO));
        addOption(binding.groupSort, getString(R.string.user_filter_sort_price_desc),
                UserFilters.SORT_PRICE_DESC.equals(draft.sort),
                () -> setSort(UserFilters.SORT_PRICE_DESC),
                () -> setSort(UserFilters.SORT_AUTO));
        addOption(binding.groupSort, getString(R.string.user_filter_sort_popular),
                UserFilters.SORT_POPULAR.equals(draft.sort),
                () -> setSort(UserFilters.SORT_POPULAR),
                () -> setSort(UserFilters.SORT_AUTO));
    }

    private void renderCategories() {
        binding.groupCategory.removeAllViews();
        addOption(binding.groupCategory, getString(R.string.user_cat_all),
                draft.categoryId == null,
                () -> {
                    draft.categoryId = null;
                    refresh();
                },
                () -> {
                    draft.categoryId = null;
                    refresh();
                });
        for (CategoryNodeUiModel node : categories) {
            final long id = node.id;
            addOption(binding.groupCategory, node.name,
                    draft.categoryId != null && draft.categoryId == id,
                    () -> {
                        draft.categoryId = id;
                        refresh();
                    },
                    () -> {
                        draft.categoryId = null;
                        refresh();
                    });
        }
    }

    private void renderTime() {
        binding.groupTime.removeAllViews();
        long[] today = rangeToday();
        long[] week = rangeWeek();
        long[] month = rangeMonth();
        addOption(binding.groupTime, getString(R.string.user_filter_time_today),
                matchesRange(today), () -> setRange(today), this::clearRange);
        addOption(binding.groupTime, getString(R.string.user_filter_time_week),
                matchesRange(week), () -> setRange(week), this::clearRange);
        addOption(binding.groupTime, getString(R.string.user_filter_time_month),
                matchesRange(month), () -> setRange(month), this::clearRange);
        addOption(binding.groupTime, getString(R.string.user_filter_time_custom),
                isCustomRange(today, week, month), () -> pickDate(true), this::clearRange);
    }

    private void renderPrice() {
        binding.groupPrice.removeAllViews();
        String current = currentPriceToken();
        addOption(binding.groupPrice, getString(R.string.user_cat_all),
                current == null, () -> setPrice(null), () -> setPrice(null));
        addOption(binding.groupPrice, getString(R.string.user_filter_price_free),
                PRICE_FREE.equals(current), () -> setPrice(PRICE_FREE), () -> setPrice(null));
        addOption(binding.groupPrice, getString(R.string.user_filter_price_under_200),
                PRICE_UNDER_200.equals(current), () -> setPrice(PRICE_UNDER_200), () -> setPrice(null));
        addOption(binding.groupPrice, getString(R.string.user_filter_price_200_500),
                PRICE_200_500.equals(current), () -> setPrice(PRICE_200_500), () -> setPrice(null));
        addOption(binding.groupPrice, getString(R.string.user_filter_price_500_1m),
                PRICE_500_1M.equals(current), () -> setPrice(PRICE_500_1M), () -> setPrice(null));
        addOption(binding.groupPrice, getString(R.string.user_filter_price_over_1m),
                PRICE_OVER_1M.equals(current), () -> setPrice(PRICE_OVER_1M), () -> setPrice(null));
    }

    private void renderCities() {
        binding.groupCity.removeAllViews();
        addOption(binding.groupCity, getString(R.string.user_filter_place_all),
                draft.city == null,
                () -> {
                    draft.city = null;
                    refresh();
                },
                () -> {
                    draft.city = null;
                    refresh();
                });
        for (String city : cities) {
            addOption(binding.groupCity, city, city.equals(draft.city),
                    () -> {
                        draft.city = city;
                        refresh();
                    },
                    () -> {
                        draft.city = null;
                        refresh();
                    });
        }
    }

    private void updateDateFields() {
        Date from = parse(draft.dateFrom);
        binding.btnDateFrom.setText(from == null
                ? getString(R.string.user_filter_date_from) : displayFormat.format(from));
        binding.btnDateFrom.setSelected(from != null);

        Date to = parse(draft.dateTo);
        binding.btnDateTo.setText(to == null
                ? getString(R.string.user_filter_date_to) : displayFormat.format(to));
        binding.btnDateTo.setSelected(to != null);
    }

    // ---------------------------------------------------------------- actions

    private void setSort(@Nullable String sort) {
        draft.sort = sort;
        refresh();
    }

    private void setPrice(@Nullable String token) {
        draft.priceType = null;
        draft.priceMin = null;
        draft.priceMax = null;
        if (token != null) {
            switch (token) {
                case PRICE_FREE:
                    draft.priceType = "FREE";
                    break;
                case PRICE_UNDER_200:
                    draft.priceMin = 0L;
                    draft.priceMax = 200_000L;
                    break;
                case PRICE_200_500:
                    draft.priceMin = 200_000L;
                    draft.priceMax = 500_000L;
                    break;
                case PRICE_500_1M:
                    draft.priceMin = 500_000L;
                    draft.priceMax = 1_000_000L;
                    break;
                case PRICE_OVER_1M:
                    draft.priceMin = 1_000_000L;
                    break;
                default:
                    break;
            }
        }
        refresh();
    }

    @Nullable
    private String currentPriceToken() {
        if ("FREE".equals(draft.priceType)) {
            return PRICE_FREE;
        }
        if (draft.priceType != null || (draft.priceMin == null && draft.priceMax == null)) {
            return null;
        }
        long min = draft.priceMin == null ? -1L : draft.priceMin;
        long max = draft.priceMax == null ? -1L : draft.priceMax;
        if (min == 0L && max == 200_000L) {
            return PRICE_UNDER_200;
        }
        if (min == 200_000L && max == 500_000L) {
            return PRICE_200_500;
        }
        if (min == 500_000L && max == 1_000_000L) {
            return PRICE_500_1M;
        }
        if (min == 1_000_000L && max == -1L) {
            return PRICE_OVER_1M;
        }
        return null;
    }

    private void setRange(long[] range) {
        draft.dateFrom = apiFormat.format(new Date(range[0]));
        draft.dateTo = apiFormat.format(new Date(range[1]));
        refresh();
    }

    private void clearRange() {
        draft.dateFrom = null;
        draft.dateTo = null;
        refresh();
    }

    private boolean matchesRange(long[] range) {
        return draft.dateFrom != null && draft.dateTo != null
                && draft.dateFrom.equals(apiFormat.format(new Date(range[0])))
                && draft.dateTo.equals(apiFormat.format(new Date(range[1])));
    }

    private boolean isCustomRange(long[] today, long[] week, long[] month) {
        return draft.dateFrom != null && !matchesRange(today) && !matchesRange(week)
                && !matchesRange(month);
    }

    private void pickDate(boolean isFrom) {
        Calendar calendar = Calendar.getInstance();
        Date parsed = parse(isFrom ? draft.dateFrom : draft.dateTo);
        if (parsed != null) {
            calendar.setTime(parsed);
        }
        new DatePickerDialog(getContext(), (view, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day, 12, 0, 0);
            picked.set(Calendar.MILLISECOND, 0);
            String iso = apiFormat.format(picked.getTime());
            if (isFrom) {
                draft.dateFrom = iso;
                if (draft.dateTo != null && draft.dateTo.compareTo(iso) < 0) {
                    draft.dateTo = iso;
                }
            } else {
                draft.dateTo = iso;
                if (draft.dateFrom != null && draft.dateFrom.compareTo(iso) > 0) {
                    draft.dateFrom = iso;
                }
            }
            refresh();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /** Vẽ lại toàn bộ chip để trạng thái chọn luôn khớp bản nháp. */
    private void refresh() {
        binding.getRoot().post(this::renderGroups);
    }

    // ---------------------------------------------------------------- helpers

    private void addOption(ChipGroup group, String label, boolean checked,
                           Runnable onCheck, Runnable onUncheck) {
        Chip chip = (Chip) LayoutInflater.from(getContext())
                .inflate(R.layout.item_user_filter_chip, group, false);
        chip.setText(label);
        chip.setOnClickListener(v -> {
            if (((Chip) v).isChecked()) {
                onCheck.run();
            } else {
                onUncheck.run();
            }
        });
        group.addView(chip);
        chip.setChecked(checked);
    }

    private String getString(int resId) {
        return getContext().getString(resId);
    }

    @Nullable
    private Date parse(@Nullable String iso) {
        if (iso == null) {
            return null;
        }
        try {
            return apiFormat.parse(iso);
        } catch (ParseException ignored) {
            return null;
        }
    }

    private long[] rangeToday() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 12);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return new long[]{start.getTimeInMillis(), start.getTimeInMillis()};
    }

    private long[] rangeWeek() {
        Calendar start = Calendar.getInstance();
        start.setFirstDayOfWeek(Calendar.MONDAY);
        start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        start.set(Calendar.HOUR_OF_DAY, 12);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);
        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }

    private long[] rangeMonth() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 12);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }
}
