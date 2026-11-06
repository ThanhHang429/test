package com.eventapp.app.data.remote.dto.ticket;

import java.util.ArrayList;
import java.util.List;

/** DTO vé — khớp `TicketDto` / `RegistrationDto` của backend. */
public final class TicketDtos {

    private TicketDtos() {
    }

    public static class Ticket {
        public long ticketId;
        public String ticketCode;
        /** VALID | CHECKED_IN | CANCELLED | REFUNDED | REVOKED */
        public String status;
        public long eventId;
        public String eventTitle;
        public String eventStartAt;
        public String eventEndAt;
        public String venueName;
        public String city;
        public String coverUrl;
        public String ticketTypeName;
        public String issuedAt;
        public String checkedInAt;
        /** Mã QR soát vé; ảnh QR tải từ `/tickets/{id}/qr.png`. */
        public String qrToken;
    }

    public static class FreeRegisterRequest {
        public long ticketTypeId;
        public int quantity = 1;
        public String idempotencyKey;
    }

    public static class Registration {
        public long registrationId;
        public String registrationCode;
        public long eventId;
        public String eventTitle;
        /** CONFIRMED | CANCELLED | WAITLISTED */
        public String status;
        public int quantity;
        public String registeredAt;
        public List<Ticket> tickets = new ArrayList<>();
    }
}
