package com.eventapp.app.ui.user.favorite;

import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.ui.user.home.EventUiModel;

import java.util.ArrayList;
import java.util.List;

/** Sample saved events until FR-U06 is wired to the backend. */
public final class SampleFavoriteData {

    private static final long DAY = 24L * 60L * 60L * 1000L;
    private static final long HOUR = 60L * 60L * 1000L;

    private SampleFavoriteData() {
    }

    public static List<FavoriteUiModel> create(List<EventUiModel> catalogue) {
        long now = System.currentTimeMillis();
        List<FavoriteUiModel> items = new ArrayList<>();

        int savedIndex = 0;
        for (EventUiModel event : catalogue) {
            if (event.id != 1L && event.id != 2L && event.id != 3L
                    && event.id != 6L && event.id != 9L) {
                continue;
            }
            event.favorite = true;
            items.add(FavoriteUiModel.from(event, now - (savedIndex + 1) * DAY));
            savedIndex++;
        }

        EventUiModel ended = new EventUiModel.Builder()
                .id(101L)
                .title("Chuỗi workshop Nhiếp ảnh Mobile")
                .organizer("Creative Lab")
                .venue("Vườn ươm Sáng tạo")
                .city("Hà Nội")
                .startAt(now - 6 * DAY)
                .endAt(now - 6 * DAY + 3 * HOUR)
                .priceFrom(200_000L)
                .category(EventCategory.EDUCATION)
                .rating(4.6f)
                .reviewCount(41)
                .favorite(true)
                .build();
        items.add(FavoriteUiModel.from(ended, now - 12 * DAY));

        EventUiModel cancelled = new EventUiModel.Builder()
                .id(102L)
                .title("Đêm nhạc Acoustic ngoài trời")
                .organizer("Open Stage")
                .venue("Phố đi bộ Nguyễn Huệ")
                .city("TP. Hồ Chí Minh")
                .startAt(now + 4 * DAY)
                .endAt(now + 4 * DAY + 2 * HOUR)
                .priceFrom(0L)
                .availability(EventUiModel.Availability.CANCELLED)
                .category(EventCategory.MUSIC)
                .rating(4.4f)
                .reviewCount(18)
                .favorite(true)
                .build();
        items.add(FavoriteUiModel.from(cancelled, now - 2 * DAY));

        items.add(new FavoriteUiModel(null, FavoriteUiModel.State.UNAVAILABLE, now - 20 * DAY));
        return items;
    }
}
