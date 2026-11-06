package com.eventapp.app.data.remote.dto.organizer;

import com.eventapp.app.data.remote.dto.notification.NotificationResponse;

import java.util.ArrayList;
import java.util.List;

/** Điều phối phòng: cảnh báo đông phòng, phòng dự phòng và thông báo (FR-O28). */
public final class CrowdDtos {

    private CrowdDtos() {
    }

    public static class OverflowRoom {
        public long id;
        public String roomName;
        public int capacity;
        public String note;
        /** ACTIVE | CLOSED */
        public String status;
        public String createdAt;
    }

    public static class CreateRoomRequest {
        public String roomName;
        public int capacity;
        public String note;

        public CreateRoomRequest(String roomName, int capacity, String note) {
            this.roomName = roomName;
            this.capacity = capacity;
            this.note = note;
        }
    }

    public static class CrowdStatus {
        public long eventId;
        public String eventTitle;
        public int capacityLimit;
        public long registered;
        public long checkedIn;
        public long notCheckedIn;
        /** checkedIn / capacityLimit (0…1+) */
        public double occupancyRate;
        /** checkedIn / registered (0…1) */
        public double checkinRate;
        /** NORMAL | WATCH | CROWDED | FULL */
        public String level;
        public String warning;
        public List<OverflowRoom> overflowRooms = new ArrayList<>();
        public OverflowRoom suggestedRoom;
        public String suggestion;
    }

    public static class SendNotificationRequest {
        public String title;
        public String body;
        public String audience;
        public String source;

        public SendNotificationRequest(String title, String body, String audience, String source) {
            this.title = title;
            this.body = body;
            this.audience = audience;
            this.source = source;
        }
    }

    public static class SendNotificationResponse {
        public NotificationResponse notification;
        public int recipientCount;
    }
}
