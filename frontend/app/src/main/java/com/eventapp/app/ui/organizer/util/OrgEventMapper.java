package com.eventapp.app.ui.organizer.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.organizer.OrganizerEventDtos;
import com.eventapp.app.ui.organizer.model.OrgEvent;

import java.util.ArrayList;
import java.util.List;

/** Chuyển DTO sự kiện của organizer sang model hiển thị. */
public final class OrgEventMapper {

    private OrgEventMapper() {
    }

    @NonNull
    public static OrgEvent from(@Nullable OrganizerEventDtos.Event dto) {
        if (dto == null) {
            return empty();
        }
        List<OrgEvent.TicketType> types = new ArrayList<>();
        if (dto.ticketTypes != null) {
            for (OrganizerEventDtos.TicketType type : dto.ticketTypes) {
                if (type == null) {
                    continue;
                }
                int total = type.quantityTotal != null
                        ? type.quantityTotal
                        : type.quantitySold + (type.remaining == null ? 0 : type.remaining);
                types.add(new OrgEvent.TicketType(
                        type.id,
                        type.name,
                        type.kind,
                        Math.round(type.price),
                        type.quantitySold,
                        total,
                        type.remaining,
                        type.status));
            }
        }
        return new OrgEvent(
                dto.id,
                dto.code,
                dto.title,
                dto.coverUrl,
                dto.reviewStatus,
                dto.lifecycleStatus,
                dto.city,
                dto.venueName,
                dto.currency,
                DateTimeUtils.toMillis(dto.startAt),
                DateTimeUtils.toMillis(dto.endAt),
                dto.capacityLimit,
                dto.ticketsSold,
                Math.round(dto.revenue),
                types);
    }

    @NonNull
    private static OrgEvent empty() {
        return new OrgEvent(0L, "", "", null, "", "", "", "", "VND",
                0L, 0L, null, 0, 0L, new ArrayList<>());
    }
}
