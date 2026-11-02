package com.eventapp.app.ui.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.favorite.FavoriteDtos;
import com.eventapp.app.data.remote.dto.recommendation.RecommendationDtos;
import com.eventapp.app.ui.model.EventUiModel;
import com.eventapp.app.ui.model.FavoriteUiModel;
import com.eventapp.app.ui.model.RecommendationUiModel;

/** Mapper cho danh sách yêu thích và gợi ý. */
public final class PageMappers {

    private PageMappers() {
    }

    @NonNull
    public static FavoriteUiModel favorite(@Nullable FavoriteDtos.Item dto) {
        if (dto == null) {
            return new FavoriteUiModel(null, FavoriteUiModel.State.UNAVAILABLE, 0L);
        }
        EventUiModel event = dto.event == null ? null : EventMapper.fromCard(dto.event);
        FavoriteUiModel.State state;
        if (!dto.available || event == null) {
            state = FavoriteUiModel.State.UNAVAILABLE;
        } else if (event.availability == EventUiModel.Availability.CANCELLED) {
            state = FavoriteUiModel.State.CANCELLED;
        } else if (event.endAt > 0L && event.endAt < System.currentTimeMillis()) {
            state = FavoriteUiModel.State.ENDED;
        } else {
            state = FavoriteUiModel.State.UPCOMING;
        }
        return new FavoriteUiModel(dto.eventId, event, state, DateTimeUtils.toMillis(dto.favoritedAt));
    }

    @NonNull
    public static RecommendationUiModel recommendation(@Nullable RecommendationDtos.Item dto) {
        if (dto == null) {
            return new RecommendationUiModel(new EventUiModel.Builder().build(), "", RecommendationUiModel.Source.RULE);
        }
        EventUiModel event = EventMapper.fromCard(dto.event);
        RecommendationUiModel.Source source = "AI".equalsIgnoreCase(dto.reasonSource)
                ? RecommendationUiModel.Source.AI : RecommendationUiModel.Source.RULE;
        return new RecommendationUiModel(event, dto.reason == null ? "" : dto.reason, source);
    }
}
