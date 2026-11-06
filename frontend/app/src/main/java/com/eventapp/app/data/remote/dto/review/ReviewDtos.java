package com.eventapp.app.data.remote.dto.review;

import java.util.ArrayList;
import java.util.List;

/** Đánh giá công khai, đánh giá của tôi và hàng chờ kiểm duyệt (FR-U20 / FR-A14 / FR-A07). */
public final class ReviewDtos {

    private ReviewDtos() {
    }

    public static class Review {
        public long id;
        public String authorName;
        public int rating;
        public String text;
        public String status;
        public String statusLabel;
        public boolean mine;
        public String createdAt;
        public String updatedAt;
    }

    public static class MyReview {
        public long id;
        public long eventId;
        public String eventTitle;
        public String eventCoverUrl;
        public int rating;
        public String text;
        public String status;
        public String statusLabel;
        public String moderationReason;
        public String createdAt;
        public String updatedAt;
        public int editCount;
        public boolean canEdit;
        public long editDaysLeft;
    }

    public static class Mutation {
        public MyReview review;
        public String notice;
    }

    public static class CreateRequest {
        public Integer rating;
        public String text;
    }

    public static class UpdateRequest {
        public Integer rating;
        public String text;
        public Long version;
    }

    public static class ReportRequest {
        public String reasonCode;
        public String note;

        public static ReportRequest of(String reasonCode, String note) {
            ReportRequest request = new ReportRequest();
            request.reasonCode = reasonCode;
            request.note = note;
            return request;
        }
    }

    public static class ReportResult {
        public long reviewId;
        public boolean reported;
    }

    public static class AdminReview {
        public long id;
        public long eventId;
        public String eventTitle;
        public String authorName;
        public String authorEmail;
        public int rating;
        public String text;
        public String status;
        public String statusLabel;
        public String moderationReason;
        public List<String> flaggedCodes = new ArrayList<>();
        public long reportCount;
        public String createdAt;
        public String updatedAt;
        public String moderatedAt;
    }

    public static class ModerationFlag {
        public String id;
        public String targetType;
        public long targetId;
        public Long eventId;
        public String eventTitle;
        public String reasonCode;
        public String reasonLabel;
        public String evidence;
        public String severity;
        public String status;
        public String createdAt;
    }

    public static class ModerationAction {
        public String reasonCode;
        public String note;

        public ModerationAction(String reasonCode) {
            this.reasonCode = reasonCode;
        }

        public ModerationAction(String reasonCode, String note) {
            this.reasonCode = reasonCode;
            this.note = note;
        }
    }

    public static class RescanRequest {
        public String targetType = "REVIEW";
        public Integer sinceDays;

        public RescanRequest(Integer sinceDays) {
            this.sinceDays = sinceDays;
        }
    }

    public static class RescanResult {
        public int scanned;
        public int flagged;
        public List<String> flagIds = new ArrayList<>();
    }
}
