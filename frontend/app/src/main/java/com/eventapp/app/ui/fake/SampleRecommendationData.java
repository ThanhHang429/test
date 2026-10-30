package com.eventapp.app.ui.fake;


import com.eventapp.app.ui.model.EventUiModel;
import com.eventapp.app.ui.model.RecommendationUiModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Sample ranked suggestions until API-10 exists. Reasons mimic the backend wording. */
public final class SampleRecommendationData {

    private static final int MAX_ITEMS = 8;

    private SampleRecommendationData() {
    }

    public static List<RecommendationUiModel> create() {
        List<EventUiModel> ranked = new ArrayList<>();
        for (EventUiModel event : SampleEventData.createEvents()) {
            if (event.aiScore != null
                    && event.availability != EventUiModel.Availability.CANCELLED) {
                ranked.add(event);
            }
        }
        Collections.sort(ranked, (left, right) ->
                Integer.compare(right.aiScore, left.aiScore));

        List<RecommendationUiModel> items = new ArrayList<>();
        for (int i = 0; i < ranked.size() && items.size() < MAX_ITEMS; i++) {
            EventUiModel event = ranked.get(i);
            boolean rule = i % 4 == 3;
            String reason = rule
                    ? "Cùng danh mục với các sự kiện bạn đã lưu gần đây."
                    : event.aiReason;
            items.add(new RecommendationUiModel(event, reason,
                    rule ? RecommendationUiModel.Source.RULE : RecommendationUiModel.Source.AI));
        }
        return items;
    }
}
