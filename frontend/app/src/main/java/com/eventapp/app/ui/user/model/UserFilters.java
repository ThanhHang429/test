package com.eventapp.app.ui.user.model;

import androidx.annotation.Nullable;

/**
 * Trạng thái bộ lọc của tab Khám phá.
 *
 * <p>Giữ đúng các tham số mà API-03 chấp nhận để màn hình chỉ việc đổ thẳng vào
 * {@code EventRepository.SearchQuery}: ngày, giá, thành phố, danh mục và kiểu sắp xếp.</p>
 */
public class UserFilters {

    /** null = để server tự chọn (relevance khi có từ khoá, sắp diễn ra khi không). */
    public static final String SORT_AUTO = null;
    public static final String SORT_RELEVANCE = "RELEVANCE";
    public static final String SORT_UPCOMING = "START_TIME_ASC";
    public static final String SORT_NEWEST = "NEWEST";
    public static final String SORT_PRICE_ASC = "PRICE_ASC";
    public static final String SORT_PRICE_DESC = "PRICE_DESC";
    public static final String SORT_POPULAR = "POPULAR";

    /** yyyy-MM-dd, null = không giới hạn. */
    @Nullable
    public String dateFrom;
    @Nullable
    public String dateTo;

    /** FREE | PAID | null. */
    @Nullable
    public String priceType;
    @Nullable
    public Long priceMin;
    @Nullable
    public Long priceMax;

    @Nullable
    public String city;
    @Nullable
    public Long categoryId;

    @Nullable
    public String sort = SORT_AUTO;

    public void clear() {
        dateFrom = null;
        dateTo = null;
        priceType = null;
        priceMin = null;
        priceMax = null;
        city = null;
        categoryId = null;
        sort = SORT_AUTO;
    }

    public UserFilters copy() {
        UserFilters copy = new UserFilters();
        copy.dateFrom = dateFrom;
        copy.dateTo = dateTo;
        copy.priceType = priceType;
        copy.priceMin = priceMin;
        copy.priceMax = priceMax;
        copy.city = city;
        copy.categoryId = categoryId;
        copy.sort = sort;
        return copy;
    }

    /** Số nhóm điều kiện đang bật — dùng cho badge trên nút lọc. Sắp xếp không tính. */
    public int activeCount() {
        int count = 0;
        if (dateFrom != null || dateTo != null) {
            count++;
        }
        if (priceType != null || priceMin != null || priceMax != null) {
            count++;
        }
        if (city != null) {
            count++;
        }
        if (categoryId != null) {
            count++;
        }
        return count;
    }

    public boolean hasAny() {
        return activeCount() > 0 || sort != SORT_AUTO;
    }
}
