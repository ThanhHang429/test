package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.data.remote.api.FavoriteApi;
import com.eventapp.app.data.remote.dto.favorite.FavoriteDtos;
import com.google.gson.JsonElement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Yêu thích (FR-U06). */
public class FavoriteRepository {

    /** Nghe thay đổi yêu thích để các màn đang mở cập nhật ngay, không phải tải lại. */
    public interface FavoriteListener {
        void onFavoriteChanged(long eventId, boolean favorite);
    }

    private final FavoriteApi api;
    private final List<FavoriteListener> listeners = new CopyOnWriteArrayList<>();

    public FavoriteRepository(@NonNull FavoriteApi api) {
        this.api = api;
    }

    public void addListener(@NonNull FavoriteListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(@NonNull FavoriteListener listener) {
        listeners.remove(listener);
    }

    public void add(long eventId, @NonNull ApiCallback<FavoriteDtos.ToggleResult> callback) {
        ApiCallback.enqueue(api.add(eventId), notifyOnSuccess(callback));
    }

    public void remove(long eventId, @NonNull ApiCallback<FavoriteDtos.ToggleResult> callback) {
        ApiCallback.enqueue(api.remove(eventId), notifyOnSuccess(callback));
    }

    private ApiCallback<FavoriteDtos.ToggleResult> notifyOnSuccess(
            @NonNull ApiCallback<FavoriteDtos.ToggleResult> callback) {
        return new ApiCallback.Delegate<FavoriteDtos.ToggleResult>(callback) {
            @Override
            public void onSuccess(@Nullable FavoriteDtos.ToggleResult data, @Nullable JsonElement meta) {
                if (data != null) {
                    for (FavoriteListener listener : listeners) {
                        listener.onFavoriteChanged(data.eventId, data.isFavorite);
                    }
                }
                callback.onSuccess(data, meta);
            }

            @Override
            public void onError(@NonNull com.eventapp.app.core.network.ApiException error) {
                callback.onError(error);
            }
        };
    }

    public void list(@NonNull String filter, int page, int size,
                     @NonNull ApiCallback<Paged<FavoriteDtos.Item>> callback) {
        ApiCallback.enqueue(api.list(filter, page, size),
                new ApiCallback.Delegate<List<FavoriteDtos.Item>>(callback) {
                    @Override
                    public void onSuccess(List<FavoriteDtos.Item> data, com.google.gson.JsonElement meta) {
                        callback.onSuccess(new Paged<>(data, meta), meta);
                    }

                    @Override
                    public void onError(@NonNull com.eventapp.app.core.network.ApiException error) {
                        callback.onError(error);
                    }
                });
    }
}
