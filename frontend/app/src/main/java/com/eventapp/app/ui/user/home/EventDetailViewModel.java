package com.eventapp.app.ui.user.home;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.R;
import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;
import com.eventapp.app.data.remote.dto.favorite.FavoriteDtos;
import com.eventapp.app.data.remote.dto.review.ReviewDtos;
import com.eventapp.app.data.repository.EventRepository;
import com.eventapp.app.data.repository.FavoriteRepository;
import com.eventapp.app.data.repository.ReviewRepository;
import com.eventapp.app.ui.mapper.EventMapper;
import com.eventapp.app.ui.mapper.ReviewMapper;
import com.eventapp.app.ui.model.EventCategoryUi;
import com.eventapp.app.ui.model.EventUiModel;
import com.eventapp.app.ui.model.ReviewUiModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * State holder cho FR-U05.
 *
 * <p>Dữ liệu lấy từ {@code GET /events/{id}} và {@code GET /events/{id}/reviews}. Chỉ những hành
 * động thật sự có API mới được bật: yêu thích và viết đánh giá. Mua vé / đăng ký / xem vé đã được
 * ẩn khỏi giao diện vì module thanh toán chưa nằm trong phạm vi backend hiện tại.</p>
 */
public class EventDetailViewModel extends ViewModel {

    public enum PrimaryAction {
        BUY_TICKET,
        REGISTER_FREE,
        VIEW_MY_TICKET,
        WRITE_REVIEW,
        NONE
    }

    public enum TicketSaleStatus {
        NOT_STARTED,
        ON_SALE,
        ENDED,
        SOLD_OUT
    }

    /** Image placeholder metadata; real screens will load the API image URL with Glide. */
    public static class GalleryItem {
        public final String label;
        @DrawableRes
        public final int iconRes;
        public final int index;
        @Nullable
        public final String url;

        public GalleryItem(String label, @DrawableRes int iconRes, int index) {
            this(label, iconRes, index, null);
        }

        public GalleryItem(String label, @DrawableRes int iconRes, int index, @Nullable String url) {
            this.label = label;
            this.iconRes = iconRes;
            this.index = index;
            this.url = url;
        }
    }

    public static class TicketOption {
        public final long id;
        public final String name;
        public final String description;
        public final long price;
        public final int remaining;
        public final long saleStart;
        public final long saleEnd;
        public final TicketSaleStatus status;

        public TicketOption(long id, String name, String description, long price, int remaining,
                            long saleStart, long saleEnd, TicketSaleStatus status) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.remaining = remaining;
            this.saleStart = saleStart;
            this.saleEnd = saleEnd;
            this.status = status;
        }
    }

    public static class DetailState {
        public EventUiModel event;
        public List<GalleryItem> gallery = new ArrayList<>();
        public List<TicketOption> tickets = new ArrayList<>();
        public List<ReviewUiModel> reviews = new ArrayList<>();
        public int[] reviewDistribution = new int[5];
        public float reviewAverage;
        public boolean cancelled;
        public boolean completed;
        public boolean offline;
        public boolean ticketsError;
        public boolean reviewsError;
        public boolean canReview;
        public long asOf;
        public int selectedTicketIndex;
        public PrimaryAction primaryAction = PrimaryAction.NONE;
        public String disabledReason;
        /** Nhãn trạng thái ngắn (Sắp mở bán / Đang mở bán / Hết vé...) do backend trả. */
        public String statusText;
        public String conditions;
        public String cancelPolicy;
        public boolean onlineLinkAvailable;
        public long organizerEventCount;
    }

    private static final int REVIEW_PAGE_SIZE = 20;

    private final EventRepository eventRepository = ServiceLocator.get(EventRepository.class);
    private final FavoriteRepository favoriteRepository = ServiceLocator.get(FavoriteRepository.class);
    private final ReviewRepository reviewRepository = ServiceLocator.get(ReviewRepository.class);

    private final MutableLiveData<DetailState> state = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> unavailable = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    private long eventId = -1L;
    private DetailState current;
    private boolean favoriteBusy;

    public LiveData<DetailState> getState() {
        return state;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<Boolean> getUnavailable() {
        return unavailable;
    }

    /** Thông báo ngắn cho Snackbar (lỗi thao tác, kết quả yêu thích...). */
    public LiveData<String> getMessage() {
        return message;
    }

    public void load(long eventId) {
        this.eventId = eventId;
        loadInternal(true);
    }

    public void refresh() {
        loadInternal(false);
    }

    public void retry() {
        error.setValue(false);
        loadInternal(true);
    }

    public void retryTickets() {
        loadInternal(false);
    }

    public void retryReviews() {
        if (current != null) {
            current.reviewsError = false;
            state.setValue(current);
        }
        loadReviews();
    }

    public void selectTicket(int index) {
        if (current == null || index < 0 || index >= current.tickets.size()) {
            return;
        }
        current.selectedTicketIndex = index;
        state.setValue(current);
    }

    /** Bật/tắt yêu thích qua đúng API {@code PUT/DELETE /me/favorites/{eventId}}. */
    public void toggleFavorite() {
        if (current == null || current.event == null || favoriteBusy) {
            return;
        }
        favoriteBusy = true;
        final boolean wasFavorite = current.event.favorite;
        ApiCallback<FavoriteDtos.ToggleResult> callback = new ApiCallback<FavoriteDtos.ToggleResult>() {
            @Override
            public void onSuccess(@Nullable FavoriteDtos.ToggleResult data,
                                  @Nullable com.google.gson.JsonElement meta) {
                favoriteBusy = false;
                if (current == null || current.event == null) {
                    return;
                }
                boolean value = data == null ? !wasFavorite : data.isFavorite;
                current.event.favorite = value;
                state.setValue(current);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                favoriteBusy = false;
                message.setValue(favoriteMessage(exception));
            }
        };
        if (wasFavorite) {
            favoriteRepository.remove(eventId, callback);
        } else {
            favoriteRepository.add(eventId, callback);
        }
    }

    private void loadInternal(boolean showSkeleton) {
        if (showSkeleton) {
            loading.setValue(true);
        }
        error.setValue(false);
        unavailable.setValue(false);

        if (eventId <= 0L) {
            loading.setValue(false);
            unavailable.setValue(true);
            return;
        }

        eventRepository.detail(eventId, new ApiCallback<DiscoveryDtos.EventDetail>() {
            @Override
            public void onSuccess(@Nullable DiscoveryDtos.EventDetail data,
                                  @Nullable com.google.gson.JsonElement meta) {
                loading.setValue(false);
                if (data == null) {
                    unavailable.setValue(true);
                    return;
                }
                current = buildState(data);
                state.setValue(current);
                loadReviews();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                loading.setValue(false);
                if (exception.getHttpCode() == 404) {
                    unavailable.setValue(true);
                } else {
                    error.setValue(true);
                    message.setValue(exception.getUserMessage());
                }
            }
        });
    }

    private void loadReviews() {
        if (current == null) {
            return;
        }
        reviewRepository.list(eventId, "NEWEST", 0, REVIEW_PAGE_SIZE,
                new ApiCallback<Paged<ReviewDtos.Review>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<ReviewDtos.Review> data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        if (current == null) {
                            return;
                        }
                        List<ReviewUiModel> mapped = new ArrayList<>();
                        if (data != null) {
                            for (ReviewDtos.Review review : data.items) {
                                mapped.add(ReviewMapper.fromPublic(review));
                            }
                        }
                        current.reviews = mapped;
                        current.reviewsError = false;
                        if (current.reviewAverage <= 0f && !mapped.isEmpty()) {
                            float sum = 0f;
                            for (ReviewUiModel review : mapped) {
                                sum += review.rating;
                            }
                            current.reviewAverage = sum / mapped.size();
                        }
                        state.setValue(current);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        if (current != null) {
                            current.reviewsError = true;
                            state.setValue(current);
                        }
                    }
                });
    }

    @NonNull
    private DetailState buildState(@NonNull DiscoveryDtos.EventDetail dto) {
        DetailState detail = new DetailState();
        detail.event = EventMapper.fromDetail(dto);
        detail.asOf = System.currentTimeMillis();
        detail.cancelled = "CANCELLED".equalsIgnoreCase(dto.lifecycleStatus)
                || "CANCELLED".equalsIgnoreCase(dto.registrationStatus);
        detail.completed = "COMPLETED".equalsIgnoreCase(dto.lifecycleStatus)
                || detail.event.endAt > 0L && detail.event.endAt < System.currentTimeMillis();
        detail.offline = false;
        detail.statusText = null;
        detail.conditions = dto.participationConditions;
        detail.cancelPolicy = dto.cancellationPolicy;
        detail.onlineLinkAvailable = !isBlank(dto.onlineUrl);
        detail.organizerEventCount = dto.organizer == null ? 0L : dto.organizer.organizedEventCount;

        detail.gallery = buildGallery(dto);
        detail.tickets = buildTickets(dto, detail.event);
        detail.selectedTicketIndex = firstBookableTicket(detail.tickets);
        detail.canReview = dto.canReview || contains(dto.allowedActions, "WRITE_REVIEW");
        detail.reviewAverage = dto.reviewSummary == null
                ? detail.event.rating : (float) dto.reviewSummary.averageRating;
        detail.reviewDistribution = distribution(dto.reviewSummary);
        detail.reviews = mapHighlights(dto.reviewSummary);
        detail.reviewsError = false;

        detail.primaryAction = detail.canReview ? PrimaryAction.WRITE_REVIEW : PrimaryAction.NONE;
        detail.disabledReason = firstNonNull(dto.disabledReason, null);
        if (detail.cancelled) {
            detail.primaryAction = PrimaryAction.NONE;
        }
        if (detail.primaryAction == PrimaryAction.NONE && isBlank(detail.statusText)) {
            detail.statusText = detail.completed
                    ? "Sự kiện đã kết thúc"
                    : "Thông tin vé chỉ để tham khảo";
        }
        return detail;
    }

    @NonNull
    private List<GalleryItem> buildGallery(@NonNull DiscoveryDtos.EventDetail dto) {
        List<GalleryItem> gallery = new ArrayList<>();
        int icon = EventCategoryUi.iconRes(EventMapper.categoryOf(dto.categoryNames, dto.tags));
        if (dto.mediaUrls != null && !dto.mediaUrls.isEmpty()) {
            for (int i = 0; i < dto.mediaUrls.size(); i++) {
                gallery.add(new GalleryItem("Ảnh " + (i + 1), icon, i, dto.mediaUrls.get(i)));
            }
            return gallery;
        }
        boolean online = "ONLINE".equalsIgnoreCase(dto.eventFormat);
        int count = online ? 3 : 5;
        String[] labels = online
                ? new String[]{"Sảnh chờ trực tuyến", "Phòng trình bày", "Khu vực hỏi đáp"}
                : new String[]{"Sân khấu chính", "Khu vực check-in", "Không gian giao lưu",
                "Sân khấu phụ", "Khu vực triển lãm"};
        for (int i = 0; i < count; i++) {
            gallery.add(new GalleryItem(labels[i], icon, i, null));
        }
        return gallery;
    }

    @NonNull
    private List<TicketOption> buildTickets(@NonNull DiscoveryDtos.EventDetail dto,
                                            @NonNull EventUiModel event) {
        List<TicketOption> tickets = new ArrayList<>();
        long fallbackStart = event.startAt > 0L
                ? event.startAt - 20L * 24L * 60L * 60L * 1000L : 0L;
        long fallbackEnd = event.startAt > 0L
                ? event.startAt - 2L * 60L * 60L * 1000L : 0L;
        if (dto.ticketTypes != null) {
            for (DiscoveryDtos.TicketType type : dto.ticketTypes) {
                long saleStart = DateTimeUtils.toMillis(type.saleStartAt);
                long saleEnd = DateTimeUtils.toMillis(type.saleEndAt);
                tickets.add(new TicketOption(
                        type.id,
                        type.name == null ? "Vé" : type.name,
                        type.description,
                        type.price == null ? 0L : Math.round(type.price),
                        type.remaining == null ? -1 : type.remaining,
                        saleStart > 0L ? saleStart : fallbackStart,
                        saleEnd > 0L ? saleEnd : fallbackEnd,
                        ticketStatus(type.saleStatus, saleStart, saleEnd)));
            }
        }
        if (tickets.isEmpty()) {
            tickets.add(new TicketOption(0L, event.free ? "Vé miễn phí" : "Vé tham dự",
                    "Thông tin vé do nhà tổ chức cung cấp.",
                    event.free ? 0L : event.priceFrom, -1,
                    fallbackStart, fallbackEnd, ticketStatus(null, fallbackStart, fallbackEnd)));
        }
        return tickets;
    }

    @NonNull
    private static TicketSaleStatus ticketStatus(@Nullable String raw, long saleStart, long saleEnd) {
        if (raw != null) {
            switch (raw.toUpperCase(Locale.ROOT)) {
                case "NOT_STARTED":
                    return TicketSaleStatus.NOT_STARTED;
                case "ENDED":
                    return TicketSaleStatus.ENDED;
                case "SOLD_OUT":
                    return TicketSaleStatus.SOLD_OUT;
                case "ON_SALE":
                    return TicketSaleStatus.ON_SALE;
                default:
                    break;
            }
        }
        long now = System.currentTimeMillis();
        if (saleStart > now) {
            return TicketSaleStatus.NOT_STARTED;
        }
        if (saleEnd > 0L && saleEnd < now) {
            return TicketSaleStatus.ENDED;
        }
        return TicketSaleStatus.ON_SALE;
    }

    private int firstBookableTicket(@NonNull List<TicketOption> tickets) {
        for (int i = 0; i < tickets.size(); i++) {
            if (tickets.get(i).status == TicketSaleStatus.ON_SALE) {
                return i;
            }
        }
        return 0;
    }

    @NonNull
    private static int[] distribution(@Nullable DiscoveryDtos.ReviewSummary summary) {
        int[] values = new int[5];
        if (summary == null || summary.distribution == null) {
            return values;
        }
        for (Map.Entry<String, Long> entry : summary.distribution.entrySet()) {
            try {
                int star = Integer.parseInt(entry.getKey());
                if (star >= 1 && star <= 5 && entry.getValue() != null) {
                    values[star - 1] = (int) Math.min(Integer.MAX_VALUE, entry.getValue());
                }
            } catch (NumberFormatException ignored) {
                // Bỏ qua khoá lạ.
            }
        }
        return values;
    }

    @NonNull
    private static List<ReviewUiModel> mapHighlights(@Nullable DiscoveryDtos.ReviewSummary summary) {
        List<ReviewUiModel> reviews = new ArrayList<>();
        if (summary == null || summary.highlights == null) {
            return reviews;
        }
        for (DiscoveryDtos.ReviewBrief brief : summary.highlights) {
            reviews.add(new ReviewUiModel.Builder()
                    .id(brief.id)
                    .authorName(brief.authorName)
                    .rating(brief.rating)
                    .comment(brief.text)
                    .createdAt(DateTimeUtils.toMillis(brief.createdAt))
                    .status(ReviewUiModel.Status.VISIBLE)
                    .source(ReviewUiModel.Source.USER)
                    .build());
        }
        return reviews;
    }

    private static boolean contains(@Nullable List<String> values, @NonNull String key) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (key.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    @Nullable
    private static String firstNonNull(@Nullable String value, @Nullable String fallback) {
        return isBlank(value) ? fallback : value;
    }

    @NonNull
    private static String favoriteMessage(@NonNull ApiException exception) {
        if (exception.getHttpCode() == 409
                || "FAVORITE_LIMIT_REACHED".equalsIgnoreCase(exception.getErrorCode())) {
            return "Bạn đã đạt giới hạn sự kiện yêu thích. Hãy bỏ bớt một sự kiện trước.";
        }
        return exception.getUserMessage();
    }
}
