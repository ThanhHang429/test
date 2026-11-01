package com.eventapp.app.ui.fake;


import com.eventapp.app.ui.model.EventUiModel;
import com.eventapp.app.core.enums.EventCategory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Dummy catalogue used until the event API and SQLite cache are available. */
public final class SampleEventData {

    private static final long DAY = 24L * 60L * 60L * 1000L;

    private SampleEventData() {
    }

    public static List<EventUiModel> createEvents() {
        // Neo về 00:00 hôm nay để giờ bắt đầu luôn tròn (19:00 thay vì 19:37) trên card.
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        long now = midnight.getTimeInMillis();
        List<EventUiModel> events = new ArrayList<>();

        events.add(new EventUiModel.Builder()
                .id(1)
                .title("Đêm nhạc Indie Sài Gòn 2026")
                .description("Đêm nhạc indie quy tụ các ban nhạc trẻ tại TP. Hồ Chí Minh.")
                .organizer("Indie Collective")
                .venue("Nhà hát Hòa Bình")
                .city("TP. Hồ Chí Minh")
                .startAt(now + 2 * DAY + 19 * 3600_000L)
                .endAt(now + 2 * DAY + 22 * 3600_000L)
                .priceFrom(350_000L)
                .availability(EventUiModel.Availability.FEW_LEFT)
                .remainingTickets(12)
                .category(EventCategory.MUSIC)
                .rating(4.8f)
                .reviewCount(128)
                .aiScore(94)
                .aiReason("Bạn thường nghe indie và đã lưu 3 sự kiện tương tự.")
                .addTag("indie")
                .addTag("live music")
                .build());

        events.add(new EventUiModel.Builder()
                .id(2)
                .title("Hội thảo AI & Tương lai 2026")
                .description("Cập nhật xu hướng AI, robotics và kỹ năng nghề nghiệp tương lai.")
                .organizer("Tech Horizon")
                .venue("Trực tuyến")
                .city("TP. Hồ Chí Minh")
                .startAt(now + 5 * DAY + 9 * 3600_000L)
                .endAt(now + 5 * DAY + 12 * 3600_000L)
                .online(true)
                .priceFrom(0L)
                .availability(EventUiModel.Availability.AVAILABLE)
                .category(EventCategory.TECH)
                .rating(4.7f)
                .reviewCount(89)
                .aiScore(91)
                .aiReason("Phù hợp với sở thích công nghệ và các sự kiện bạn đã xem.")
                .addTag("ai")
                .addTag("hội thảo")
                .build());

        events.add(new EventUiModel.Builder()
                .id(3)
                .title("Giải chạy Marathon Quốc tế")
                .organizer("Saigon Runners")
                .venue("Công viên 23/9")
                .city("TP. Hồ Chí Minh")
                .startAt(now + 9 * DAY + 5 * 3600_000L)
                .endAt(now + 9 * DAY + 10 * 3600_000L)
                .priceFrom(250_000L)
                .availability(EventUiModel.Availability.FEW_LEFT)
                .remainingTickets(5)
                .category(EventCategory.SPORTS)
                .rating(4.6f)
                .reviewCount(214)
                .aiScore(88)
                .aiReason("Bạn đã quan tâm các hoạt động thể thao ngoài trời.")
                .addTag("marathon")
                .addTag("chạy bộ")
                .build());

        events.add(new EventUiModel.Builder()
                .id(4)
                .title("Triển lãm Sắc màu Đương đại")
                .organizer("Galerie 39")
                .venue("Bảo tàng Mỹ thuật Việt Nam")
                .city("Hà Nội")
                .startAt(now + 1 * DAY + 10 * 3600_000L)
                .endAt(now + 1 * DAY + 18 * 3600_000L)
                .priceFrom(120_000L)
                .availability(EventUiModel.Availability.AVAILABLE)
                .category(EventCategory.ART)
                .rating(4.9f)
                .reviewCount(64)
                .aiScore(86)
                .aiReason("Khớp với gu nghệ thuật thị giác của bạn.")
                .addTag("triển lãm")
                .addTag("đương đại")
                .build());

        events.add(new EventUiModel.Builder()
                .id(5)
                .title("Food Festival Đà Nẵng")
                .organizer("Danang Foodies")
                .venue("Công viên Biển Đông")
                .city("Đà Nẵng")
                .startAt(now + 12 * DAY + 16 * 3600_000L)
                .endAt(now + 12 * DAY + 22 * 3600_000L)
                .priceFrom(0L)
                .availability(EventUiModel.Availability.AVAILABLE)
                .category(EventCategory.FOOD)
                .rating(4.5f)
                .reviewCount(302)
                .addTag("ẩm thực")
                .addTag("đà nẵng")
                .build());

        events.add(new EventUiModel.Builder()
                .id(6)
                .title("Startup Demo Day mùa 4")
                .organizer("Innovation Hub")
                .venue("Trung tâm Hội nghị GEM")
                .city("TP. Hồ Chí Minh")
                .startAt(now + 7 * DAY + 13 * 3600_000L)
                .endAt(now + 7 * DAY + 17 * 3600_000L)
                .priceFrom(500_000L)
                .availability(EventUiModel.Availability.SOLD_OUT)
                .category(EventCategory.BUSINESS)
                .rating(4.4f)
                .reviewCount(57)
                .addTag("startup")
                .build());

        events.add(new EventUiModel.Builder()
                .id(7)
                .title("Workshop Nhiếp ảnh Mobile")
                .organizer("Creative Lab")
                .venue("Vườn ươm Sáng tạo")
                .city("Hà Nội")
                .startAt(now + 3 * DAY + 14 * 3600_000L)
                .endAt(now + 3 * DAY + 17 * 3600_000L)
                .priceFrom(200_000L)
                .availability(EventUiModel.Availability.AVAILABLE)
                .category(EventCategory.EDUCATION)
                .rating(4.7f)
                .reviewCount(41)
                .addTag("nhiếp ảnh")
                .addTag("workshop")
                .build());

        events.add(new EventUiModel.Builder()
                .id(8)
                .title("Yoga & Sống khỏe mỗi sáng")
                .organizer("Green Life")
                .venue("Công viên Thống Nhất")
                .city("Hà Nội")
                .startAt(now + 2 * DAY + 6 * 3600_000L)
                .endAt(now + 2 * DAY + 8 * 3600_000L)
                .priceFrom(0L)
                .availability(EventUiModel.Availability.AVAILABLE)
                .category(EventCategory.HEALTH)
                .rating(4.8f)
                .reviewCount(76)
                .addTag("yoga")
                .build());

        events.add(new EventUiModel.Builder()
                .id(9)
                .title("Đêm nhạc Acoustic Hà Nội")
                .organizer("Hanoi Acoustic")
                .venue("Nhà hát Lớn Hà Nội")
                .city("Hà Nội")
                .startAt(now + 16 * DAY + 20 * 3600_000L)
                .endAt(now + 16 * DAY + 22 * 3600_000L)
                .priceFrom(450_000L)
                .availability(EventUiModel.Availability.FEW_LEFT)
                .remainingTickets(18)
                .category(EventCategory.MUSIC)
                .rating(4.9f)
                .reviewCount(188)
                .aiScore(89)
                .aiReason("Cùng gu âm nhạc acoustic với bạn.")
                .addTag("acoustic")
                .build());

        events.add(new EventUiModel.Builder()
                .id(10)
                .title("Hội chợ Sách Thiếu nhi")
                .organizer("NXB Trẻ")
                .venue("Phố đi bộ Nguyễn Huệ")
                .city("TP. Hồ Chí Minh")
                .startAt(now + 20 * DAY + 8 * 3600_000L)
                .endAt(now + 20 * DAY + 18 * 3600_000L)
                .priceFrom(0L)
                .availability(EventUiModel.Availability.AVAILABLE)
                .category(EventCategory.EDUCATION)
                .rating(4.6f)
                .reviewCount(93)
                .addTag("sách")
                .addTag("thiếu nhi")
                .build());

        events.add(new EventUiModel.Builder()
                .id(11)
                .title("Giải bóng rổ Sinh viên 2026")
                .organizer("University League")
                .venue("Nhà thi đấu Phú Thọ")
                .city("TP. Hồ Chí Minh")
                .startAt(now + 11 * DAY + 18 * 3600_000L)
                .endAt(now + 11 * DAY + 21 * 3600_000L)
                .priceFrom(150_000L)
                .availability(EventUiModel.Availability.AVAILABLE)
                .category(EventCategory.SPORTS)
                .rating(4.5f)
                .reviewCount(112)
                .addTag("bóng rổ")
                .build());

        events.add(new EventUiModel.Builder()
                .id(12)
                .title("Triển lãm Công nghệ 2026")
                .organizer("Future Expo")
                .venue("Trực tuyến")
                .city("Hà Nội")
                .startAt(now + 30 * DAY + 9 * 3600_000L)
                .endAt(now + 30 * DAY + 18 * 3600_000L)
                .online(true)
                .priceFrom(0L)
                .availability(EventUiModel.Availability.AVAILABLE)
                .category(EventCategory.TECH)
                .rating(4.8f)
                .reviewCount(205)
                .aiScore(83)
                .aiReason("Sự kiện công nghệ được nhiều người có sở thích giống bạn quan tâm.")
                .addTag("công nghệ")
                .addTag("expo")
                .build());

        events.add(new EventUiModel.Builder()
                .id(13)
                .title("Đêm nhạc mở Acoustic Sài Gòn")
                .organizer("Open Stage")
                .venue("Phố đi bộ Nguyễn Huệ")
                .city("TP. Hồ Chí Minh")
                .startAt(now + 3 * 3600_000L)
                .endAt(now + 6 * 3600_000L)
                .priceFrom(0L)
                .availability(EventUiModel.Availability.AVAILABLE)
                .category(EventCategory.MUSIC)
                .rating(4.7f)
                .reviewCount(38)
                .addTag("acoustic")
                .addTag("âm nhạc")
                .build());

        return events;
    }
}
