package com.eventapp.app.ui.user;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentUserDiscoveryBinding;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.user.adapter.EventAdapter;
import com.eventapp.app.ui.user.data.DemoContent;
import com.eventapp.app.ui.user.model.DemoEvent;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tab Khám phá: tìm kiếm, lọc danh mục, sự kiện nổi bật và danh sách sắp diễn ra. */
public class DiscoveryFragment extends Fragment {

    private FragmentUserDiscoveryBinding binding;
    private EventAdapter featuredAdapter;
    private EventAdapter listAdapter;

    private String category = "all";
    private String query = "";

    private final Runnable dataObserver = this::refresh;

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
        binding.tvSub.setText(getString(R.string.user_discovery_sub,
                DemoContent.get().events().size()));

        EventAdapter.Listener listener = new EventAdapter.Listener() {
            @Override
            public void onOpen(@NonNull DemoEvent event) {
                EventDetailActivity.start(requireContext(), event.id);
            }

            @Override
            public void onSaveChanged(@NonNull DemoEvent event, boolean saved) {
                SnackPill.show(binding.getRoot(), getString(
                        saved ? R.string.user_saved_toast : R.string.user_unsaved_toast), dp(112));
                refresh();
            }
        };

        featuredAdapter = new EventAdapter(true, listener);
        binding.rvFeatured.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvFeatured.setAdapter(featuredAdapter);

        listAdapter = new EventAdapter(false, listener);
        binding.rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEvents.setAdapter(listAdapter);

        binding.chipAll.setSelected(true);
        bindChip(binding.chipAll, "all");
        bindChip(binding.chipMusic, "music");
        bindChip(binding.chipTech, "tech");
        bindChip(binding.chipBusiness, "business");
        bindChip(binding.chipArt, "art");
        bindChip(binding.chipFree, "free");

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                applyFilter();
            }
        });

        binding.btnFilter.setOnClickListener(v -> showFilterDialog());
        binding.btnReset.setOnClickListener(v -> resetFilters());
        binding.tvSeeAllFeatured.setOnClickListener(v -> binding.scroll.smoothScrollTo(0,
                binding.upcomingSection.getTop()));

        refresh();
    }

    private void bindChip(View chip, String key) {
        chip.setOnClickListener(v -> {
            category = key;
            binding.chipAll.setSelected(false);
            binding.chipMusic.setSelected(false);
            binding.chipTech.setSelected(false);
            binding.chipBusiness.setSelected(false);
            binding.chipArt.setSelected(false);
            binding.chipFree.setSelected(false);
            chip.setSelected(true);
            applyFilter();
        });
    }

    private void showFilterDialog() {
        final String[] keys = {"all", "music", "tech", "business", "art", "free"};
        final String[] labels = {
                getString(R.string.user_cat_all),
                getString(R.string.user_cat_music),
                getString(R.string.user_cat_tech),
                getString(R.string.user_cat_business),
                getString(R.string.user_cat_art),
                getString(R.string.user_cat_free)
        };
        int checked = 0;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(category)) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.user_filter_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    category = keys[which];
                    binding.chipAll.setSelected("all".equals(category));
                    binding.chipMusic.setSelected("music".equals(category));
                    binding.chipTech.setSelected("tech".equals(category));
                    binding.chipBusiness.setSelected("business".equals(category));
                    binding.chipArt.setSelected("art".equals(category));
                    binding.chipFree.setSelected("free".equals(category));
                    applyFilter();
                    dialog.dismiss();
                })
                .show();
    }

    private void resetFilters() {
        category = "all";
        query = "";
        binding.etSearch.setText("");
        binding.chipAll.setSelected(true);
        binding.chipMusic.setSelected(false);
        binding.chipTech.setSelected(false);
        binding.chipBusiness.setSelected(false);
        binding.chipArt.setSelected(false);
        binding.chipFree.setSelected(false);
        applyFilter();
    }

    private void refresh() {
        if (binding == null) {
            return;
        }
        binding.tvSub.setText(getString(R.string.user_discovery_sub,
                DemoContent.get().events().size()));
        applyFilter();
    }

    private void applyFilter() {
        if (binding == null) {
            return;
        }
        List<DemoEvent> result = new ArrayList<>();
        for (DemoEvent event : DemoContent.get().events()) {
            boolean matchCategory = "free".equals(category) ? event.isFree()
                    : event.matchesCategory(category);
            boolean matchQuery = query.isEmpty()
                    || event.title.toLowerCase(Locale.ROOT).contains(query)
                    || event.venue.toLowerCase(Locale.ROOT).contains(query)
                    || event.categoryLabel.toLowerCase(Locale.ROOT).contains(query)
                    || event.organizer.toLowerCase(Locale.ROOT).contains(query);
            if (matchCategory && matchQuery) {
                result.add(event);
            }
        }

        boolean simpleView = "all".equals(category) && query.isEmpty();
        binding.featuredSection.setVisibility(simpleView ? View.VISIBLE : View.GONE);
        if (simpleView) {
            featuredAdapter.setItems(DemoContent.get().featured());
        }

        listAdapter.setItems(result);
        binding.tvResultCount.setText(getString(R.string.user_event_count, result.size()));
        binding.emptyState.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
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
        String[] parts = DemoContent.USER_NAME.split(" ");
        return getString(res, parts[parts.length - 1]);
    }

    @Override
    public void onStart() {
        super.onStart();
        DemoContent.get().addListener(dataObserver);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onStop() {
        DemoContent.get().removeListener(dataObserver);
        super.onStop();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
