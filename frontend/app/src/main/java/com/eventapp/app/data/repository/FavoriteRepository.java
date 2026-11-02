package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.data.remote.api.FavoriteApi;
import com.eventapp.app.data.remote.dto.favorite.FavoriteDtos;

import java.util.List;

/** Yêu thích (FR-U06). */
public class FavoriteRepository {

    private final FavoriteApi api;

    public FavoriteRepository(@NonNull FavoriteApi api) {
        this.api = api;
    }

    public void add(long eventId, @NonNull ApiCallback<FavoriteDtos.ToggleResult> callback) {
        ApiCallback.enqueue(api.add(eventId), callback);
    }

    public void remove(long eventId, @NonNull ApiCallback<FavoriteDtos.ToggleResult> callback) {
        ApiCallback.enqueue(api.remove(eventId), callback);
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
