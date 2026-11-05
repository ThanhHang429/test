package com.eventapp.app.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentUserSavedBinding;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.host.UserHostActivity;
import com.eventapp.app.ui.model.EventUiModel;
import com.eventapp.app.ui.user.adapter.EventAdapter;
import com.eventapp.app.ui.user.viewmodel.SavedViewModel;

import java.util.ArrayList;
import java.util.List;

/** Tab Đã lưu: danh sách sự kiện người dùng đánh dấu (FR-U06). */
public class SavedFragment extends Fragment {

    private FragmentUserSavedBinding binding;
    private EventAdapter adapter;
    private SavedViewModel viewModel;
    private String filter = "all";
    private final List<EventUiModel> all = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUserSavedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        NavContentInset.apply(binding.getRoot());
        adapter = new EventAdapter(false, new EventAdapter.Listener() {
            @Override
            public void onOpen(@NonNull EventUiModel event) {
                EventDetailActivity.start(requireContext(), event.id);
            }

            @Override
            public void onToggleFavorite(@NonNull EventUiModel event) {
                removeFavorite(event);
            }
        });
        binding.rvSaved.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSaved.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(SavedViewModel.class);
        observe();

        binding.chipAll.setSelected(true);
        binding.chipAll.setOnClickListener(v -> selectFilter("all"));
        binding.chipUpcoming.setOnClickListener(v -> selectFilter("upcoming"));
        binding.chipFree.setOnClickListener(v -> selectFilter("free"));
        binding.btnExplore.setOnClickListener(v -> {
            if (getActivity() instanceof UserHostActivity) {
                ((UserHostActivity) getActivity()).openTab(UserBottomNavView.TAB_DISCOVERY);
            }
        });

        viewModel.load();
    }

    private void observe() {
        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            all.clear();
            all.addAll(items);
            render();
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error == null || binding == null) {
                return;
            }
            viewModel.consumeError();
            SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(112));
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // Tab vừa được mở: luôn lấy danh sách mới để tránh hiển thị dữ liệu cũ
            // nếu lần đồng bộ trước bị lỗi mạng hoặc lỡ mất sự kiện.
            viewModel.load();
        }
    }

    private void selectFilter(String value) {
        filter = value;
        binding.chipAll.setSelected("all".equals(value));
        binding.chipUpcoming.setSelected("upcoming".equals(value));
        binding.chipFree.setSelected("free".equals(value));
        render();
    }

    private void removeFavorite(EventUiModel event) {
        if (!event.favorite) {
            return;
        }
        // ViewModel cập nhật lạc quan và tự hoàn tác nếu mạng lỗi.
        viewModel.remove(event);
        SnackPill.show(binding.getRoot(), getString(R.string.user_unsaved_toast), dp(112));
    }

    private void render() {
        if (binding == null) {
            return;
        }
        long now = System.currentTimeMillis();
        List<EventUiModel> result = new ArrayList<>();
        for (EventUiModel event : all) {
            boolean match;
            if ("upcoming".equals(filter)) {
                match = event.startAt >= now;
            } else if ("free".equals(filter)) {
                match = event.free;
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
    public void onResume() {
        super.onResume();
        // Quay lại từ màn chi tiết/thanh toán: chỉ đồng bộ khi tab đang hiển thị.
        if (!isHidden()) {
            viewModel.load();
        }
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
