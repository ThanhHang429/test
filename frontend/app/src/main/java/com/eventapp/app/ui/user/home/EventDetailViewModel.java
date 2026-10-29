package com.eventapp.app.ui.user.home;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.DrawableRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.R;
import com.eventapp.app.ui.user.review.ReviewUiModel;
import com.eventapp.app.ui.user.review.SampleReviewData;

import java.util.ArrayList;
import java.util.List;

/**
 * State holder for FR-U05. Allowed actions and ticket availability are represented explicitly so
 * the screen never derives purchase actions from cached values. Dummy data is used until API-04
 * is available.
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

        public GalleryItem(String label, @DrawableRes int iconRes, int index) {
            this.label = label;
            this.iconRes = iconRes;
            this.index = index;
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
        public String conditions;
        public String cancelPolicy;
        public boolean onlineLinkAvailable;
    }

    private final MutableLiveData<DetailState> state = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> unavailable = new MutableLiveData<>(false);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long eventId = -1L;
    private DetailState current;

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
        if (current == null) {
            return;
        }
        current.ticketsError = false;
        state.setValue(current);
        handler.postDelayed(() -> {
            if (current != null) {
                current.ticketsError = false;
                state.setValue(current);
            }
        }, 300L);
    }

    public void retryReviews() {
        if (current == null) {
            return;
        }
        current.reviewsError = false;
        state.setValue(current);
        handler.postDelayed(() -> {
            if (current != null) {
                current.reviewsError = false;
                state.setValue(current);
            }
        }, 300L);
    }

    public void selectTicket(int index) {
        if (current == null || index < 0 || index >= current.tickets.size()) {
            return;
        }
        current.selectedTicketIndex = index;
        state.setValue(current);
    }

    public void toggleFavorite() {
        if (current == null || current.event == null) {
            return;
        }
        if (current.cancelled) {
            return;
        }
        current.event.favorite = !current.event.favorite;
        state.setValue(current);
    }

    private void loadInternal(boolean showSkeleton) {
        if (showSkeleton) {
            loading.setValue(true);
        }
        error.setValue(false);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            EventUiModel event = findEvent(eventId);
            if (event == null) {
                unavailable.setValue(true);
                loading.setValue(false);
                return;
            }
            unavailable.setValue(false);
            current = buildState(event);
            state.setValue(current);
            loading.setValue(false);
        }, showSkeleton ? 360L : 280L);
    }

    private EventUiModel findEvent(long id) {
        List<EventUiModel> events = SampleEventData.createEvents();
        if (id <= 0L) {
            return events.isEmpty() ? null : events.get(0);
        }
        for (EventUiModel event : events) {
            if (event.id == id) {
                return event;
            }
        }
        return null;
    }

    private DetailState buildState(EventUiModel event) {
        DetailState detail = new DetailState();
        detail.event = event;
        detail.asOf = System.currentTimeMillis();
        detail.cancelled = event.availability == EventUiModel.Availability.CANCELLED;
        detail.completed = event.endAt > 0L && event.endAt < System.currentTimeMillis();
        detail.offline = false;
        detail.onlineLinkAvailable = event.online && event.favorite;

        long saleStart = event.startAt - 20L * 24L * 60L * 60L * 1000L;
        long saleEnd = event.startAt - 2L * 60L * 60L * 1000L;
        detail.tickets = buildTickets(event, saleStart, saleEnd);
        detail.gallery = buildGallery(event);
        detail.reviews = SampleReviewData.forEvent(event.id, event.title);
        detail.reviewDistribution = SampleReviewData.distribution(detail.reviews);
        detail.reviewAverage = SampleReviewData.average(detail.reviews);
        detail.conditions = "Người tham dự cần mang theo vé QR hoặc giấy tờ tùy thân. "
                + "Trẻ em dưới 12 tuổi phải đi cùng người giám hộ. "
                + "Không mang theo vật dụng nguy hiểm hoặc thức uống có cồn từ bên ngoài.";
        detail.cancelPolicy = "Vé có thể hủy trước thời điểm bắt đầu 48 giờ và được hoàn 100% "
                + "giá trị vé. Trong vòng 48 giờ trước sự kiện, vé không được hoàn. "
                + "Trường hợp sự kiện bị hủy bởi nhà tổ chức, toàn bộ vé sẽ được hoàn trong 7 ngày "
                + "làm việc theo phương thức thanh toán ban đầu.";
        detail.canReview = detail.completed;
        detail.selectedTicketIndex = firstBookableTicket(detail.tickets);

        if (detail.cancelled) {
            detail.primaryAction = PrimaryAction.NONE;
            detail.disabledReason = null;
        } else if (detail.completed) {
            detail.primaryAction = detail.canReview ? PrimaryAction.WRITE_REVIEW
                    : PrimaryAction.NONE;
            detail.disabledReason = detail.canReview ? null : "Sự kiện đã kết thúc";
        } else if (event.availability == EventUiModel.Availability.SOLD_OUT) {
            detail.primaryAction = PrimaryAction.BUY_TICKET;
            detail.disabledReason = "Hết vé";
        } else if (event.availability == EventUiModel.Availability.UNKNOWN) {
            detail.primaryAction = PrimaryAction.NONE;
            detail.disabledReason = "Hiện chưa thể đăng ký";
        } else if (event.free) {
            detail.primaryAction = PrimaryAction.REGISTER_FREE;
            detail.disabledReason = null;
        } else {
            detail.primaryAction = PrimaryAction.BUY_TICKET;
            detail.disabledReason = null;
        }

        TicketOption selected = selectedTicket(detail);
        if (selected != null && selected.status == TicketSaleStatus.NOT_STARTED
                && detail.primaryAction != PrimaryAction.NONE) {
            detail.disabledReason = "Chưa mở bán";
        } else if (selected != null && selected.status == TicketSaleStatus.ENDED
                && detail.primaryAction != PrimaryAction.NONE) {
            detail.disabledReason = "Đã kết thúc bán";
        } else if (selected != null && selected.status == TicketSaleStatus.SOLD_OUT
                && detail.primaryAction != PrimaryAction.NONE) {
            detail.disabledReason = "Hết vé";
        }
        return detail;
    }

    private List<TicketOption> buildTickets(EventUiModel event, long saleStart, long saleEnd) {
        List<TicketOption> tickets = new ArrayList<>();
        long now = System.currentTimeMillis();
        boolean soldOut = event.availability == EventUiModel.Availability.SOLD_OUT
                || event.availability == EventUiModel.Availability.CANCELLED;
        if (event.free) {
            TicketSaleStatus status;
            if (soldOut) {
                status = TicketSaleStatus.SOLD_OUT;
            } else if (saleStart > now) {
                status = TicketSaleStatus.NOT_STARTED;
            } else if (saleEnd < now) {
                status = TicketSaleStatus.ENDED;
            } else {
                status = TicketSaleStatus.ON_SALE;
            }
            tickets.add(new TicketOption(1L, "Vé miễn phí", "Mỗi tài khoản đăng ký tối đa 2 vé.",
                    0L, soldOut ? 0 : 120, saleStart, saleEnd, status));
            return tickets;
        }

        long basePrice = Math.max(event.priceFrom, 100_000L);
        tickets.add(new TicketOption(1L, "Vé Standard",
                "Bao gồm quyền tham dự toàn bộ chương trình.",
                basePrice, soldOut ? 0 : 48, saleStart, saleEnd,
                ticketStatus(soldOut, saleStart, saleEnd, now)));
        tickets.add(new TicketOption(2L, "Vé Premium",
                "Khu vực ghế gần sân khấu, nước uống và quà tặng.",
                basePrice + 250_000L, soldOut ? 0 : 8, saleStart, saleEnd,
                ticketStatus(soldOut, saleStart, saleEnd, now)));
        if (event.rating >= 4.7f) {
            tickets.add(new TicketOption(3L, "Vé VIP",
                    "Phòng chờ riêng, check-in ưu tiên và gặp gỡ diễn giả.",
                    basePrice + 600_000L, soldOut ? 0 : 4, saleStart, saleEnd,
                    ticketStatus(soldOut, saleStart, saleEnd, now)));
        }
        return tickets;
    }

    private TicketSaleStatus ticketStatus(boolean soldOut, long saleStart, long saleEnd, long now) {
        if (soldOut) {
            return TicketSaleStatus.SOLD_OUT;
        }
        if (saleStart > now) {
            return TicketSaleStatus.NOT_STARTED;
        }
        if (saleEnd < now) {
            return TicketSaleStatus.ENDED;
        }
        return TicketSaleStatus.ON_SALE;
    }

    private List<GalleryItem> buildGallery(EventUiModel event) {
        List<GalleryItem> gallery = new ArrayList<>();
        int icon = EventCategoryUi.iconRes(event.category);
        int count = event.online ? 3 : 5;
        String[] labels = event.online
                ? new String[]{"Sảnh chờ trực tuyến", "Phòng trình bày", "Khu vực hỏi đáp"}
                : new String[]{"Sân khấu chính", "Khu vực check-in", "Không gian giao lưu",
                "Sân khấu phụ", "Khu vực triển lãm"};
        for (int i = 0; i < count; i++) {
            gallery.add(new GalleryItem(labels[i], icon, i));
        }
        return gallery;
    }

    private int firstBookableTicket(List<TicketOption> tickets) {
        for (int i = 0; i < tickets.size(); i++) {
            if (tickets.get(i).status == TicketSaleStatus.ON_SALE) {
                return i;
            }
        }
        return 0;
    }

    private TicketOption selectedTicket(DetailState detail) {
        if (detail.tickets.isEmpty()) {
            return null;
        }
        int index = Math.max(0, Math.min(detail.selectedTicketIndex, detail.tickets.size() - 1));
        return detail.tickets.get(index);
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
