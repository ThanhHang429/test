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
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.data.remote.dto.favorite.FavoriteDtos;
import com.eventapp.app.data.repository.FavoriteRepository;
import com.eventapp.app.databinding.FragmentUserSavedBinding;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.host.UserHostActivity;
import com.eventapp.app.ui.mapper.EventMapper;
import com.eventapp.app.ui.model.EventUiModel;
import com.eventapp.app.ui.user.adapter.EventAdapter;

import java.util.ArrayList;
import java.util.List;

/** Tab Đã lưu: danh sách sự kiện người dùng đánh dấu (FR-U06). */
public class SavedFragment extends Fragment {

    private static final int PAGE_SIZE = 50;

    private FragmentUserSavedBinding binding;
    private EventAdapter adapter;
    private String filter = "all";
    private long loadVersion;
    private final List<EventUiModel> all = new ArrayList<>();
    private final FavoriteRepository.FavoriteListener favoriteListener =
            (eventId, favorite) -> {
                if (binding == null) {
                    return;
                }
                // Mọi thay đổi đều làm phản hồi list đang bay trở nên lỗi thời.
                loadVersion++;
                if (!favorite) {
                    // Bỏ lưu: xoá ngay tại chỗ dù tab đang ẩn hay hiện.
                    removeById(eventId);
                    return;
                }
                // Thêm mới: cần tải lại để có đủ thẻ sự kiện; khi tab ẩn thì
                // onHiddenChanged(false) sẽ tự tải lúc người dùng mở tab.
                if (!isHidden()) {
                    load();
                }
            };

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

        binding.chipAll.setSelected(true);
        binding.chipAll.setOnClickListener(v -> selectFilter("all"));
        binding.chipUpcoming.setOnClickListener(v -> selectFilter("upcoming"));
        binding.chipFree.setOnClickListener(v -> selectFilter("free"));
        binding.btnExplore.setOnClickListener(v -> {
            if (getActivity() instanceof UserHostActivity) {
                ((UserHostActivity) getActivity()).openTab(UserBottomNavView.TAB_DISCOVERY);
            }
        });

        ServiceLocator.get(FavoriteRepository.class).addListener(favoriteListener);
        load();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // Tab vừa được mở: luôn lấy danh sách mới để tránh hiển thị dữ liệu cũ
            // nếu lần đồng bộ trước bị lỗi mạng hoặc lỡ mất sự kiện.
            load();
        }
    }

    private void selectFilter(String value) {
        filter = value;
        binding.chipAll.setSelected("all".equals(value));
        binding.chipUpcoming.setSelected("upcoming".equals(value));
        binding.chipFree.setSelected("free".equals(value));
        render();
    }

    private void load() {
        final long version = ++loadVersion;
        ServiceLocator.get(FavoriteRepository.class).list("ALL", 0, PAGE_SIZE,
                new ApiCallback<Paged<FavoriteDtos.Item>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<FavoriteDtos.Item> data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        // Bỏ phản hồi cũ nếu trong lúc chờ đã có thao tác lưu/bỏ lưu.
                        if (binding == null || version != loadVersion) {
                            return;
                        }
                        all.clear();
                        if (data != null) {
                            for (FavoriteDtos.Item item : data.items) {
                                if (item != null && item.event != null) {
                                    all.add(EventMapper.fromCard(item.event));
                                }
                            }
                        }
                        render();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        if (binding == null || version != loadVersion) {
                            return;
                        }
                        all.clear();
                        render();
                        SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(112));
                    }
                });
    }

    private void removeFavorite(EventUiModel event) {
        if (!event.favorite) {
            return;
        }
        // Cập nhật ngay trên giao diện, không đợi phản hồi mạng.
        loadVersion++;
        event.favorite = false;
        removeById(event.id);
        SnackPill.show(binding.getRoot(), getString(R.string.user_unsaved_toast), dp(112));

        ServiceLocator.get(FavoriteRepository.class).remove(event.id,
                new ApiCallback<FavoriteDtos.ToggleResult>() {
                    @Override
                    public void onSuccess(@Nullable FavoriteDtos.ToggleResult data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        // Đã xoá khỏi danh sách trước đó.
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        if (binding == null) {
                            return;
                        }
                        loadVersion++;
                        event.favorite = true;
                        boolean exists = false;
                        for (EventUiModel item : all) {
                            if (item.id == event.id) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            all.add(0, event);
                        }
                        render();
                        SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(112));
                    }
                });
    }

    private void removeById(long eventId) {
        if (all.removeIf(item -> item.id == eventId)) {
            render();
        }
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
            load();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        ServiceLocator.get(FavoriteRepository.class).removeListener(favoriteListener);
        super.onDestroyView();
        binding = null;
    }
}
