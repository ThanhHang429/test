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

/** Danh sách sự kiện đã lưu và thao tác bỏ lưu (FR-U06). */
public class SavedViewModel extends ViewModel {

    private static final int PAGE_SIZE = 50;

    private final FavoriteRepository repository = ServiceLocator.get(FavoriteRepository.class);

    private final MutableLiveData<List<EventUiModel>> items =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();

    private final FavoriteRepository.FavoriteListener favoriteListener =
            (eventId, favorite) -> {
                if (!favorite) {
                    removeById(eventId);
                } else {
                    load();
                }
            };

    private long loadVersion;

    public SavedViewModel() {
        repository.addListener(favoriteListener);
    }

    public LiveData<List<EventUiModel>> getItems() {
        return items;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public void load() {
        final long version = ++loadVersion;
        error.setValue(null);
        repository.list("ALL", 0, PAGE_SIZE,
                new ApiCallback<Paged<FavoriteDtos.Item>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<FavoriteDtos.Item> data,
                                          @Nullable JsonElement meta) {
                        if (version != loadVersion) {
                            return;
                        }
                        List<EventUiModel> result = new ArrayList<>();
                        if (data != null) {
                            for (FavoriteDtos.Item item : data.items) {
                                if (item != null && item.event != null) {
                                    result.add(EventMapper.fromCard(item.event));
                                }
                            }
                        }
                        items.setValue(result);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        if (version != loadVersion) {
                            return;
                        }
                        items.setValue(new ArrayList<>());
                        error.setValue(exception);
                    }
                });
    }

    public void remove(@NonNull EventUiModel event) {
        List<EventUiModel> current = items.getValue();
        if (current == null) {
            return;
        }
        int index = indexOf(current, event.id);
        if (index < 0) {
            return;
        }
        List<EventUiModel> optimistic = new ArrayList<>(current);
        optimistic.remove(index);
        items.setValue(optimistic);
        loadVersion++;

        repository.remove(event.id, new ApiCallback<FavoriteDtos.ToggleResult>() {
            @Override
            public void onSuccess(@Nullable FavoriteDtos.ToggleResult data,
                                  @Nullable JsonElement meta) {
                // Đã xoá khỏi danh sách trước đó.
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                List<EventUiModel> rollback = new ArrayList<>(
                        items.getValue() == null ? new ArrayList<>() : items.getValue());
                event.favorite = true;
                if (indexOf(rollback, event.id) < 0) {
                    rollback.add(Math.min(index, rollback.size()), event);
                }
                items.setValue(rollback);
                error.setValue(exception);
            }
        });
    }

    public void consumeError() {
        error.setValue(null);
    }

    private void removeById(long eventId) {
        List<EventUiModel> current = items.getValue();
        if (current == null) {
            return;
        }
        List<EventUiModel> result = new ArrayList<>(current);
        if (result.removeIf(item -> item.id == eventId)) {
            items.setValue(result);
        }
    }

    private static int indexOf(@NonNull List<EventUiModel> source, long eventId) {
        for (int i = 0; i < source.size(); i++) {
            if (source.get(i).id == eventId) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onCleared() {
        repository.removeListener(favoriteListener);
        super.onCleared();
    }
}
