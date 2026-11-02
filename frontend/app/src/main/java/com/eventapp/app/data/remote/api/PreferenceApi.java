package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.preference.PreferenceDtos;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;

/** Sở thích người dùng (FR-U03). */
public interface PreferenceApi {

    @GET("me/preferences")
    Call<ResponseWrapper.Envelope<PreferenceDtos.Preference>> get();

    @PUT("me/preferences")
    Call<ResponseWrapper.Envelope<PreferenceDtos.Preference>> put(
            @Body PreferenceDtos.UpdateRequest request);

    @DELETE("me/preferences")
    Call<ResponseWrapper.Envelope<PreferenceDtos.Preference>> reset();
}
