package com.eventapp.app;

import android.app.Application;

import androidx.annotation.NonNull;

import com.eventapp.app.core.common.AppLogger;
import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiClient;

/**
 * Application: chỉ khởi tạo những gì thật sự cần cho frame đầu tiên.
 *
 * <p>Trước đây {@link ApiClient#retrofit()} được gọi ngay trên main thread, kéo theo việc nạp
 * toàn bộ OkHttp/Retrofit/Gson trước khi Activity đầu tiên kịp vẽ. Giờ việc dựng Retrofit chạy ở
 * luồng nền để "làm ấm" sẵn — request đầu tiên không phải chờ khởi tạo mà màn hình cũng không bị
 * chặn.</p>
 */
public class EventApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ServiceLocator.reset();
        AppLogger.d("EventApp khởi tạo · BASE_URL=" + BuildConfig.BASE_URL);
        warmUpNetwork();
    }

    /**
     * Dựng Retrofit ở luồng nền.
     *
     * <p>Không dùng thread pool chung của app vì đây là tác vụ một lần, và cũng không chờ nó xong:
     * nếu người dùng vào màn cần mạng trước khi warm-up hoàn tất thì {@link ApiClient#retrofit()}
     * vẫn tự dựng (có double-check lock) — chậm nhất chỉ bằng hành vi cũ.</p>
     */
    private void warmUpNetwork() {
        Thread warmUp = new Thread(() -> {
            try {
                ApiClient.retrofit();
            } catch (RuntimeException error) {
                // Cấu hình BASE_URL sai sẽ lộ ra ở request đầu tiên với thông điệp rõ ràng.
                AppLogger.e("Không dựng được Retrofit khi warm-up", error);
            }
        }, "api-warmup");
        warmUp.setPriority(Thread.MIN_PRIORITY);
        warmUp.start();
    }

    @NonNull
    public static EventApp get(@NonNull android.content.Context context) {
        return (EventApp) context.getApplicationContext();
    }
}
