package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.api.TicketApi;
import com.eventapp.app.data.remote.dto.ticket.TicketDtos;
import com.eventapp.app.ui.model.TicketUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Repository quản lý Vé điện tử thật từ Backend (`/api/v1/tickets`, FR-U12).
 *
 * <p>Giữ cache danh sách vé mới nhất trong phiên đăng nhập để các màn hình Vé, Chi tiết vé,
 * Lịch và Tài khoản đồng bộ tức thì ngay khi thanh toán Webhook hoàn tất.</p>
 */
public class TicketRepository {

    private final TicketApi api;
    private final CopyOnWriteArrayList<TicketUiModel> cachedTickets = new CopyOnWriteArrayList<>();

    public TicketRepository(@NonNull TicketApi api) {
        this.api = api;
    }

    public void getMyTickets(@Nullable String status, int page, int size,
                             @NonNull ApiCallback<List<TicketUiModel>> callback) {
        ApiCallback.enqueue(api.list(status, page, size), new ApiCallback<List<TicketDtos.Ticket>>() {
            @Override
            public void onSuccess(@Nullable List<TicketDtos.Ticket> data, @Nullable JsonElement meta) {
                List<TicketUiModel> mapped = mapList(data);
                if ((status == null || "ALL".equalsIgnoreCase(status)) && page == 0) {
                    cachedTickets.clear();
                    cachedTickets.addAll(mapped);
                }
                callback.onSuccess(mapped, meta);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                callback.onError(error);
            }
        });
    }

    public void getTicketDetail(long ticketId, @NonNull ApiCallback<TicketUiModel> callback) {
        ApiCallback.enqueue(api.get(ticketId), new ApiCallback<TicketDtos.Ticket>() {
            @Override
            public void onSuccess(@Nullable TicketDtos.Ticket data, @Nullable JsonElement meta) {
                TicketUiModel mapped = map(data);
                if (mapped != null) {
                    upsertCache(mapped);
                }
                callback.onSuccess(mapped, meta);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                callback.onError(error);
            }
        });
    }

    @NonNull
    public List<TicketUiModel> getCachedTickets() {
        return Collections.unmodifiableList(new ArrayList<>(cachedTickets));
    }

    @Nullable
    public TicketUiModel findCachedByCode(@Nullable String ticketCode) {
        if (ticketCode == null || ticketCode.trim().isEmpty()) {
            return null;
        }
        String target = ticketCode.trim();
        for (TicketUiModel item : cachedTickets) {
            if (target.equalsIgnoreCase(item.code)) {
                return item;
            }
        }
        return null;
    }

    @Nullable
    public TicketUiModel findCachedById(long ticketId) {
        if (ticketId <= 0) {
            return null;
        }
        for (TicketUiModel item : cachedTickets) {
            if (item.id == ticketId) {
                return item;
            }
        }
        return null;
    }

    private void upsertCache(@NonNull TicketUiModel ticket) {
        for (int i = 0; i < cachedTickets.size(); i++) {
            if (cachedTickets.get(i).id == ticket.id) {
                cachedTickets.set(i, ticket);
                return;
            }
        }
        cachedTickets.add(0, ticket);
    }

    @NonNull
    public static List<TicketUiModel> mapList(@Nullable List<TicketDtos.Ticket> dtos) {
        List<TicketUiModel> result = new ArrayList<>();
        if (dtos == null) {
            return result;
        }
        for (TicketDtos.Ticket dto : dtos) {
            TicketUiModel mapped = map(dto);
            if (mapped != null) {
                result.add(mapped);
            }
        }
        return result;
    }

    @Nullable
    public static TicketUiModel map(@Nullable TicketDtos.Ticket dto) {
        if (dto == null) {
            return null;
        }
        TicketUiModel.Status status = mapStatus(dto.status, dto.checkedInAt);
        long startAt = DateTimeUtils.toMillis(dto.eventStartAt);
        long endAt = DateTimeUtils.toMillis(dto.eventEndAt);
        return new TicketUiModel.Builder()
                .id(dto.ticketId)
                .eventId(dto.eventId)
                .code(dto.ticketCode)
                .qrToken(dto.qrToken)
                .eventTitle(dto.eventTitle)
                .coverUrl(dto.coverUrl)
                .category(inferCategory(dto.eventTitle))
                .ticketType(dto.ticketTypeName != null && !dto.ticketTypeName.isEmpty()
                        ? dto.ticketTypeName : "Vé tham dự")
                .venue(dto.venueName != null ? dto.venueName : "")
                .city(dto.city != null ? dto.city : "")
                .startAt(startAt)
                .endAt(endAt)
                .quantity(1)
                .totalPrice(0L)
                .orderCode(dto.ticketCode)
                .status(status)
                .build();
    }

    private static TicketUiModel.Status mapStatus(@Nullable String raw, @Nullable String checkedInAt) {
        if (checkedInAt != null && !checkedInAt.trim().isEmpty()) {
            return TicketUiModel.Status.USED;
        }
        if (raw == null) {
            return TicketUiModel.Status.UPCOMING;
        }
        String upper = raw.trim().toUpperCase(Locale.US);
        switch (upper) {
            case "CHECKED_IN":
            case "USED":
                return TicketUiModel.Status.USED;
            case "CANCELLED":
            case "REFUNDED":
            case "REVOKED":
            case "EXPIRED":
                return TicketUiModel.Status.CANCELLED;
            default:
                return TicketUiModel.Status.UPCOMING;
        }
    }

    private static EventCategory inferCategory(@Nullable String title) {
        if (title == null) {
            return EventCategory.OTHER;
        }
        String lower = title.toLowerCase(Locale.ROOT);
        if (lower.contains("tech") || lower.contains("ai") || lower.contains("dev") || lower.contains("công nghệ")) {
            return EventCategory.TECH;
        }
        if (lower.contains("music") || lower.contains("nhạc") || lower.contains("concert") || lower.contains("indie")) {
            return EventCategory.MUSIC;
        }
        if (lower.contains("startup") || lower.contains("business") || lower.contains("kinh doanh")) {
            return EventCategory.BUSINESS;
        }
        if (lower.contains("design") || lower.contains("art") || lower.contains("nghệ thuật")) {
            return EventCategory.ART;
        }
        if (lower.contains("workshop") || lower.contains("đào tạo") || lower.contains("education")) {
            return EventCategory.EDUCATION;
        }
        return EventCategory.OTHER;
    }
}
