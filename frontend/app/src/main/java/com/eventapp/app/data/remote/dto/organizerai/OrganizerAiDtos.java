package com.eventapp.app.data.remote.dto.organizerai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** AI cho organizer (FR-O06 / FR-O27). */
public final class OrganizerAiDtos {

    private OrganizerAiDtos() {
    }

    public static class DescriptionRequest {
        public String title;
        public Long categoryId;
        public String format;
        public String startTime;
        public String location;
        public String onlineInfo;
        public String audience;
        public List<String> keyPoints = new ArrayList<>();
        public String tone;
        public String length;
        public Integer variants;
        public Long eventId;
    }

    public static class DescriptionVariant {
        public String text;
        public String shortSummary;
    }

    public static class DescriptionResponse {
        public List<DescriptionVariant> descriptions = new ArrayList<>();
        public List<String> missingInfo = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public String source;
        public Long aiRequestId;
        public String correlationId;
    }

    /** Thông báo gửi người mua (FR-O06): chủ đề + đối tượng + ý chính, AI soạn 1–3 bản. */
    public static class AnnouncementRequest {
        public String topic;
        public Long eventId;
        public String eventTitle;
        public String audience;
        public List<String> keyPoints = new ArrayList<>();
        public String tone;
        public String length;
        public Integer variants;
    }

    public static class AnnouncementResponse {
        public List<DescriptionVariant> variants = new ArrayList<>();
        public List<String> missingInfo = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public String source;
        public Long aiRequestId;
        public String correlationId;
    }

    public static class Insight {
        public String type;
        public String text;
        public String relatedMetric;
        public String confidence;
    }

    public static class AnalysisResponse {
        public long eventId;
        public Map<String, Object> metrics = new LinkedHashMap<>();
        public List<String> summary = new ArrayList<>();
        public List<Insight> insights = new ArrayList<>();
        public String disclaimer;
        public List<String> dataLimitations = new ArrayList<>();
        public boolean insufficientData;
        public String source;
        public String generatedAt;
        public Long aiRequestId;
        public String correlationId;
    }
}
