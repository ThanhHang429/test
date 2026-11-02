package com.eventapp.app.ui.model;

import com.eventapp.app.ui.model.EventUiModel;

/**
 * Row state for FR-U06. {@code event == null} means the saved event is no longer public
 * (suspended/unpublished) so the row must not expose its title or cover.
 */
public class FavoriteUiModel {

    public enum State {
        UPCOMING,
        ENDED,
        CANCELLED,
        UNAVAILABLE
    }

    public final EventUiModel event;
    public final State state;
    public final long savedAt;
    /** Id sự kiện backend trả về, giữ lại cả khi {@code event == null} để còn bỏ yêu thích được. */
    public final long eventId;

    public FavoriteUiModel(EventUiModel event, State state, long savedAt) {
        this(event == null ? -1L : event.id, event, state, savedAt);
    }

    public FavoriteUiModel(long eventId, EventUiModel event, State state, long savedAt) {
        this.eventId = eventId > 0L ? eventId : (event == null ? -1L : event.id);
        this.event = event;
        this.state = state == null ? State.UNAVAILABLE : state;
        this.savedAt = savedAt;
    }

    public boolean isUnavailable() {
        return state == State.UNAVAILABLE || event == null;
    }

    public static FavoriteUiModel from(EventUiModel event, long savedAt) {
        return new FavoriteUiModel(event, stateOf(event, savedAt), savedAt);
    }

    private static State stateOf(EventUiModel event, long now) {
        if (event == null) {
            return State.UNAVAILABLE;
        }
        if (event.availability == EventUiModel.Availability.CANCELLED) {
            return State.CANCELLED;
        }
        if (event.endAt > 0L && event.endAt < now) {
            return State.ENDED;
        }
        return State.UPCOMING;
    }
}
