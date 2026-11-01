package com.eventapp.app.ui.fake;

import com.eventapp.app.R;
import com.eventapp.app.ui.model.CategoryUiModel;
import com.eventapp.app.ui.model.ContentRuleUiModel;
import com.eventapp.app.ui.model.FaqUiModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sample catalogue for FR-A09 while the backend endpoints are not wired yet.
 * Mirrors the shapes of {@code GET /admin/categories} and {@code GET /admin/content-rules}.
 */
public final class SampleCategoryData {

    private SampleCategoryData() {
    }

    public static List<CategoryUiModel> categories() {
        List<CategoryUiModel> items = new ArrayList<>();
        items.add(category(1, "Âm nhạc", "am-nhac", 0, R.drawable.ic_ds_event,
                R.color.ds_primary, CategoryUiModel.Status.ACTIVE, 12));
        items.add(category(11, "Nhạc sống", "nhac-song", 1, R.drawable.ic_ds_mic,
                R.color.ds_primary, CategoryUiModel.Status.ACTIVE, 7));
        items.add(category(12, "Hòa nhạc", "hoa-nhac", 1, R.drawable.ic_ds_star_filled,
                R.color.ds_primary, CategoryUiModel.Status.ACTIVE, 5));
        items.add(category(2, "Công nghệ", "cong-nghe", 0, R.drawable.ic_ds_chart,
                R.color.ds_secondary, CategoryUiModel.Status.ACTIVE, 9));
        items.add(category(21, "Hội thảo", "hoi-thao", 2, R.drawable.ic_ds_users,
                R.color.ds_secondary, CategoryUiModel.Status.ACTIVE, 6));
        items.add(category(22, "Workshop", "workshop", 2, R.drawable.ic_ds_edit,
                R.color.ds_secondary, CategoryUiModel.Status.ACTIVE, 3));
        items.add(category(3, "Thể thao", "the-thao", 0, R.drawable.ic_ds_ticket,
                R.color.ds_organizer, CategoryUiModel.Status.ACTIVE, 6));
        items.add(category(4, "Nghệ thuật", "nghe-thuat", 0, R.drawable.ic_ds_image,
                R.color.ds_info, CategoryUiModel.Status.HIDDEN, 2));
        items.add(category(5, "Ẩm thực", "am-thuc", 0, R.drawable.ic_ds_payment,
                R.color.ds_warning, CategoryUiModel.Status.ACTIVE, 4));
        return items;
    }

    public static List<ContentRuleUiModel> rules() {
        List<ContentRuleUiModel> items = new ArrayList<>();
        items.add(rule(101, "trúng thưởng", ContentRuleUiModel.Kind.KEYWORD,
                ContentRuleUiModel.MatchType.CONTAINS, Arrays.asList("EVENT_TEXT", "TAG"),
                ContentRuleUiModel.Severity.HIGH, true));
        items.add(rule(102, "chuyển khoản trước", ContentRuleUiModel.Kind.KEYWORD,
                ContentRuleUiModel.MatchType.CONTAINS, Collections.singletonList("EVENT_TEXT"),
                ContentRuleUiModel.Severity.HIGH, true));
        items.add(rule(103, "cam kết lợi nhuận", ContentRuleUiModel.Kind.KEYWORD,
                ContentRuleUiModel.MatchType.WORD, Arrays.asList("EVENT_TEXT", "REVIEW"),
                ContentRuleUiModel.Severity.MEDIUM, true));
        items.add(rule(104, "zalo", ContentRuleUiModel.Kind.KEYWORD,
                ContentRuleUiModel.MatchType.WORD, Arrays.asList("EVENT_TEXT", "TAG"),
                ContentRuleUiModel.Severity.MEDIUM, true));
        items.add(rule(105, "telegram", ContentRuleUiModel.Kind.KEYWORD,
                ContentRuleUiModel.MatchType.WORD, Collections.singletonList("EVENT_TEXT"),
                ContentRuleUiModel.Severity.LOW, false));
        items.add(rule(106, "vay tiền nhanh", ContentRuleUiModel.Kind.KEYWORD,
                ContentRuleUiModel.MatchType.CONTAINS, Collections.singletonList("REVIEW"),
                ContentRuleUiModel.Severity.HIGH, true));
        items.add(rule(201, "bit.ly", ContentRuleUiModel.Kind.DOMAIN,
                ContentRuleUiModel.MatchType.CONTAINS, Collections.emptyList(),
                ContentRuleUiModel.Severity.MEDIUM, true));
        items.add(rule(202, "tinyurl.com", ContentRuleUiModel.Kind.DOMAIN,
                ContentRuleUiModel.MatchType.CONTAINS, Collections.emptyList(),
                ContentRuleUiModel.Severity.MEDIUM, true));
        items.add(rule(203, "shopee.vn/voucher", ContentRuleUiModel.Kind.DOMAIN,
                ContentRuleUiModel.MatchType.CONTAINS, Collections.emptyList(),
                ContentRuleUiModel.Severity.LOW, false));
        return items;
    }

    public static List<FaqUiModel> faqs() {
        List<FaqUiModel> items = new ArrayList<>();
        items.add(new FaqUiModel(301, "Làm sao để hủy vé?",
                "Mở Vé của tôi, chọn vé cần hủy rồi bấm Hủy vé. Vé đã thanh toán sẽ được hoàn theo "
                        + "chính sách của từng sự kiện.", true, 1));
        items.add(new FaqUiModel(302, "Chính sách hoàn tiền",
                "Thời gian hoàn tiền từ 3 đến 7 ngày làm việc, tính từ lúc ban tổ chức chấp nhận "
                        + "yêu cầu hoàn.", true, 2));
        items.add(new FaqUiModel(303, "Cách check-in tại sự kiện",
                "Mở vé điện tử, đưa mã QR cho nhân sự soát vé. Mỗi mã chỉ có hiệu lực một lần.", true, 3));
        items.add(new FaqUiModel(304, "Liên hệ hỗ trợ",
                "Gửi yêu cầu trong mục Hỗ trợ của ứng dụng, bộ phận chăm sóc khách hàng phản hồi "
                        + "trong 24 giờ.", false, 4));
        return items;
    }

    private static CategoryUiModel category(long id, String name, String slug, long parentId,
                                            int iconRes, int colorRes,
                                            CategoryUiModel.Status status, int usageCount) {
        return new CategoryUiModel(id, name, slug, parentId, iconRes, colorRes, status, usageCount, 1);
    }

    private static ContentRuleUiModel rule(long id, String term, ContentRuleUiModel.Kind kind,
                                           ContentRuleUiModel.MatchType matchType, List<String> scopes,
                                           ContentRuleUiModel.Severity severity, boolean active) {
        return new ContentRuleUiModel(id, term, kind, matchType, scopes, severity, active);
    }
}
