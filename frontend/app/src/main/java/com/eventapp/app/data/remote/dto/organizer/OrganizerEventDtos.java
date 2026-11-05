package com.eventapp.app.data.remote.dto.organizer;

import java.util.ArrayList;
import java.util.List;

/** Sự kiện của organizer đang đăng nhập (FR-O02). */
public final class OrganizerEventDtos {

    private OrganizerEventDtos() {
    }

    public static class TicketType {
        public long id;
        public String name;
        /** FREE | PAID */
        public String kind;
        public double price;
        public String currency;
        public Integer quantityTotal;
        public int quantitySold;
        public int quantityReserved;
        public Integer remaining;
        /** ACTIVE | PAUSED | ENDED | ARCHIVED */
        public String status;
        public String saleStartAt;
        public String saleEndAt;
    }

    public static class Event {
        public long id;
        public String code;
        public String title;
        public String slug;
        public String coverUrl;
        /** DRAFT | PENDING_REVIEW | REVISION_REQUIRED | REJECTED | APPROVED */
        public String reviewStatus;
        /** UNPUBLISHED | PUBLISHED | SUSPENDED | CANCELLED | COMPLETED */
        public String lifecycleStatus;
        public String startAt;
        public String endAt;
        public String timezone;
        public String city;
        public String venueName;
        public Integer capacityLimit;
        public List<TicketType> ticketTypes = new ArrayList<>();
        public int ticketsSold;
        public double revenue;
        public String currency;
    }
}
