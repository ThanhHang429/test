package com.eventapp.app.data.di;

import androidx.annotation.NonNull;

import com.eventapp.app.core.network.ApiClient;
import com.eventapp.app.data.remote.api.AdminCategoryApi;
import com.eventapp.app.data.remote.api.AuthApi;
import com.eventapp.app.data.remote.api.CategoryApi;
import com.eventapp.app.data.remote.api.EventApi;
import com.eventapp.app.data.remote.api.FavoriteApi;
import com.eventapp.app.data.remote.api.IntegrationApi;
import com.eventapp.app.data.remote.api.ModerationApi;
import com.eventapp.app.data.remote.api.NotificationApi;
import com.eventapp.app.data.remote.api.OrganizerApi;
import com.eventapp.app.data.remote.api.OrderApi;
import com.eventapp.app.data.remote.api.PreferenceApi;
import com.eventapp.app.data.remote.api.RecommendationApi;
import com.eventapp.app.data.remote.api.ReviewApi;
import com.eventapp.app.data.remote.api.TicketTypeApi;
import com.eventapp.app.data.remote.api.WalletApi;
import com.eventapp.app.data.repository.OrderRepository;
import com.eventapp.app.data.repository.AiRepository;
import com.eventapp.app.data.repository.AuthRepository;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.data.repository.EventRepository;
import com.eventapp.app.data.repository.FavoriteRepository;
import com.eventapp.app.data.repository.NotificationRepository;
import com.eventapp.app.data.repository.OrganizerRepository;
import com.eventapp.app.data.repository.ReviewRepository;
import com.eventapp.app.data.repository.SystemConfigRepository;
import com.eventapp.app.data.repository.TicketTypeRepository;
import com.eventapp.app.data.repository.UserRepository;
import com.eventapp.app.data.repository.WalletRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nơi giữ các repository dùng chung.
 *
 * <p>ViewModel đang được tạo bằng {@code new ViewModelProvider(this).get(X.class)} nên phải có
 * constructor rỗng. Thay vì đổi sang ViewModelFactory ở hàng trăm màn hình, ViewModel lấy
 * repository qua {@code ServiceLocator.get(...)}.</p>
 *
 * <p>Mỗi repository là singleton vì chúng không giữ state riêng — state nằm ở ViewModel.</p>
 */
public final class ServiceLocator {

    private static final Map<Class<?>, Object> INSTANCES = new ConcurrentHashMap<>();

    private ServiceLocator() {
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> T get(@NonNull Class<T> type) {
        Object existing = INSTANCES.get(type);
        if (existing != null) {
            return (T) existing;
        }
        synchronized (ServiceLocator.class) {
            existing = INSTANCES.get(type);
            if (existing == null) {
                existing = create(type);
                INSTANCES.put(type, existing);
            }
            return (T) existing;
        }
    }

    /** Xoá cache — dùng khi đăng xuất nếu sau này repository có state. */
    public static void reset() {
        INSTANCES.clear();
    }

    @NonNull
    private static Object create(@NonNull Class<?> type) {
        if (type == AuthRepository.class) {
            return new AuthRepository(ApiClient.create(AuthApi.class));
        }
        if (type == CategoryRepository.class) {
            return new CategoryRepository(
                    ApiClient.create(CategoryApi.class),
                    ApiClient.create(AdminCategoryApi.class));
        }
        if (type == EventRepository.class) {
            return new EventRepository(ApiClient.create(EventApi.class));
        }
        if (type == FavoriteRepository.class) {
            return new FavoriteRepository(ApiClient.create(FavoriteApi.class));
        }
        if (type == ReviewRepository.class) {
            return new ReviewRepository(
                    ApiClient.create(ReviewApi.class),
                    ApiClient.create(ModerationApi.class));
        }
        if (type == UserRepository.class) {
            return new UserRepository(ApiClient.create(PreferenceApi.class));
        }
        if (type == AiRepository.class) {
            return new AiRepository(ApiClient.create(RecommendationApi.class));
        }
        if (type == OrganizerRepository.class) {
            return new OrganizerRepository(ApiClient.create(OrganizerApi.class));
        }
        if (type == NotificationRepository.class) {
            return new NotificationRepository(ApiClient.create(NotificationApi.class));
        }
        if (type == SystemConfigRepository.class) {
            return new SystemConfigRepository(ApiClient.create(IntegrationApi.class));
        }
        if (type == WalletRepository.class) {
            return new WalletRepository(ApiClient.create(WalletApi.class));
        }
        if (type == TicketTypeRepository.class) {
            return new TicketTypeRepository(ApiClient.create(TicketTypeApi.class));
        }
        if (type == com.eventapp.app.data.repository.TicketRepository.class) {
            return new com.eventapp.app.data.repository.TicketRepository(
                    ApiClient.create(com.eventapp.app.data.remote.api.TicketApi.class));
        }
        if (type == OrderRepository.class) {
            return new OrderRepository(
                    ApiClient.create(OrderApi.class),
                    ApiClient.create(com.eventapp.app.data.remote.api.PaymentApi.class));
        }
        if (type == com.eventapp.app.data.repository.PromotionRepository.class) {
            return new com.eventapp.app.data.repository.PromotionRepository(
                    ApiClient.create(com.eventapp.app.data.remote.api.PromotionApi.class));
        }
        if (type == com.eventapp.app.data.repository.RefundRepository.class) {
            return new com.eventapp.app.data.repository.RefundRepository(
                    ApiClient.create(com.eventapp.app.data.remote.api.RefundApi.class));
        }
        if (type == com.eventapp.app.data.repository.FinanceRepository.class) {
            return new com.eventapp.app.data.repository.FinanceRepository(
                    ApiClient.create(com.eventapp.app.data.remote.api.ReportApi.class));
        }
        throw new IllegalStateException("Repository chưa được đăng ký: " + type.getName());
    }
}
