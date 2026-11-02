package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.data.remote.api.PreferenceApi;
import com.eventapp.app.data.remote.dto.preference.PreferenceDtos;

/** Sở thích người dùng (FR-U03). */
public class UserRepository {

    private final PreferenceApi api;

    public UserRepository(@NonNull PreferenceApi api) {
        this.api = api;
    }

    public void preferences(@NonNull ApiCallback<PreferenceDtos.Preference> callback) {
        ApiCallback.enqueue(api.get(), callback);
    }

    public void savePreferences(@NonNull PreferenceDtos.UpdateRequest request,
                                @NonNull ApiCallback<PreferenceDtos.Preference> callback) {
        ApiCallback.enqueue(api.put(request), callback);
    }

    public void resetPreferences(@NonNull ApiCallback<PreferenceDtos.Preference> callback) {
        ApiCallback.enqueue(api.reset(), callback);
    }
}
