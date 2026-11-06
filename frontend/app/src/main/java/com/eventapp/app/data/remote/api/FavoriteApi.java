package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.favorite.FavoriteDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Yêu thích (FR-U06). */
public interface FavoriteApi {

    @PUT("me/favorites/{eventId}")
    Call<ResponseWrapper.Envelope<FavoriteDtos.ToggleResult>> add(@Path("eventId") long eventId);

    @DELETE("me/favorites/{eventId}")
    Call<ResponseWrapper.Envelope<FavoriteDtos.ToggleResult>> remove(@Path("eventId") long eventId);

    @GET("me/favorites")
    Call<ResponseWrapper.Envelope<List<FavoriteDtos.Item>>> list(
            @Query("filter") String filter,
            @Query("page") int page,
            @Query("size") int size);
}
