package com.eventapp.app.core.network;

import androidx.annotation.NonNull;

import com.eventapp.app.BuildConfig;
import com.eventapp.app.core.common.AppConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Nơi duy nhất dựng Retrofit cho toàn app.
 *
 * <p>{@code BASE_URL} lấy từ {@code BuildConfig} (khai báo trong {@code app/build.gradle}), đã có
 * sẵn hậu tố {@code /api/v1/} nên các Api interface chỉ khai báo đường dẫn tương đối.</p>
 */
public final class ApiClient {

    private static volatile Retrofit retrofit;
    private static volatile Gson gson;

    private ApiClient() {
    }

    @NonNull
    public static Retrofit retrofit() {
        Retrofit local = retrofit;
        if (local == null) {
            synchronized (ApiClient.class) {
                local = retrofit;
                if (local == null) {
                    local = build();
                    retrofit = local;
                }
            }
        }
        return local;
    }

    @NonNull
    public static Gson gson() {
        Gson local = gson;
        if (local == null) {
            synchronized (ApiClient.class) {
                local = gson;
                if (local == null) {
                    local = new GsonBuilder().create();
                    gson = local;
                }
            }
        }
        return local;
    }

    @NonNull
    public static <T> T create(@NonNull Class<T> service) {
        return retrofit().create(service);
    }

    @NonNull
    private static Retrofit build() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(AppConstants.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(AppConstants.READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(AppConstants.WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .addInterceptor(new RequestIdProvider())
                .addInterceptor(new AuthInterceptor())
                .addInterceptor(HttpLogger.create())
                .build();

        String baseUrl = BuildConfig.BASE_URL;
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson()))
                .build();
    }
}
