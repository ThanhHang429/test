package com.eventapp.app.ui.organizer.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.order.OrderDtos;
import com.eventapp.app.data.repository.OrderRepository;
import com.eventapp.app.ui.organizer.model.OrgOrder;
import com.eventapp.app.ui.organizer.model.OrgTone;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.google.gson.JsonElement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Đơn hàng nhà tổ chức (FR-O17). */
public class OrgOrdersViewModel extends ViewModel {

    public static final class OrderStats {
        public final int total;
        public final int paid;
        public final int pending;
        public final int refund;

        public OrderStats(int total, int paid, int pending, int refund) {
            this.total = total;
            this.paid = paid;
            this.pending = pending;
            this.refund = refund;
        }
    }

    private final OrderRepository repository;
    private final MutableLiveData<List<OrgOrder>> ordersLive = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<OrderStats> statsLive = new MutableLiveData<>(new OrderStats(0, 0, 0, 0));
    private final MutableLiveData<Boolean> loadingLive = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLive = new MutableLiveData<>();

    public OrgOrdersViewModel() {
        this.repository = ServiceLocator.get(OrderRepository.class);
    }

    public LiveData<List<OrgOrder>> getOrders() {
        return ordersLive;
    }

    public LiveData<OrderStats> getStats() {
        return statsLive;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLive;
    }

    public LiveData<String> getError() {
        return errorLive;
    }

    public void consumeError() {
        errorLive.setValue(null);
    }

    public void loadOrders(@Nullable String status, @Nullable String query) {
        loadingLive.setValue(true);
        repository.getOrganizerOrders(status, query, new ApiCallback<List<OrderDtos.Order>>() {
            @Override
            public void onSuccess(List<OrderDtos.Order> data, JsonElement meta) {
                loadingLive.setValue(false);
                List<OrgOrder> mapped = new ArrayList<>();
                int paid = 0;
                int pending = 0;
                int refund = 0;
                if (data != null) {
                    for (OrderDtos.Order dto : data) {
                        OrgOrder item = fromDto(dto);
                        mapped.add(item);
                        if (OrgOrder.FILTER_PAID.equals(item.filter)) paid++;
                        else if (OrgOrder.FILTER_PENDING.equals(item.filter)) pending++;
                        else if (OrgOrder.FILTER_REFUND.equals(item.filter)) refund++;
                    }
                }
                ordersLive.setValue(mapped);
                statsLive.setValue(new OrderStats(mapped.size(), paid, pending, refund));
            }

            @Override
            public void onError(@NonNull ApiException error) {
                loadingLive.setValue(false);
                errorLive.setValue(error.getUserMessage());
            }
        });
    }

    public static OrgOrder fromDto(@NonNull OrderDtos.Order dto) {
        String raw = dto.status != null ? dto.status.trim().toUpperCase(Locale.ROOT) : "PENDING_PAYMENT";
        long amount = dto.totalAmount != null ? dto.totalAmount.longValue() : 0L;

        String filter;
        String statusLabel;
        int tone;
        switch (raw) {
            case "PAID":
                filter = OrgOrder.FILTER_PAID;
                statusLabel = amount > 0 ? "ĐÃ THANH TOÁN" : "MIỄN PHÍ";
                tone = OrgTone.SUCCESS;
                break;
            case "PENDING_PAYMENT":
                filter = OrgOrder.FILTER_PENDING;
                statusLabel = "CHỜ XỬ LÝ";
                tone = OrgTone.WARNING;
                break;
            case "REFUND_PENDING":
                filter = OrgOrder.FILTER_REFUND;
                statusLabel = "CHỜ HOÀN TIỀN";
                tone = OrgTone.WARNING;
                break;
            case "REFUNDED":
            case "PARTIALLY_REFUNDED":
                filter = OrgOrder.FILTER_REFUND;
                statusLabel = "HOÀN TIỀN";
                tone = OrgTone.DANGER;
                break;
            case "EXPIRED":
                filter = "CANCELLED";
                statusLabel = "HẾT HẠN";
                tone = OrgTone.NEUTRAL;
                break;
            case "CANCELLED":
            default:
                filter = "CANCELLED";
                statusLabel = "ĐÃ HỦY";
                tone = OrgTone.NEUTRAL;
                break;
        }

        String ticketSummary = dto.ticketSummary;
        if (ticketSummary == null || ticketSummary.trim().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (dto.items != null) {
                for (OrderDtos.Item item : dto.items) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(item.quantity).append("x ").append(item.ticketName);
                }
            }
            ticketSummary = sb.length() > 0 ? sb.toString() : "Vé sự kiện";
        }

        String buyer = dto.buyerName != null && !dto.buyerName.trim().isEmpty()
                ? dto.buyerName.trim() : "Khách hàng";
        String contact = dto.buyerEmail != null && !dto.buyerEmail.trim().isEmpty()
                ? dto.buyerEmail.trim() : "—";
        String eventTitle = dto.eventTitle != null && !dto.eventTitle.trim().isEmpty()
                ? dto.eventTitle.trim() : "Sự kiện #" + dto.eventId;
        String timeStr = formatIso(dto.paidAt != null ? dto.paidAt : dto.createdAt);
        String paymentStr = dto.paymentMethod != null && !dto.paymentMethod.trim().isEmpty()
                ? dto.paymentMethod.trim()
                : (amount <= 0 ? "Miễn phí" : "Chuyển khoản / Ví");

        return new OrgOrder(
                dto.orderId,
                raw,
                dto.orderCode != null ? dto.orderCode : ("ORD-" + dto.orderId),
                amount,
                filter,
                statusLabel,
                tone,
                buyer,
                eventTitle,
                ticketSummary,
                timeStr,
                paymentStr,
                contact
        );
    }

    private static String formatIso(@Nullable String iso) {
        if (iso == null || iso.trim().isEmpty()) return "—";
        try {
            return OrgFormat.dateTime(Instant.parse(iso.trim()).toEpochMilli());
        } catch (Exception ignored) {
            return iso;
        }
    }
}
