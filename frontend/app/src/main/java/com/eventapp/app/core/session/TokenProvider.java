package com.eventapp.app.core.session;

import androidx.annotation.Nullable;

/** Nguồn cấp access token cho {@code AuthInterceptor}. */
public interface TokenProvider {

    @Nullable
    String getToken();
}
