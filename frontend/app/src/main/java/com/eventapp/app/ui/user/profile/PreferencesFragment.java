package com.eventapp.app.ui.user.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.ui.common.view.BudgetInputView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.eventapp.app.ui.user.home.EventCategoryUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** FR-U03 preferences form: categories, format, budget, cities and preferred days. */
public class PreferencesFragment extends Fragment {

    private PreferencesViewModel viewModel;

    private NestedScrollView scrollView;
    private SkeletonLayout skeleton;
    private ErrorStateView errorState;
    private ChipGroup categoryGroup;
    private ChipGroup formatGroup;
    private ChipGroup priceGroup;
    private ChipGroup cityGroup;
    private ChipGroup dayGroup;
    private Chip chipWeekday;
    private Chip chipWeekend;
    private BudgetInputView budgetView;
    private SectionHeaderView categoryHeader;
    private TextView hiddenNote;
    private MaterialButton saveButton;
    private MaterialButton resetButton;
    private LinearProgressIndicator progress;

    private boolean binding;
    private boolean firstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_preferences, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PreferencesViewModel.class);
        bindViews(view);
        setupToolbar();
        setupHeaders();
        setupFormat();
        setupPrice();
        setupBudget();
        setupDays();
        setupStates();
        observe();
    }

    private void bindViews(View view) {
        scrollView = view.findViewById(R.id.scrollPreferences);
        skeleton = view.findViewById(R.id.skeletonPreferences);
        errorState = view.findViewById(R.id.errorPreferences);
        categoryGroup = view.findViewById(R.id.chipGroupPrefCategories);
        formatGroup = view.findViewById(R.id.chipGroupPrefFormat);
        priceGroup = view.findViewById(R.id.chipGroupPrefPrice);
        cityGroup = view.findViewById(R.id.chipGroupPrefCities);
        dayGroup = view.findViewById(R.id.chipGroupPrefDays);
        chipWeekday = view.findViewById(R.id.chipPrefWeekday);
        chipWeekend = view.findViewById(R.id.chipPrefWeekend);
        budgetView = view.findViewById(R.id.budgetPref);
        categoryHeader = view.findViewById(R.id.headerPrefCategories);
        hiddenNote = view.findViewById(R.id.tvPrefHiddenNote);
        saveButton = view.findViewById(R.id.btnPrefSave);
        resetButton = view.findViewById(R.id.btnPrefReset);
        progress = view.findViewById(R.id.progressPrefSave);
    }

    private void setupToolbar() {
        requireView().findViewById(R.id.btnPrefBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
        requireView().findViewById(R.id.btnPrefSkip).setOnClickListener(v -> skip());
    }

    private void setupHeaders() {
        categoryHeader.setTitle(R.string.hc_pref_categories_title);
        categoryHeader.setSubtitle(R.string.hc_pref_categories_hint);
        categoryHeader.getActionContainer().setClickable(false);

        SectionHeaderView format = requireView().findViewById(R.id.headerPrefFormat);
        format.setTitle(R.string.hc_pref_format_title);

        SectionHeaderView price = requireView().findViewById(R.id.headerPrefPrice);
        price.setTitle(R.string.hc_pref_price_title);
        price.setSubtitle(R.string.hc_pref_price_hint);

        SectionHeaderView cities = requireView().findViewById(R.id.headerPrefCities);
        cities.setTitle(R.string.hc_pref_cities_title);
        cities.setSubtitle(R.string.hc_pref_cities_hint);

        SectionHeaderView days = requireView().findViewById(R.id.headerPrefDays);
        days.setTitle(R.string.hc_pref_days_title);
        days.setSubtitle(R.string.hc_pref_days_hint);
    }

    private void setupFormat() {
        formatGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (binding || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipPrefFormatOnsite) {
                viewModel.setFormat(PreferencesForm.Format.ONSITE);
            } else if (id == R.id.chipPrefFormatOnline) {
                viewModel.setFormat(PreferencesForm.Format.ONLINE);
            } else {
                viewModel.setFormat(PreferencesForm.Format.ANY);
            }
        });
    }

    private void setupPrice() {
        priceGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (binding || checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipPrefPriceFree) {
                viewModel.setPriceMode(PreferencesForm.PriceMode.FREE_ONLY);
            } else if (id == R.id.chipPrefPricePaid) {
                viewModel.setPriceMode(PreferencesForm.PriceMode.PAID_ONLY);
            } else {
                viewModel.setPriceMode(PreferencesForm.PriceMode.ANY);
            }
        });
    }

    private void setupBudget() {
        budgetView.setOnAmountChangedListener(amount -> {
            if (!binding) {
                viewModel.setMaxBudget(amount);
            }
        });
    }

    private void setupDays() {
        dayGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (binding) {
                return;
            }
            Set<PreferencesForm.DayPart> days = new LinkedHashSet<>();
            if (checkedIds.contains(R.id.chipPrefWeekday)) {
                days.add(PreferencesForm.DayPart.WEEKDAY);
            }
            if (checkedIds.contains(R.id.chipPrefWeekend)) {
                days.add(PreferencesForm.DayPart.WEEKEND);
            }
            viewModel.setDays(days);
        });
    }

    private void setupStates() {
        saveButton.setOnClickListener(v -> save());
        resetButton.setOnClickListener(v -> {
            viewModel.reset();
            Snackbar.make(requireView(), R.string.hc_pref_reset_done, Snackbar.LENGTH_SHORT).show();
        });

        errorState.setTitle(R.string.hc_pref_load_error);
        errorState.setMessage(R.string.hc_error_default);
        errorState.setOnRetryClickListener(v -> viewModel.retry());
    }

    private void observe() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            skeleton.setVisibility(isLoading && firstLoad ? View.VISIBLE : View.GONE);
            scrollView.setVisibility(isLoading && firstLoad ? View.INVISIBLE : View.VISIBLE);
            if (!isLoading) {
                firstLoad = false;
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            boolean failed = Boolean.TRUE.equals(error);
            errorState.setVisibility(failed ? View.VISIBLE : View.GONE);
            scrollView.setVisibility(failed ? View.GONE : View.VISIBLE);
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), this::bindCategories);
        viewModel.getHiddenCategories().observe(getViewLifecycleOwner(), this::bindHiddenNote);
        viewModel.getForm().observe(getViewLifecycleOwner(), this::render);

        viewModel.getSaving().observe(getViewLifecycleOwner(), saving -> {
            boolean isSaving = Boolean.TRUE.equals(saving);
            progress.setVisibility(isSaving ? View.VISIBLE : View.GONE);
            saveButton.setEnabled(!isSaving);
            resetButton.setEnabled(!isSaving);
            saveButton.setText(isSaving ? R.string.hc_pref_saving : R.string.hc_pref_save);
            setEditable(!isSaving);
        });

        viewModel.getSaveResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null || result == PreferencesViewModel.RESULT_NONE) {
                return;
            }
            if (result == PreferencesViewModel.RESULT_SUCCESS) {
                Snackbar.make(requireView(), R.string.hc_pref_saved, Snackbar.LENGTH_SHORT).show();
            } else if (result == PreferencesViewModel.RESULT_CONFLICT) {
                showConflictDialog();
            } else {
                Snackbar.make(requireView(), R.string.hc_pref_save_error,
                        Snackbar.LENGTH_LONG).show();
            }
            viewModel.consumeSaveResult();
        });
    }

    private void bindCategories(List<EventCategory> categories) {
        if (categories == null) {
            return;
        }
        List<EventCategory> hidden = viewModel.getHiddenCategories().getValue();
        List<EventCategory> all = new ArrayList<>(categories);
        if (hidden != null) {
            all.addAll(hidden);
        }

        categoryGroup.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (EventCategory category : all) {
            boolean isHidden = hidden != null && hidden.contains(category);
            Chip chip = (Chip) inflater.inflate(R.layout.view_ds_filter_chip, categoryGroup,
                    false);
            chip.setId(View.generateViewId());
            chip.setText(EventCategoryUi.label(requireContext(), category));
            chip.setChipIconResource(EventCategoryUi.iconRes(category));
            chip.setChipIconVisible(true);
            chip.setChipIconSize(dp(16));
            chip.setTag(category);
            if (isHidden) {
                chip.setEnabled(false);
                chip.setAlpha(0.45f);
                chip.setPaintFlags(chip.getPaintFlags()
                        | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                chip.setOnCheckedChangeListener((button, checked) -> {
                    if (binding) {
                        return;
                    }
                    if (!viewModel.toggleCategory(category)) {
                        binding = true;
                        chip.setChecked(false);
                        binding = false;
                        Snackbar.make(requireView(), R.string.hc_pref_categories_limit,
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
            categoryGroup.addView(chip);
        }
        PreferencesForm form = viewModel.getForm().getValue();
        if (form != null) {
            render(form);
        }
    }

    private void bindHiddenNote(List<EventCategory> hidden) {
        if (hidden == null || hidden.isEmpty()) {
            hiddenNote.setVisibility(View.GONE);
            return;
        }
        StringBuilder names = new StringBuilder();
        for (EventCategory category : hidden) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(EventCategoryUi.label(requireContext(), category));
        }
        hiddenNote.setText(getString(R.string.hc_pref_hidden_note, names.toString()));
        hiddenNote.setVisibility(View.VISIBLE);
    }

    private void buildCityChips() {
        cityGroup.removeAllViews();
        String[] cities = getResources().getStringArray(R.array.hc_cities);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 1; i < cities.length; i++) {
            final String city = cities[i];
            Chip chip = (Chip) inflater.inflate(R.layout.view_ds_filter_chip, cityGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(city);
            chip.setTag(city);
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (binding) {
                    return;
                }
                if (!viewModel.toggleCity(city)) {
                    binding = true;
                    chip.setChecked(false);
                    binding = false;
                    Snackbar.make(requireView(), R.string.hc_pref_cities_limit,
                            Snackbar.LENGTH_SHORT).show();
                }
            });
            cityGroup.addView(chip);
        }
    }

    private void render(PreferencesForm form) {
        if (form == null) {
            return;
        }
        if (cityGroup.getChildCount() == 0) {
            buildCityChips();
        }
        binding = true;
        try {
            categoryHeader.setActionText(getString(R.string.hc_pref_categories_count,
                    form.categoryIds.size()));
            for (int i = 0; i < categoryGroup.getChildCount(); i++) {
                View child = categoryGroup.getChildAt(i);
                if (child instanceof Chip && child.getTag() instanceof EventCategory) {
                    EventCategory category = (EventCategory) child.getTag();
                    ((Chip) child).setChecked(
                            form.categoryIds.contains(EventCategoryUi.id(category)));
                }
            }

            formatGroup.check(formatChipId(form.format));
            priceGroup.check(priceChipId(form.priceMode));

            boolean showBudget = form.priceMode != PreferencesForm.PriceMode.FREE_ONLY;
            budgetView.setVisibility(showBudget ? View.VISIBLE : View.GONE);
            budgetView.setAmount(form.maxBudget);

            for (int i = 0; i < cityGroup.getChildCount(); i++) {
                View child = cityGroup.getChildAt(i);
                if (child instanceof Chip && child.getTag() instanceof String) {
                    ((Chip) child).setChecked(form.cities.contains((String) child.getTag()));
                }
            }

            chipWeekday.setChecked(form.days.contains(PreferencesForm.DayPart.WEEKDAY));
            chipWeekend.setChecked(form.days.contains(PreferencesForm.DayPart.WEEKEND));
        } finally {
            binding = false;
        }
    }

    private int formatChipId(PreferencesForm.Format format) {
        if (format == PreferencesForm.Format.ONSITE) {
            return R.id.chipPrefFormatOnsite;
        }
        if (format == PreferencesForm.Format.ONLINE) {
            return R.id.chipPrefFormatOnline;
        }
        return R.id.chipPrefFormatAny;
    }

    private int priceChipId(PreferencesForm.PriceMode mode) {
        if (mode == PreferencesForm.PriceMode.FREE_ONLY) {
            return R.id.chipPrefPriceFree;
        }
        if (mode == PreferencesForm.PriceMode.PAID_ONLY) {
            return R.id.chipPrefPricePaid;
        }
        return R.id.chipPrefPriceAny;
    }

    private void setEditable(boolean enabled) {
        for (ChipGroup group : new ChipGroup[]{categoryGroup, formatGroup, priceGroup, cityGroup,
                dayGroup}) {
            group.setEnabled(enabled);
            for (int i = 0; i < group.getChildCount(); i++) {
                group.getChildAt(i).setEnabled(enabled && !isHiddenCategoryChip(group, i));
            }
        }
        budgetView.setEnabledState(enabled);
        budgetView.setAlpha(enabled ? 1f : 0.5f);
    }

    private boolean isHiddenCategoryChip(ChipGroup group, int index) {
        if (group != categoryGroup) {
            return false;
        }
        List<EventCategory> hidden = viewModel.getHiddenCategories().getValue();
        View child = group.getChildAt(index);
        return hidden != null && child != null && hidden.contains(child.getTag());
    }

    private void save() {
        PreferencesForm form = viewModel.getForm().getValue();
        if (form == null) {
            return;
        }
        if (form.priceMode != PreferencesForm.PriceMode.FREE_ONLY) {
            if (!budgetView.isValid()) {
                budgetView.setError(getString(R.string.hc_budget_invalid));
                return;
            }
            budgetView.setError(null);
            viewModel.setMaxBudget(budgetView.getAmount());
        }
        viewModel.save();
    }

    private void showConflictDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.hc_pref_conflict_title)
                .setMessage(R.string.hc_pref_conflict_message)
                .setPositiveButton(R.string.hc_pref_conflict_use_latest,
                        (dialog, which) -> viewModel.reloadLatest())
                .setNegativeButton(R.string.hc_pref_conflict_keep_mine,
                        (dialog, which) -> viewModel.forceSave())
                .show();
    }

    private void skip() {
        Toast.makeText(requireContext(), R.string.hc_pref_skipped, Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).popBackStack();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
