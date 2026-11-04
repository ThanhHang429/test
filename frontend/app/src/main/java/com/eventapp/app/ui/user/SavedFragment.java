package com.eventapp.app.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentUserSavedBinding;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.host.UserHostActivity;
import com.eventapp.app.ui.user.adapter.EventAdapter;
import com.eventapp.app.ui.user.data.DemoContent;
import com.eventapp.app.ui.user.model.DemoEvent;

import java.util.ArrayList;
import java.util.List;

/** Tab Đã lưu: danh sách sự kiện người dùng đánh dấu. */
public class SavedFragment extends Fragment {

    private FragmentUserSavedBinding binding;
    private EventAdapter adapter;
    private String filter = "all";

    private final Runnable dataObserver = this::reload;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUserSavedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new EventAdapter(false, new EventAdapter.Listener() {
            @Override
            public void onOpen(@NonNull DemoEvent event) {
                EventDetailActivity.start(requireContext(), event.id);
            }

            @Override
            public void onSaveChanged(@NonNull DemoEvent event, boolean saved) {
                SnackPill.show(binding.getRoot(), getString(
                        saved ? R.string.user_saved_toast : R.string.user_unsaved_toast), dp(112));
                reload();
            }
        });
        binding.rvSaved.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSaved.setAdapter(adapter);

        binding.chipAll.setSelected(true);
        binding.chipAll.setOnClickListener(v -> selectFilter("all"));
        binding.chipUpcoming.setOnClickListener(v -> selectFilter("upcoming"));
        binding.chipFree.setOnClickListener(v -> selectFilter("free"));
        binding.btnExplore.setOnClickListener(v -> {
            if (getActivity() instanceof UserHostActivity) {
                ((UserHostActivity) getActivity()).openTab(UserBottomNavView.TAB_DISCOVERY);
            }
        });

        reload();
    }

    private void selectFilter(String value) {
        filter = value;
        binding.chipAll.setSelected("all".equals(value));
        binding.chipUpcoming.setSelected("upcoming".equals(value));
        binding.chipFree.setSelected("free".equals(value));
        reload();
    }

    private void reload() {
        if (binding == null) {
            return;
        }
        List<DemoEvent> all = DemoContent.get().savedEvents();
        List<DemoEvent> result = new ArrayList<>();
        for (DemoEvent event : all) {
            boolean match;
            if ("upcoming".equals(filter)) {
                match = event.startAt >= System.currentTimeMillis();
            } else if ("free".equals(filter)) {
                match = event.isFree();
            } else {
                match = true;
            }
            if (match) {
                result.add(event);
            }
        }
        adapter.setItems(result);
        binding.tvSub.setText(getString(R.string.user_saved_sub, all.size()));
        binding.emptyState.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvSaved.setVisibility(result.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        DemoContent.get().addListener(dataObserver);
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
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
