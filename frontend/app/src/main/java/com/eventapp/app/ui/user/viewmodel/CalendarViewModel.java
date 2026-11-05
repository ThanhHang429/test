package com.eventapp.app.ui.user.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.favorite.FavoriteDtos;
import com.eventapp.app.data.repository.FavoriteRepository;
import com.eventapp.app.ui.mapper.EventMapper;
import com.eventapp.app.ui.model.EventUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/** Sự kiện đã lưu dùng cho tab Lịch (FR-U06). */
public class CalendarViewModel extends ViewModel {

    private static final int PAGE_SIZE = 50;

    private final FavoriteRepository repository = ServiceLocator.get(FavoriteRepository.class);

    private final MutableLiveData<List<EventUiModel>> savedEvents =
            new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<EventUiModel>> getSavedEvents() {
        return savedEvents;
    }

    public void loadSaved() {
        repository.list("UPCOMING", 0, PAGE_SIZE,
                new ApiCallback<Paged<FavoriteDtos.Item>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<FavoriteDtos.Item> data,
                                          @Nullable JsonElement meta) {
                        List<EventUiModel> result = new ArrayList<>();
                        if (data != null) {
                            for (FavoriteDtos.Item item : data.items) {
                                if (item != null && item.event != null) {
                                    result.add(EventMapper.fromCard(item.event));
                                }
                            }
                        }
                        savedEvents.setValue(result);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        // Không có mạng thì vẫn hiển thị vé đã mua.
                        savedEvents.setValue(new ArrayList<>());
                    }
                });
    }
}
