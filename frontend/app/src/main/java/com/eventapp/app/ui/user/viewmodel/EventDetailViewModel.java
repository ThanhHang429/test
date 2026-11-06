package com.eventapp.app.ui.user.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;
import com.eventapp.app.data.remote.dto.favorite.FavoriteDtos;
import com.eventapp.app.data.repository.EventRepository;
import com.eventapp.app.data.repository.FavoriteRepository;
import com.eventapp.app.ui.mapper.EventMapper;
import com.eventapp.app.ui.model.EventUiModel;
import com.google.gson.JsonElement;

/** Trạng thái chi tiết sự kiện và thao tác lưu/bỏ lưu (FR-U05, FR-U06). */
public class EventDetailViewModel extends ViewModel {

    private final EventRepository events = ServiceLocator.get(EventRepository.class);
    private final FavoriteRepository favorites = ServiceLocator.get(FavoriteRepository.class);

    private final MutableLiveData<EventUiModel> event = new MutableLiveData<>();
    private final MutableLiveData<ApiException> loadError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> favorite = new MutableLiveData<>();
    private final MutableLiveData<ApiException> favoriteError = new MutableLiveData<>();

    public LiveData<EventUiModel> getEvent() {
        return event;
    }

    public LiveData<ApiException> getLoadError() {
        return loadError;
    }

    public LiveData<Boolean> getFavorite() {
        return favorite;
    }

    public LiveData<ApiException> getFavoriteError() {
        return favoriteError;
    }

    public void load(long eventId) {
        loadError.setValue(null);
        events.detail(eventId, new ApiCallback<DiscoveryDtos.EventDetail>() {
            @Override
            public void onSuccess(@Nullable DiscoveryDtos.EventDetail data,
                                  @Nullable JsonElement meta) {
                if (data == null) {
                    loadError.setValue(ApiException.malformed(null));
                    return;
                }
                event.setValue(EventMapper.fromDetail(data));
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                loadError.setValue(exception);
            }
        });
    }

    public void toggleFavorite(long eventId, boolean target) {
        favoriteError.setValue(null);
        ApiCallback<FavoriteDtos.ToggleResult> callback =
                new ApiCallback<FavoriteDtos.ToggleResult>() {
                    @Override
                    public void onSuccess(@Nullable FavoriteDtos.ToggleResult data,
                                          @Nullable JsonElement meta) {
                        boolean saved = data != null
                                ? data.isFavorite
                                : target;
                        favorite.setValue(saved);
                        EventUiModel current = event.getValue();
                        if (current != null) {
                            current.favorite = saved;
                            event.setValue(current);
                        }
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        favoriteError.setValue(exception);
                    }
                };
        if (target) {
            favorites.add(eventId, callback);
        } else {
            favorites.remove(eventId, callback);
        }
    }

    public void consumeFavorite() {
        favorite.setValue(null);
    }

    public void consumeFavoriteError() {
        favoriteError.setValue(null);
    }
}
