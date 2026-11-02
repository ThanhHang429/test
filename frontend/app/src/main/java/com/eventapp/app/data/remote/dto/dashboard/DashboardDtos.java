package com.eventapp.app.data.remote.dto.dashboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dashboard organizer (FR-O03). */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    public static class BlockMeta {
        public String status;
        public String asOf;
        public String note;
    }

    public static class EventStatusBlock {
        public Map<String, Long> counts = new LinkedHashMap<>();
        public long total;
        public BlockMeta meta;
    }

    public static class RegistrationBlock {
        public long confirmed;
        public long cancelled;
        public long waitlisted;
        public long ticketsIssued;
        public long ticketsCancelled;
        public BlockMeta meta;
    }

    public static class FinanceBlock {
        public double verifiedAmount;
        public double pendingAmount;
        public double refundedAmount;
        public double feeAmount;
        public double netAmount;
        public String currency;
        public BlockMeta meta;
    }

    public static class CheckinBlock {
        public long checkedIn;
        public long ticketsIssued;
        public double checkinRate;
        public BlockMeta meta;
    }

    public static class ReviewBlock {
        public double averageRating;
        public long totalVisible;
        public long newInRange;
        public BlockMeta meta;
    }

    public static class Task {
        public String type;
        public long eventId;
        public String title;
        public String detail;
        public String dueAt;
        public int priority;
    }

    public static class TopEvent {
        public long eventId;
        public String title;
        public double verifiedAmount;
        public long registrations;
    }

    public static class Dashboard {
        public String range;
        public String from;
        public String to;
        public EventStatusBlock events;
        public RegistrationBlock registrations;
        public FinanceBlock finance;
        public CheckinBlock checkin;
        public ReviewBlock reviews;
        public List<Task> tasks = new ArrayList<>();
        public List<TopEvent> topEvents = new ArrayList<>();
        public String asOf;
    }
}
