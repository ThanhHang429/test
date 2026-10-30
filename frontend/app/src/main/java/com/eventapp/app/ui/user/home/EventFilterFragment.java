package com.eventapp.app.ui.user.home;


import com.eventapp.app.data.model.EventFilterCriteria;
import com.eventapp.app.ui.model.EventCategoryUi;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * FR-U04 filter sheet. It stays open on configuration changes through
 * {@link EventFilterViewModel} and returns the criteria to the caller with FragmentResult.
 */
public class EventFilterFragment extends BottomSheetDialogFragment {

    public static final String REQUEST_KEY = "event_filter_request";
    public static final String ARG_CRITERIA = "arg_filter_criteria";
    public static final String RESULT_CRITERIA = "result_filter_criteria";

    private static final float PRICE_MAX = EventFilterCriteria.MAX_PRICE;

    private EventFilterViewModel viewModel;
    private EventFilterCriteria draft;

    private ChipGroup categoryGroup;
    private ChipGroup timeGroup;
    private ChipGroup formatGroup;
    private ChipGroup priceGroup;
    private ChipGroup sortGroup;
    private View customDateLayout;
    private MaterialButton dateFromButton;
    private MaterialButton dateToButton;
    private TextView dateSummary;
    private MaterialAutoCompleteTextView cityInput;
    private RangeSlider priceSlider;
    private TextView priceSummary;
    private MaterialSwitch availableSwitch;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));

    private boolean binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(ignored -> {
            FrameLayout sheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (sheet == null) {
                return;
            }
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setDraggable(false);
        });
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        FrameLayout sheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
        if (sheet == null) {
            return;
        }
        ViewGroup.LayoutParams params = sheet.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        sheet.setLayoutParams(params);
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
        behavior.setDraggable(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_home_event_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(EventFilterViewModel.class);
        if (!viewModel.hasCriteria()) {
            EventFilterCriteria initial = new EventFilterCriteria();
            Bundle args = getArguments();
            if (args != null) {
                Object value = args.getSerializable(ARG_CRITERIA);
                if (value instanceof EventFilterCriteria) {
                    initial = ((EventFilterCriteria) value).copy();
                }
            }
            viewModel.setCriteria(initial);
        }
        draft = viewModel.getCriteria();

        bindViews(view);
        buildCategoryChips();
        bindListeners();
        render();
    }

    private void bindViews(View view) {
        categoryGroup = view.findViewById(R.id.chipGroupFilterCategories);
        timeGroup = view.findViewById(R.id.chipGroupFilterTime);
        formatGroup = view.findViewById(R.id.chipGroupFilterFormat);
        priceGroup = view.findViewById(R.id.chipGroupFilterPrice);
        sortGroup = view.findViewById(R.id.chipGroupFilterSort);
        customDateLayout = view.findViewById(R.id.layoutFilterCustomDate);
        dateFromButton = view.findViewById(R.id.btnFilterDateFrom);
        dateToButton = view.findViewById(R.id.btnFilterDateTo);
        dateSummary = view.findViewById(R.id.tvFilterDateSummary);
        cityInput = view.findViewById(R.id.actFilterCity);
        priceSlider = view.findViewById(R.id.sliderFilterPrice);
        priceSummary = view.findViewById(R.id.tvFilterPriceSummary);
        availableSwitch = view.findViewById(R.id.switchFilterAvailable);

        view.findViewById(R.id.btnFilterClose).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnFilterReset).setOnClickListener(v -> {
            draft.reset();
            render();
        });
        view.findViewById(R.id.btnFilterApply).setOnClickListener(v -> apply());
    }

    private void buildCategoryChips() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (EventCategory category : EventCategory.values()) {
            long id = EventCategoryUi.id(category);
            Chip chip = (Chip) inflater.inflate(R.layout.view_ds_filter_chip, categoryGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(EventCategoryUi.label(requireContext(), category));
            chip.setTag(id);
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (binding) {
                    return;
                }
                if (checked) {
                    draft.categoryIds.add(id);
                } else {
                    draft.categoryIds.remove(id);
                }
            });
            categoryGroup.addView(chip);
        }
    }

    private void bindListeners() {
        timeGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (binding || checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            EventFilterCriteria.TimePreset preset = EventFilterCriteria.TimePreset.ANY;
            if (checked == R.id.chipFilterTimeToday) {
                preset = EventFilterCriteria.TimePreset.TODAY;
            } else if (checked == R.id.chipFilterTimeWeek) {
                preset = EventFilterCriteria.TimePreset.THIS_WEEK;
            } else if (checked == R.id.chipFilterTimeMonth) {
                preset = EventFilterCriteria.TimePreset.THIS_MONTH;
            } else if (checked == R.id.chipFilterTimeCustom) {
                preset = EventFilterCriteria.TimePreset.CUSTOM;
            }
            draft.timePreset = preset;
            if (preset == EventFilterCriteria.TimePreset.CUSTOM) {
                customDateLayout.setVisibility(View.VISIBLE);
                if (draft.dateFrom <= 0L || draft.dateTo <= 0L) {
                    openDatePicker();
                }
            } else {
                customDateLayout.setVisibility(View.GONE);
                draft.dateFrom = 0L;
                draft.dateTo = 0L;
            }
        });

        formatGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (binding || checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            if (checked == R.id.chipFilterFormatOnsite) {
                draft.format = EventFilterCriteria.Format.ONSITE;
            } else if (checked == R.id.chipFilterFormatOnline) {
                draft.format = EventFilterCriteria.Format.ONLINE;
            } else {
                draft.format = EventFilterCriteria.Format.ANY;
            }
        });

        priceGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (binding || checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            if (checked == R.id.chipFilterPriceFree) {
                draft.priceType = EventFilterCriteria.PriceType.FREE;
                draft.priceRangeSet = false;
            } else if (checked == R.id.chipFilterPricePaid) {
                draft.priceType = EventFilterCriteria.PriceType.PAID;
            } else {
                draft.priceType = EventFilterCriteria.PriceType.ANY;
            }
            if (draft.priceType == EventFilterCriteria.PriceType.FREE) {
                binding = true;
                priceSlider.setValues(0f, PRICE_MAX);
                binding = false;
                draft.priceRangeSet = false;
                draft.priceMin = 0L;
                draft.priceMax = (long) PRICE_MAX;
            }
            updatePriceState();
        });

        sortGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (binding || checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            if (checked == R.id.chipFilterSortNewest) {
                draft.sort = EventFilterCriteria.Sort.NEWEST;
            } else if (checked == R.id.chipFilterSortPriceAsc) {
                draft.sort = EventFilterCriteria.Sort.PRICE_ASC;
            } else if (checked == R.id.chipFilterSortPriceDesc) {
                draft.sort = EventFilterCriteria.Sort.PRICE_DESC;
            } else if (checked == R.id.chipFilterSortPopular) {
                draft.sort = EventFilterCriteria.Sort.POPULAR;
            } else {
                draft.sort = EventFilterCriteria.Sort.START_TIME_ASC;
            }
        });

        dateFromButton.setOnClickListener(v -> openDatePicker());
        dateToButton.setOnClickListener(v -> openDatePicker());

        String[] cities = getResources().getStringArray(R.array.hc_cities);
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, cities);
        cityInput.setAdapter(cityAdapter);
        cityInput.setOnItemClickListener((parent, view, position, id) -> {
            draft.city = position == 0 ? "" : cities[position];
        });

        priceSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (binding) {
                return;
            }
            List<Float> values = slider.getValues();
            if (values.size() < 2) {
                return;
            }
            long min = Math.round(values.get(0));
            long max = Math.round(values.get(1));
            draft.priceMin = min;
            draft.priceMax = max;
            draft.priceRangeSet = min > 0L || max < (long) PRICE_MAX;
            updatePriceSummary();
        });

        availableSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!binding) {
                draft.availableOnly = checked;
            }
        });
    }

    private void render() {
        binding = true;
        try {
            for (int i = 0; i < categoryGroup.getChildCount(); i++) {
                View child = categoryGroup.getChildAt(i);
                if (child instanceof Chip && child.getTag() instanceof Long) {
                    ((Chip) child).setChecked(
                            draft.categoryIds.contains((Long) child.getTag()));
                }
            }

            int timeChip = R.id.chipFilterTimeAny;
            switch (draft.timePreset) {
                case TODAY:
                    timeChip = R.id.chipFilterTimeToday;
                    break;
                case THIS_WEEK:
                    timeChip = R.id.chipFilterTimeWeek;
                    break;
                case THIS_MONTH:
                    timeChip = R.id.chipFilterTimeMonth;
                    break;
                case CUSTOM:
                    timeChip = R.id.chipFilterTimeCustom;
                    break;
                case ANY:
                default:
                    break;
            }
            timeGroup.check(timeChip);
            customDateLayout.setVisibility(draft.timePreset
                    == EventFilterCriteria.TimePreset.CUSTOM ? View.VISIBLE : View.GONE);
            renderDates();

            int formatChip = R.id.chipFilterFormatAny;
            if (draft.format == EventFilterCriteria.Format.ONSITE) {
                formatChip = R.id.chipFilterFormatOnsite;
            } else if (draft.format == EventFilterCriteria.Format.ONLINE) {
                formatChip = R.id.chipFilterFormatOnline;
            }
            formatGroup.check(formatChip);

            int priceChip = R.id.chipFilterPriceAny;
            if (draft.priceType == EventFilterCriteria.PriceType.FREE) {
                priceChip = R.id.chipFilterPriceFree;
            } else if (draft.priceType == EventFilterCriteria.PriceType.PAID) {
                priceChip = R.id.chipFilterPricePaid;
            }
            priceGroup.check(priceChip);

            float min = draft.priceRangeSet ? clampPrice(draft.priceMin) : 0f;
            float max = draft.priceRangeSet ? clampPrice(draft.priceMax) : PRICE_MAX;
            if (min > max) {
                float swap = min;
                min = max;
                max = swap;
            }
            priceSlider.setValues(min, max);
            updatePriceState();

            int sortChip = R.id.chipFilterSortStart;
            switch (draft.sort) {
                case NEWEST:
                    sortChip = R.id.chipFilterSortNewest;
                    break;
                case PRICE_ASC:
                    sortChip = R.id.chipFilterSortPriceAsc;
                    break;
                case PRICE_DESC:
                    sortChip = R.id.chipFilterSortPriceDesc;
                    break;
                case POPULAR:
                    sortChip = R.id.chipFilterSortPopular;
                    break;
                case START_TIME_ASC:
                default:
                    break;
            }
            sortGroup.check(sortChip);

            String[] cities = getResources().getStringArray(R.array.hc_cities);
            cityInput.setText(draft.city.trim().isEmpty() ? cities[0] : draft.city, false);
            availableSwitch.setChecked(draft.availableOnly);
        } finally {
            binding = false;
        }
    }

    private void renderDates() {
        if (draft.dateFrom > 0L && draft.dateTo > 0L) {
            String from = dateFormat.format(new Date(draft.dateFrom));
            String to = dateFormat.format(new Date(draft.dateTo));
            dateFromButton.setText(from);
            dateToButton.setText(to);
            dateSummary.setText(getString(R.string.hc_filter_date_range, from, to));
        } else {
            dateFromButton.setText(R.string.hc_filter_date_from);
            dateToButton.setText(R.string.hc_filter_date_to);
            dateSummary.setText("");
        }
    }

    private void updatePriceState() {
        boolean free = draft.priceType == EventFilterCriteria.PriceType.FREE;
        priceSlider.setEnabled(!free);
        priceSlider.setAlpha(free ? 0.45f : 1f);
        updatePriceSummary();
    }

    private void updatePriceSummary() {
        List<Float> values = priceSlider.getValues();
        if (values.size() < 2) {
            return;
        }
        long min = Math.round(values.get(0));
        long max = Math.round(values.get(1));
        if (min <= 0L && max >= (long) PRICE_MAX) {
            priceSummary.setText(R.string.hc_filter_price_unlimited);
        } else {
            priceSummary.setText(getString(R.string.hc_filter_price_summary,
                    PriceTextView.formatVnd(min), PriceTextView.formatVnd(max)));
        }
    }

    private void openDatePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(R.string.hc_filter_time_custom);
        if (draft.dateFrom > 0L && draft.dateTo > 0L) {
            builder.setSelection(new Pair<>(
                    localStartOfDayToUtc(draft.dateFrom),
                    localStartOfDayToUtc(draft.dateTo)));
        }
        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null || selection.first == null || selection.second == null) {
                return;
            }
            long from = utcToLocalStartOfDay(selection.first);
            long to = utcToLocalEndOfDay(selection.second);
            if (to - from > 366L * 24L * 60L * 60L * 1000L) {
                Toast.makeText(requireContext(), R.string.hc_filter_date_max,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            draft.timePreset = EventFilterCriteria.TimePreset.CUSTOM;
            draft.dateFrom = from;
            draft.dateTo = to;
            binding = true;
            timeGroup.check(R.id.chipFilterTimeCustom);
            binding = false;
            customDateLayout.setVisibility(View.VISIBLE);
            renderDates();
        });
        picker.show(getChildFragmentManager(), "filter_date_range");
    }

    private void apply() {
        if (draft.timePreset == EventFilterCriteria.TimePreset.CUSTOM
                && (draft.dateFrom <= 0L || draft.dateTo <= 0L)) {
            draft.timePreset = EventFilterCriteria.TimePreset.ANY;
        }
        if (draft.priceType == EventFilterCriteria.PriceType.FREE) {
            draft.priceRangeSet = false;
        }
        Bundle result = new Bundle();
        result.putSerializable(RESULT_CRITERIA, draft.copy());
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    private static float clampPrice(long value) {
        float clamped = Math.max(0f, Math.min(PRICE_MAX, value));
        return Math.round(clamped / 50000f) * 50000f;
    }

    private static long utcToLocalStartOfDay(long utcMillis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMillis);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        return local.getTimeInMillis();
    }

    private static long utcToLocalEndOfDay(long utcMillis) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(utcToLocalStartOfDay(utcMillis));
        start.add(Calendar.DAY_OF_MONTH, 1);
        return start.getTimeInMillis() - 1L;
    }

    /** MaterialDatePicker works in UTC; the stored filter uses local start of day. */
    private static long localStartOfDayToUtc(long localMillis) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(localMillis);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        return utc.getTimeInMillis();
    }
}
