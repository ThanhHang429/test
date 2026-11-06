package com.eventapp.app.data.remote.dto.favorite;

import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;

/** Yêu thích (FR-U06). */
public final class FavoriteDtos {

    private FavoriteDtos() {
    }

    public static class Item {
        public long eventId;
        public boolean available;
        public String statusLabel;
        public String favoritedAt;
        public DiscoveryDtos.EventCard event;
    }

    public static class ToggleResult {
        public long eventId;
        public boolean isFavorite;
    }
}
