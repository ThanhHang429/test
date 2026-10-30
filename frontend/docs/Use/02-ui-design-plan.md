# 02 — Kế hoạch thiết kế giao diện (Module Khám phá sự kiện, AI & Tích hợp nâng cao)

| | |
|---|---|
| Người phụ trách | Hữu Cảnh |
| Nguồn yêu cầu | `docs/Use/SRS – Module Khám phá sự kiện, AI & Tích hợp nâng cao (Hữu Cảnh).pdf` |
| Nguồn giao diện | `frontend/stitch/{User,Organizer,Admin}_screen.zip` |
| Bản đồ màn hình | `docs/Use/01-screen-map.md` |
| Trạng thái | Kế hoạch — dùng làm checklist triển khai |
| Cập nhật | 2026-10-30 |

Mục tiêu: với **15 FR** trong phạm vi (SRS §1.2), xác định rõ màn hình nào đã xong, màn hình nào còn
khung rỗng, màn hình nào cần **tự thiết kế** vì Stitch chưa có, và thứ tự triển khai để mỗi bước đều
chạy được độc lập.

---

## 1. Hiện trạng mã nguồn (đối chiếu thực tế)

Mọi màn hình trong `01-screen-map.md` đã tồn tại dưới dạng `Fragment` + `ViewModel` + layout, nhưng
phần lớn layout còn là **khung rỗng 6 dòng** (`FrameLayout` + comment `TODO`). Bảng dưới phân loại
theo đúng trạng thái file.

| FR | Màn hình (nav id) | Fragment / Layout | Trạng thái | Stitch tham chiếu |
|---|---|---|---|---|
| FR-U04 | `homeFragment`, `event_listFragment`, `event_filterFragment` | `user/home/HomeFragment`, `EventListFragment`, `EventFilterFragment` | **Đã thiết kế** (461 + ~557 dòng logic, `SearchHistoryStore`, `EventFilterCriteria`) | `kh_m_ph_ai_g_i_s_ki_n` |
| FR-U05 | `event_detailFragment` | `user/home/EventDetailFragment` | **Đã thiết kế** (tab Tổng quan/Loại vé/Đánh giá/Chính sách, offline + partial error) | `chi_ti_t_s_ki_n_ch_n_v` |
| FR-U20 | `write_reviewFragment`, `event_reviewsFragment` | `user/review/*` | **Đã thiết kế** (`ReviewAdapter`, `RatingDistributionView`, `SampleReviewData`) | `chi_ti_t_s_ki_n_ch_n_v` (khối đánh giá) |
| FR-O09 | `event_formFragment` | `organizer/event/EventFormFragment` + `ClassificationSectionView` | **Đã thiết kế** (khối phân loại) | `t_o_s_ki_n_b_c_1_...` (mục Danh mục) |
| FR-U06 | `favoritesFragment` | `user/favorite/FavoritesFragment` | **Đã thiết kế** (bộ lọc, chuông nhắc, item 4 trạng thái, hoàn tác) | `user_s_ki_n_y_u_th_ch_l_u` |
| FR-U07 | `recommendFragment` (+ mục "Dành cho bạn" ở Home) | `user/home/RecommendFragment` | **Đã thiết kế** (lý do, nguồn AI/RULE, quota làm mới, ẩn gợi ý) | `kh_m_ph_ai_g_i_s_ki_n` |
| FR-U03 | `preferencesFragment` | `user/profile/PreferencesFragment` | **Đã thiết kế** (chip danh mục 10, hình thức, ngân sách, 3 thành phố, ngày, xử lý 409) | **Chưa có Stitch → tự thiết kế** |
| FR-U08 | **chưa có destination** | — | Chưa có màn hình trong skeleton | `eticket_u08_ai_tr_l_t_v_n_s_ki_n` |
| FR-O03 | `organizer_dashboardFragment` | `organizer/dashboard/*` | **Đã thiết kế** (chu kỳ 7/30/tùy chọn, khối tài chính tách đã nhận/đang chờ, trạng thái 2 chiều, việc cần xử lý, top sự kiện) | `organizer_qu_n_l_s_ki_n_doanh_thu` |
| FR-O06 | `ai_descriptionFragment` (+ nút AI trong form) | `organizer/event/AiDescriptionFragment` | **Đã thiết kế** (gate dữ liệu tối thiểu, giọng/độ dài/số phương án, tab 1–3, hậu kiểm + placeholder, thay thế/chèn/thoác tác) | `t_o_s_ki_n_b_c_1_...ai_copilot` |
| FR-O27 | `ai_insightFragment` | `organizer/report/AiInsightFragment` | **Đã thiết kế** (picker sự kiện, KPI từ metrics, chart, tóm tắt AI/RULE, gợi ý có độ tin cậy, thiếu dữ liệu, quota) | `organizer_b_o_c_o_ph_n_t_ch_doanh_thu` |
| FR-A07 | `content_moderationFragment` | `admin/event/ContentModerationFragment` | **Đã thiết kế** (2 tab A07/A14, KPI, filter, card cờ, confirm/dismiss/rescan, snackbar hoàn tác, skeleton/empty/error) | `admin_x_l_b_o_c_o_vi_ph_m_light`, `admin_h_ng_i_ki_m_duy_t_s_ki_n_light` |
| FR-A09 | `category_manageFragment` | `admin/category/CategoryManageFragment` | Khung rỗng | **Chưa có → tự thiết kế** |
| FR-A14 | **gộp vào `content_moderationFragment`** (xem §5.3) | `admin/event/ContentModerationFragment` | **Đã thiết kế** (tab đánh giá bị báo cáo, bottom sheet chi tiết, ẩn/giữ/yêu cầu sửa kèm mã lý do + ghi chú) | `admin_x_l_b_o_c_o_vi_ph_m_light` |
| FR-A20 | `integration_healthFragment` | `admin/system/IntegrationHealthFragment` | Khung rỗng | `admin_gi_m_s_t_h_th_ng_ki_m_to_n_light` |

**Kết luận:** 12/15 FR đã có giao diện thật (U03, U04, U05, U06, U07, U20, O03, O06, O09, O27,
A07, A14). 2 FR còn khung rỗng (A09, A20), 1 FR (U08) chưa có màn hình. Kế hoạch dưới đây chỉ làm
các hạng mục còn lại, không sửa lại phần đã xong.

---

## 2. Ngôn ngữ thiết kế bắt buộc dùng lại

Nguồn: `stitch/.../dynamic_pulse/DESIGN.md` đã được ánh xạ sẵn vào token của app. **Không thêm màu,
font hay dependency mới**; mọi màn hình mới chỉ dùng những gì đã có:

| Hạng mục | Dùng gì |
|---|---|
| Màu | `res/values/colors_ds.xml` (`ds_primary`, `ds_cta` cho CTA chuyển đổi, `ds_success/warning/danger/_soft`, `ds_ai_gradient_*`, `ds_organizer*`, `ds_admin_risk/medium/low`, `ds_skeleton`) |
| Chữ | `TextAppearance.EventApp.Ds.{Display,H1,H2,H3,BodyL,BodyM,Caption,Label,LabelSmall,Price}` trong `styles_ds.xml` |
| Khoảng cách / bo góc | `dimens_ds.xml` (lưới 8dp: `ds_content_margin` 16dp, `ds_card_gap` 12dp, `ds_section_gap` 24dp, `ds_radius_12/16/20`, `ds_touch_target` 48dp) |
| Card / button / chip / input | `Widget.EventApp.Ds.Card{,.Outlined,.Soft}`, `Button.Primary/Cta/Outlined/Text`, `Chip.Filter/Preset/Tag`, `TextInputLayout` |
| Chuỗi | `res/values/strings_huucanh.xml`, prefix `hc_` — không thêm chuỗi tiếng Việt hard-code trong layout |
| View tái dùng | `AiBadge`, `StatusChip`, `SectionHeaderView`, `EmptyStateView`, `ErrorStateView`, `OfflineBanner`, `SkeletonLayout`, `StarRatingView`, `RatingDistributionView`, `PriceTextView`, `SimpleBarChartView`, `ProgressMetricView`, `BudgetInputView`, `ClassificationSectionView`, `RoleGuardLayout` |
| Adapter đã có | `EventAdapter`, `FavoriteAdapter`, `CategoryAdapter`, `AiInsightAdapter`, `ReviewAdapter`, `EventTicketAdapter`, `EventGalleryAdapter` |
| Drawable nền | `bg_ds_card`, `bg_ds_card_soft`, `bg_ds_ai_gradient`, `bg_ds_search`, `bg_ds_sheet`, `bg_ds_chip`, `bg_ds_success_soft`, `bg_ds_error_soft`, `bg_ds_info_soft`, `bg_ds_skeleton`, bộ `ic_ds_*` |

**Quy ước bắt buộc cho mọi màn hình mới** (theo `00-overview.md` §5 và cách `EventDetailFragment` đang làm):

1. Đủ 4 trạng thái qua `SkeletonLayout` / nội dung / `EmptyStateView` / `ErrorStateView`.
2. `SwipeRefreshLayout` + `OfflineBanner` khi có cache; nhãn "Cập nhật lúc hh:mm".
3. Nội dung do AI sinh luôn có `AiBadge`/nhãn nguồn (`hc_ai_badge`, `hc_ai_badge_rule`, `hc_ai_disclaimer`).
4. ViewModel **không** giữ `Context`; dữ liệu mẫu tách riêng theo kiểu `SampleEventData` / `SampleReviewData`.
5. Mọi action nguy hiểm/không khả dụng phải có `disabledReason` hiển thị, không ẩn nút mà không giải thích.

---

## 3. Bản đồ Stitch → chức năng

Đường dẫn thư mục trong zip (đã giải nén tạm ở `frontend/stitch/_extracted/` để đọc, có thể bỏ sau):

| Thư mục Stitch | Dùng cho | Ghi chú chuyển thể |
|---|---|---|
| `chi_ti_t_s_ki_n_ch_n_v` | U05 (đã dùng) | Hero gradient, pill ngày giờ, card vé, sticky bottom bar |
| `kh_m_ph_ai_g_i_s_ki_n` | U04, **U07** | Khối "AI Gợi ý cho bạn" + chip `AI Match 98%` + dòng lý do + "Làm mới" |
| `user_s_ki_n_y_u_th_ch_l_u` | **U06** | 3 filter chip (Tất cả/Đang mở bán/Sắp diễn ra), card có nút hành động theo trạng thái, carousel "Có thể bạn cũng thích" |
| `eticket_u08_ai_tr_l_t_v_n_s_ki_n` | **U08** | Bong bóng chat, thẻ sự kiện nhúng, quick reply, composer có nút gửi |
| `t_o_s_ki_n_b_c_1_th_ng_tin_chung_ai_copilot_organizer_teal` | **O06**, O09 | Khối "AI Copilot" với đề xuất + `Áp dụng / Tùy chỉnh / Sinh lại`; bảng màu teal → map sang `ds_organizer*` |
| `organizer_qu_n_l_s_ki_n_doanh_thu` | **O03** | KPI card, danh sách sự kiện kèm doanh thu, segmented thời gian |
| `organizer_b_o_c_o_ph_n_t_ch_doanh_thu_organizer_teal` | **O27** | KPI + biểu đồ + "Cơ cấu hạng vé" + khuyến nghị; dùng `SimpleBarChartView` |
| `admin_x_l_b_o_c_o_vi_ph_m_light` | **A07, A14** | Thẻ sự vụ theo mức ưu tiên, khối bằng chứng, nút hành động (Xác nhận/Bỏ qua) |
| `admin_h_ng_i_ki_m_duy_t_s_ki_n_light` | A07 (huy hiệu trong hàng đợi) | Chỉ lấy ngôn ngữ huy hiệu/mức độ, không lấy layout hàng đợi (thuộc Châu) |
| `admin_gi_m_s_t_h_th_ng_ki_m_to_n_light` | **A20** | Header trạng thái, tile KPI, danh sách dịch vụ với dot trạng thái, nhật ký, panel cấu hình |
| `eticket_u19_trung_t_m_th_ng_b_o`, `eticket_u09`, `eticket_u16`, `v_c_a_t_i_e_ticket_check_in`, `thanh_to_n_qr_gi_ch`, `ch_nh_s_a_h_s_i_m_t_kh_u`, `catch_house_*` | Không thuộc phạm vi | Của Quốc Anh / Trâm / Hằng |
| `organizer_tr_m_check_in_qr...`, `eticket_o22_...`, `admin_th_m_nh_h_s_ban_t_ch_c_light`, `eticket_a01_...`, `eticket_a04_...` | Không thuộc phạm vi | Trâm / Châu / dashboard admin chung |

**Không có Stitch, phải tự thiết kế (dựa trên ngôn ngữ Dynamic Pulse):** U03 Sở thích, A09 Danh mục &
từ khóa cấm, phần "Đánh giá của tôi" trong A14. Nguyên tắc: giữ đúng lưới 8dp, card `ds_radius_16`,
CTA `ds_primary` (không dùng `ds_cta` vì đây không phải luồng mua), chip `Widget.EventApp.Ds.Chip.Filter`.

---

## 4. Đặc tả thiết kế từng màn hình còn lại

Mỗi mục gồm: cấu trúc khối, hành vi, trạng thái, chuỗi cần thêm. Tên file giữ đúng quy ước
`fragment_<area>_<snake>.xml` / `item_<snake>.xml`.

### 4.1 FR-U06 — Yêu thích (`fragment_favorite_favorites.xml`)

Tham chiếu `user_s_ki_n_y_u_th_ch_l_u`. Đây là **tab đã lưu**, không phải màn hình khám phá thứ hai.

1. Top bar: tiêu đề "Đã Lưu" + số mục (`3 sự kiện đã lưu`) + icon chia sẻ danh sách (disable khi rỗng).
2. `MaterialSwitch` banner "Chuông báo sự kiện": nhắc mở bán/giảm giá/sắp hết vé — chỉ lưu cục bộ,
   ghi rõ trạng thái trong `strings`.
3. `ChipGroup` lọc `ALL / UPCOMING / PAST` (U06-R04) + `TextInputLayout` tìm trong danh sách đã lưu.
4. `RecyclerView` `item_favorite.xml`: ảnh bìa 16:9 + huy hiệu trạng thái (`Miễn phí`, `Còn 18/120 chỗ`,
   `Sắp mở bán 12:00 ngày mai`, `Đã hủy`, `Đã kết thúc`), pill giờ, tiêu đề 2 dòng, địa điểm, giá, tim,
   CTA phụ thuộc trạng thái (`Đặt ngay` / `Đăng ký miễn phí` / `Nhận thông báo mở bán`).
5. Sự kiện `SUSPENDED/UNPUBLISHED`: dòng xám "Sự kiện không còn khả dụng", **không** lộ tên/ảnh, chỉ
   nút "Xóa khỏi danh sách" (U06 + SRS luồng ngoại lệ).
6. `EmptyStateView`: "Chưa có sự kiện nào được lưu" + nút "Đi khám phá" → `event_listFragment`.
7. Cuối danh sách: `SectionHeaderView` "Có thể bạn cũng thích" + carousel `item_event_featured` (tái dùng U07).

Hành vi: tim = optimistic UI rồi gọi API; lỗi thì hoàn tác + Snackbar; khách bấm tim → điều hướng đăng
nhập rồi quay lại đúng màn (U06). Ngắt mạng: chỉ xem, ẩn nút xóa.
Chuỗi mới: `hc_fav_*` (title, count, switch, filter_all/upcoming/past, unavailable, remove, empty_*).
VM: `FavoritesViewModel` cần `LiveData<Boolean> loading/error`, `List<FavoriteUi> items`, `filter`, `query`,
`LiveData<Integer> removeResult`. Adapter: `FavoriteAdapter` (đã có) — bổ sung trạng thái unavailable.

### 4.2 FR-U07 — AI gợi ý (`fragment_home_recommend.xml`) + mục "Dành cho bạn" ở Home

1. Header: "Gợi ý bởi AI" (`AiBadge`) + `generatedAt` + nút "Làm mới" (rate-limit 1 lần/phút → disable
   kèm tooltip), subtitle nêu rõ nguồn: "Dựa trên sở thích và hoạt động của bạn" hoặc fallback
   "Phổ biến sắp diễn ra" khi `personalized = false` (U07 nhánh người mới).
2. Chip sở thích tóm tắt (danh mục, hình thức, ngân sách) + nút "Đổi sở thích" → `preferencesFragment`.
3. Danh sách thẻ `item_event_recommend.xml` (mới, dựa trên `item_event.xml` + dòng lý do): ảnh, chip
   `hc_ai_match`/`hc_ai_badge_rule`, lý do ≤ 140 ký tự, giá/ngày **lấy từ dữ liệu chính thức**, menu
   "Không quan tâm" (Snackbar hoàn tác), chỉ mở được `event_detailFragment` (U07-R02).
4. Nhãn fallback: `isFallback = true` → chip xám "Gợi ý theo luật" + dòng "Trợ lý AI tạm thời không khả dụng".
5. Skeleton 3 thẻ khi tải; lỗi → **ẩn cả mục** ở Home, ở màn riêng thì `ErrorStateView` + thử lại
   (U07-R04, NFR-03).
6. Empty: "Chưa có gợi ý nào" + nút sang `event_listFragment`.
Chuỗi mới: `hc_rec_*` (title_personalized, title_popular, refresh, change_prefs, reason_source_ai/rule,
not_interested, undo, fallback_note, empty_*). VM: `RecommendViewModel` + `RecommendationUiModel` (reason,
reasonSource) — thêm hằng `RULE/AI` theo enum server.

### 4.3 FR-U03 — Sở thích (`fragment_profile_preferences.xml`) — *tự thiết kế*

Bố cục 1 cột, cuộn, sticky bottom bar 2 nút `Đặt lại` (Text) + `Lưu` (Primary):

1. Header "Sở thích của bạn" + caption "Dùng để cá nhân hóa gợi ý. Bạn có thể bỏ qua."
2. `SectionHeaderView` "Danh mục quan tâm" + đếm `3/10`; `ChipGroup` filter-chip lấy từ API-02, mỗi chip
   có icon/màu theo `EventCategoryUi`; quá 10 → chip còn lại `alpha 0.4` + Snackbar (U03 + tiêu chí).
3. "Hình thức": segmented 3 lựa chọn `ANY / ONSITE / ONLINE` dạng chip đơn chọn.
4. "Mức giá": chip `ANY / FREE_ONLY / PAID_ONLY` + `BudgetInputView` (đã có) hiện khi `PAID_ONLY`/`ANY`,
   ẩn khi `FREE_ONLY`; presets 200k/500k/1M/2M/5M; validate 0–100.000.000 (`hc_budget_invalid`).
5. "Thành phố": chip chọn tối đa 3 từ `hc_cities`.
6. "Thời gian": chip `WEEKDAY / WEEKEND` (mặc định cả hai).
7. Trạng thái: skeleton khi tải danh mục; lỗi danh mục → chỉ khối danh mục báo lỗi, các khối khác vẫn sửa
   được (U03-R07); danh mục bị ẩn → chip gạch ngang + ghi chú "Danh mục đã ẩn" (U03-R03).
8. Lưu: khoá nút khi đang gửi, chỉ báo "Đã lưu" sau khi có 200 (U03 luồng ngoại lệ); 409 → dialog "Giữ
   bản của bạn / Dùng bản mới nhất" (`dialog_confirm.xml` tái dùng).
Chuỗi mới: `hc_pref_*`. VM: `PreferencesViewModel` với `PreferencesUiState` (danh sách chip, selection,
version, loading/error/saving), `save()`/`reset()`/`skip()`.

### 4.4 FR-U08 — Chatbot (`eticket_u08_ai_tr_l_t_v_n_s_ki_n`) — *cần quyết định màn hình*

Stitch có thiết kế nhưng **skeleton chưa có destination**. Hai lựa chọn:

- **A (đề xuất):** thêm `ui/user/chat/AiChatFragment` + `fragment_chat_ai_chat.xml` + `item_chat_message.xml`,
  nav id `ai_chatFragment`, mở bằng FAB "AI Trợ lý" nổi trên `homeFragment` (đúng Stitch).
- **B:** chỉ làm bottom sheet trong Home, không thêm destination — ít rủi ro hơn nhưng khó tái sử dụng.

→ Cần người phụ trách duyệt trước khi sửa `nav_user.xml` (AGENTS `FLOW-04`). Chỉ triển khai sau khi
DQ-11 được chốt, vì SRS xếp FR-U08 là "Mở rộng / Thấp" và phụ thuộc quyết định mở tính năng.

Thiết kế (nếu duyệt): app bar có avatar AI + "Trực tuyến" + nút xóa hội thoại (`hc_chat_clear`); danh
sách message (bong bóng user `ds_primary`, AI `ds_surface` viền); thẻ sự kiện nhúng dùng lại
`item_event_featured` thu gọn; hàng quick reply chip; "đang soạn" 3 chấm + nút dừng; composer 48dp có
nút gửi tròn; footer ghi chú `hc_ai_disclaimer` dưới mỗi câu trả lời; tối đa 50 tin lưu cục bộ.

### 4.5 FR-O03 — Dashboard Organizer (`fragment_dashboard_organizer_dashboard.xml`)

Tham chiếu `organizer_qu_n_l_s_ki_n_doanh_thu`, đổi teal → `ds_organizer*`.

1. App bar "Tổng quan" + chu kỳ (7 ngày / 30 ngày / tùy chọn) + `asOf`.
2. Banner trạng thái hồ sơ: `APPROVED` (ẩn) / `SUSPENDED` (đỏ, chỉ xem) / `PENDING/REJECTED/MORE_INFO`
   (khối hướng dẫn + nút sang `application_statusFragment`).
3. Lưới 2×2 KPI `ProgressMetricView`: "Đã nhận" (verified, `ds_success`), "Đang chờ" (**luôn tách riêng**,
   `ds_warning`, O03-R02), "Vé đã phát hành", "Check-in".
4. "Tình trạng sự kiện": chip đếm theo cặp `reviewStatus` × `lifecycleStatus` (O03-R04) —
   DRAFT, PENDING_REVIEW, REVISION_REQUIRED, REJECTED, PUBLISHED, SUSPENDED, CANCELLED, COMPLETED.
5. "Việc cần xử lý" (O03-R07): list ưu tiên — sự kiện trong 48h, yêu cầu hoàn tiền, sự kiện cần sửa;
   mỗi dòng có icon mức khẩn + mũi tên sang màn tương ứng.
6. "Top sự kiện": 5 dòng (tên, doanh thu đã xác minh, số đăng ký) — chỉ định dạng số, không tự cộng (O03-R03).
7. Mỗi khối có `status` riêng (OK/ERROR/STALE): lỗi tài chính chỉ làm đỏ khối tài chính, kèm nút thử lại
   (O03-R05); offline → `OfflineBanner` + chặn hành động tài chính (O03-R06).
Chuỗi mới: `hc_odash_*`. VM: `OrganizerDashboardViewModel` với `DashboardState` gồm từng khối + status +
asOf, dữ liệu mẫu trong `SampleDashboardData`.

**Đã triển khai (2026-10-30).** `KpiTileView` (mới, dùng chung với O27) hiển thị số lớn + delta + chú thích,
đồng thời có khối lỗi riêng với nút "Thử lại" → `retryMetric()` chỉ tải lại đúng khối đó. `MaterialDatePicker`
cho khoảng tùy chọn, chặn > 366 ngày ngay trên client. `DashboardTaskAdapter` (icon/mức khẩn riêng) và
`TopEventAdapter` (hạng + thanh tỷ trọng theo doanh thu cao nhất) điều hướng sang `my_eventsFragment`,
`organizer_refundsFragment`, `ticket_type_listFragment`, `ai_insightFragment`. Dữ liệu mẫu cố ý để khối
check-in ở trạng thái `ERROR` lần đầu để thấy đúng hành vi O03-R05; bấm "Thử lại" sẽ nạp lại thành công.

### 4.6 FR-O06 — AI viết mô tả (`fragment_event_ai_description.xml` + nút trong form)

Tham chiếu khối AI Copilot ở `t_o_s_ki_n_b_c_1_...` (đổi teal → `ds_ai_gradient_*`).

1. Trong `EventFormFragment`: nút "Viết mô tả bằng AI" (`ic_ds_ai_sparkle`) cạnh ô mô tả, disable + lý do
   khi thiếu dữ liệu tối thiểu (tiêu đề, danh mục, ≥ 3 ý chính) — chỉ rõ trường còn thiếu.
2. `ai_descriptionFragment` mở dạng bottom sheet/full screen 2 phần:
   - Panel điều kiện: giọng văn (Chuyên nghiệp/Thân thiện/Trang trọng), độ dài (Ngắn/Vừa/Dài), số phương
     án (1–3) — chip đơn chọn, mặc định `PROFESSIONAL / MEDIUM / 1`.
   - `TabLayout` "Phương án 1..3" + nội dung preview trong card, `hc_ai_disclaimer` + `AiBadge`.
3. Vùng `warnings[]`: card `bg_ds_error_soft` "Cần kiểm tra: số liệu/ngày/liên kết không có trong dữ liệu
   bạn nhập" (O06-R03). `missingInfo[]`: card `bg_ds_warning_soft` "Thông tin còn thiếu" — placeholder
   `[Cần bổ sung: ...]` được tô nền trong preview.
4. Nút: `Dùng bản này` (Primary) / `Sinh lại` (Outlined) / `Hoàn tác` (Text); đang tạo → tiến trình + nút
   `Hủy` (timeout 25s); lỗi → "Không tạo được mô tả, bạn vẫn có thể nhập tay" và form giữ nguyên (O06 nhánh lỗi).
5. Nhãn trong ô mô tả: "Do AI soạn, hãy kiểm tra lại" — tự mất khi người dùng sửa và xác nhận (O06-R05).
Chuỗi mới: `hc_aidesc_*`. VM: `AiDescriptionViewModel` (tone, length, variants, state REQUESTING/SUCCESS/
ERROR, warnings, missingInfo, selectedVariant, undoStack).

**Đã triển khai (2026-10-30).** Form có card "Mô tả sự kiện" mới: ô tiêu đề (counter 120, lỗi khi 0< len <5),
hình thức (Trực tiếp/Trực tuyến), ô nhập ý chính + chip tag đóng được + chip gợi ý (`hc_aidesc_keypoint_ideas`,
tối đa 10, mỗi ý 3–200 ký tự), ô mô tả và nút AI. `EventFormViewModel.AiGate` chặn nút khi thiếu tiêu đề /
danh mục chính / ít nhất 3 ý chính và hiện đúng lý do bên cạnh nút (O06 điều kiện trước).
`ai_descriptionFragment` mở với `init` snapshot từ form, tự tạo lần đầu, chip `Đang tạo…` + `Hủy` (guard 25s),
`TabLayout` cho 1–3 phương án, preview tô nền mọi chỗ `[Cần bổ sung: …]`, card "Thông tin còn thiếu" và card
"Cần kiểm tra" (hậu kiểm số/ngày/điện thoại/URL bằng regex — `SampleAiDescriptionData.postCheck`, nhánh SHORT
cố ý chứa số lạ để thấy cảnh báo), card lỗi/từ chối/hết quota (từ chối chặn từ cấm, quota 10 lượt/giờ).
Trả kết quả qua `SavedStateHandle` của back-stack trước; form hỏi "Thay thế hay chèn thêm?" rồi hiện Snackbar
"Hoàn tác"; nhãn "Do AI soạn, hãy kiểm tra lại" tự mất ngay lần sửa đầu tiên (O06-R05).

### 4.7 FR-O27 — AI phân tích (`fragment_report_ai_insight.xml`)

Tham chiếu `organizer_b_o_c_o_ph_n_t_ch_doanh_thu`.

1. Header: tên sự kiện + "Phân tích lúc hh:mm" (`generatedAt`) + chip nguồn `AI` / `RULE` (O27-R05).
2. Hàng thẻ `metrics`: lấp đầy %, số đăng ký, doanh thu, tỷ lệ check-in — **chỉ** lấy từ `metrics`
   (O27-R01), dùng `ProgressMetricView`.
3. `SimpleBarChartView` chuỗi đăng ký theo ngày (đã có view, sự kiện rỗng có `hc_chart_empty`).
4. `summary`: 2–4 đoạn, `TextAppearance.Ds.BodyM`, trong card `Card.Soft`.
5. Danh sách `insights[]` (`item_ai_insight.xml` đã có): chip `OBSERVATION`/`SUGGESTION`, text, nhãn độ
   tin cậy LOW/MEDIUM/HIGH (`StatusChip` tone MUTED/INFO/SUCCESS), link "Mở chỉnh sửa" sang màn tương ứng
   — **không có nút áp dụng tự động** (O27-R03).
6. `disclaimer` cố định + `dataLimitations[]` (`bg_ds_warning_soft`); `insufficientData = true` → `EmptyStateView`
   "Chưa đủ dữ liệu để phân tích" + nêu ngưỡng cần đạt, **không** hiện gợi ý.
Chuỗi mới: `hc_insight_*`. VM: `AiInsightViewModel` (metrics, summary, insights, source, disclaimer,
limitations, insufficientData, loading/error).

**Đã triển khai (2026-10-30).** `MaterialAutoCompleteTextView` đổi sự kiện tại chỗ; mỗi sự kiện là một
nhánh dữ liệu mẫu riêng: AI đầy đủ (Indie), fallback luật kèm `hc_insight_rule_notice` (Hội thảo), và
`insufficientData` (Workshop) — nhánh thiếu dữ liệu ẩn toàn bộ gợi ý nhưng vẫn hiện metrics + chart.
`AiInsightAdapter` gắn chip loại/độ tin cậy và **chỉ** mở màn chỉnh sửa, không có nút áp dụng. Quota 5 lượt/
sự kiện/ngày mở khóa lại khi đổi sự kiện; hết quota thì làm mờ nút làm mới và hiện ghi chú.

### 4.8 FR-A07 + FR-A14 — Kiểm duyệt nội dung & báo cáo (`fragment_event_content_moderation.xml`)

Tham chiếu `admin_x_l_b_o_c_o_vi_ph_m_light` + ngôn ngữ huy hiệu của `admin_h_ng_i_ki_m_duy_t_s_ki_n_light`.
Một màn hình, hai lane (TabLayout), vì cả A07 và A14 đều "Admin xử lý cờ/báo cáo" và skeleton chỉ có một
destination `content_moderationFragment`.

1. Hàng tổng quan: 3 tile (Cần xử lý / Mức cao / Đã xử lý hôm nay) dùng `ds_admin_risk/medium/low`.
2. Tab "Cờ nội dung" (A07): filter chip theo `targetType` (Sự kiện/Ảnh/Nhãn/Đánh giá), `severity`
   (HIGH/MEDIUM/LOW), `status` (OPEN/CONFIRMED/DISMISSED). Card `item_moderation_flag.xml`:
   - dòng đầu: tên đối tượng + loại + `severity` (`StatusChip` tone DANGER/WARNING/SUCCESS) + thời gian;
   - `reason` (≤300 ký tự) và `evidence` in nghiêng trong khối `bg_ds_error_soft` (A07-R02);
   - nguồn `RULE`/`AI` + `confidence`; nhãn "Chưa quét AI" khi `moderationStatus = UNCHECKED`;
   - nút `Xác nhận` (`ds_admin_risk`) / `Bỏ qua` (bắt buộc nhập ghi chú — `dialog_reason.xml`) / `Quét lại`.
3. Tab "Đánh giá bị báo cáo" (A14): danh sách đánh giá kèm huy hiệu nguồn (`HELD / FLAGGED / REPORTED`
   — chuỗi `hc_review_source_*` đã có) + mức độ; mở chi tiết bằng bottom sheet có nội dung đánh giá, tác
   giả rút gọn, mã sự kiện, điểm spam/toxicity (`hc_review_scores`), nút `Ẩn đánh giá` / `Giữ lại` /
   `Yêu cầu sửa` — chọn mã lý do trong `dialog_reason.xml` (A14 giao diện).
4. Cả hai tab: sau khi xử lý, item mờ dần và biến khỏi danh sách OPEN + Snackbar hoàn tác; mọi thao tác
   ghi audit (hiển thị note "Đã ghi nhật ký thao tác").
5. Trạng thái: skeleton, empty ("Không có cờ nào cần xử lý" + "Xem đã xử lý"), error + retry.
Chuỗi mới: `hc_mod_*`, `hc_a14_*`. VM: `ContentModerationViewModel` (tab, filters, list cờ, list đánh giá,
result live data cho confirm/dismiss/hide).

### 4.9 FR-A09 — Danh mục & nội dung cấu hình (`fragment_category_category_manage.xml`) — *tự thiết kế*

1. `TabLayout`: "Danh mục" / "Từ khóa cấm" / "Nội dung hướng dẫn" (đúng SRS A09 "Giao diện Android").
2. Tab Danh mục: danh sách dạng cây 2 cấp (`item_category.xml` mở rộng: indent + nút mũi tên), mỗi dòng
   có icon/màu, `slug`, chip `ACTIVE/HIDDEN`, `usageCount`, tay nắm kéo thả (ReorderableListView hoặc
   `ItemTouchHelper` — dùng lớp có sẵn trong AndroidX, không thêm dependency mới).
   - FAB "+" mở `dialog` form (tên, danh mục cha, icon, màu, thứ tự); sửa có `version`.
   - Ẩn/Hiện: dialog xác nhận nêu số sự kiện + số người dùng đang chọn (BR-020, A09-R01); ẩn cha → cảnh
     báo ẩn luôn con.
   - Kéo thả xong → gọi batch sort, Snackbar "Đã cập nhật thứ tự".
3. Tab Từ khóa cấm: ô thêm nhanh (từ/cụm từ + kiểu khớp CONTAINS/WORD + phạm vi EVENT_TEXT/TAG/REVIEW +
   mức độ), list có switch bật/tắt và chip phạm vi; sửa/xóa inline.
4. Tab Nội dung hướng dẫn (FAQ): list câu hỏi theo thứ tự, editor tiêu đề + nội dung + trạng thái hiển thị
   (dùng cho chatbot POLICY_FAQ).
5. Lỗi trùng tên 409 → Snackbar + focus ô tên; lệch version → dialog "Tải bản mới nhất".
Chuỗi mới: `hc_cat_*`. VM: `CategoryManageViewModel` (tab, tree, bannedKeywords, faqs, version, saving).

### 4.10 FR-A20 — Tình trạng tích hợp (`fragment_system_integration_health.xml`)

Bám `admin_gi_m_s_t_h_th_ng_ki_m_to_n_light`, bỏ phần tài chính (thuộc Hằng).

1. Header trạng thái tổng: dot `HEALTHY/DEGRADED/DOWN` + `lastUpdated` + chip "Realtime" (polling 30s nhẹ,
   dừng khi màn hình không hiển thị).
2. Tile KPI: uptime AI, tỷ lệ lỗi 5 phút, p95 latency, số lần gọi hôm nay (dùng `ProgressMetricView`).
3. Danh sách dịch vụ: AI Gateway (theo từng task RECOMMEND/DESCRIPTION/ANALYTICS/MODERATION), Payment
   webhook, FCM, SQLite sync — mỗi dòng: icon, tên, dot trạng thái, số liệu phụ, chip `Ổn định/Chậm/Lỗi`.
4. Bộ lọc thời gian (1h / 24h / 7 ngày) + `SimpleBarChartView` lỗi theo giờ.
5. "Nhật ký gần đây": list lỗi (time, provider, mã lỗi, đã fallback hay chưa) — không hiển thị dữ liệu
   người dùng (PRIV-01).
6. "Công tắc tác vụ AI": `MaterialSwitch` cho từng task, có dialog xác nhận, ghi rõ "Tắt sẽ dùng fallback
   theo luật" (A20 + FR-U07 fallback). Chỉ ADMIN được bật/tắt — dùng `RoleGuardLayout`.
7. Lỗi tải một khối → chỉ khối đó hiện "Không tải được, thử lại"; không có số liệu thì ẩn tile.
Chuỗi mới: `hc_health_*`. VM: `IntegrationHealthViewModel` (overall, metrics, services, recentErrors,
toggles, range, loading/error).

---

## 5. Thành phần dùng chung cần bổ sung

| Thành phần | Dùng ở | Ghi chú |
|---|---|---|
| `AiReasonRow` (TextView + `AiBadge`) | U07, O06, O27, U08 | tách từ `EventDetailFragment.bindHeader` |
| `SeverityChip` (map severity → `StatusChip.Tone`) | A07, A14 | tránh lặp `if` ở 2 màn |
| `KpiTileView` (số lớn + nhãn + delta) | O03, O27, A20 | **Đã có** — `view_ds_kpi_tile.xml` + trạng thái lỗi/retry từng khối |
| `ChatBubbleView` | U08 | chỉ khi U08 được duyệt |
| `CategoryTreeRow` / `ItemTouchHelper.Callback` | A09 | kéo thả, không thêm thư viện |
| `ChannelStatusRow` | A20 | dot trạng thái + số liệu |
| `item_event_recommend.xml`, `item_moderation_flag.xml` | U07, A07 | item layout mới |
| `dialog_preferences_conflict.xml` (hoặc tái dùng `dialog_confirm.xml`) | U03 | 409 version |

Không thêm item layout nếu có thể cấu hình bằng `item_event.xml` + cờ hiển thị (ưu tiên tái dùng).

---

## 6. Thứ tự triển khai (mỗi giai đoạn tự đứng được)

| Giai đoạn | Nội dung | Vì sao thứ tự này | Ước lượng |
|---|---|---|---|
| **P1** | U06 Yêu thích + U07 Gợi ý | Tái dùng nhiều nhất (`EventAdapter`, `item_event`, `HomeFragment`), hoàn tất cụm Khám phá | L |
| **P2** ✅ | U03 Sở thích — đã xong 2026-10-30 | Là đầu vào của U07/U08; cần dialog 409 — độc lập | M |
| **P3** ✅ | O03 Dashboard + O27 AI Insight — đã xong 2026-10-30 | Cùng bộ KPI/chart, cùng dữ liệu mẫu organizer | L |
| **P4** ✅ | O06 AI mô tả — đã xong 2026-10-30 | Cần mở rộng `EventFormFragment` (đang chỉ có khối O09) | M |
| **P5** | A07 + A14 kiểm duyệt → A09 danh mục → A20 tích hợp | Cụm admin, dùng chung `SeverityChip`/`KpiTileView` | L |
| **P6** (tùy chọn) | U08 Chatbot | Cần duyệt DQ-11 + thêm destination vào `nav_user.xml` | M |

Sau mỗi giai đoạn: cập nhật bảng §1 (trạng thái → "Đã thiết kế") và bổ sung mục chuỗi tương ứng trong
`strings_huucanh.xml`.

---

## 7. Kiểm tra & tiêu chí hoàn thành

Với mỗi màn hình:

1. `cd frontend && ./gradlew :app:assembleDebug` — biên dịch layout + Java, không lỗi resource.
2. `cd frontend && ./gradlew :app:lintDebug` — không thêm cảnh báo mới (đặc biệt `HardcodedText`,
   `ContentDescription`, `TouchTargetSizeCheck`).
3. `cd frontend && node scripts/verify-skeleton.mjs` — soát trùng tên class, layout, nav id. Lưu ý: script đang
   **đỏ sẵn 44 lỗi không liên quan** (quy tắc "mỗi file Java phải có TODO" đánh cả các file đã thiết kế, và
   bước kiểm tra XML gọi `powershell` nên lỗi `ENOENT` trên Linux). Không sửa các lỗi này trong phạm vi
   module; chỉ cần phần `nav graph / layout / Java / Api / DTO` vẫn báo `OK`.
4. Kiểm tra thủ công bằng dữ liệu mẫu: đủ 4 trạng thái (loading/success/empty/error), xoay màn hình,
   offline banner, và hành vi bị chặn có `disabledReason`.
5. Không có chuỗi tiếng Việt hard-code; mọi chữ nằm trong `strings_huucanh.xml` với prefix `hc_`.

Định nghĩa "Xong": layout có ID ổn định, Fragment bind + observe đầy đủ, ViewModel không giữ `Context`,
có dữ liệu mẫu riêng, không thêm dependency, không đổi kiến trúc/nav ngoài mục đã được duyệt.

---

## 8. Rủi ro & điểm cần chốt

| # | Vấn đề | Ảnh hưởng | Đề xuất |
|---|---|---|---|
| 1 | U08 chưa có destination trong `nav_user.xml` | Không thể làm UI đúng SRS nếu không sửa nav | Chốt phương án A/B ở §4.4; chỉ làm sau khi DQ-11 có kết luận |
| 2 | A14 không có màn hình riêng trong skeleton | Dễ làm trùng với `review_queueFragment` (của Châu) | Gộp vào `content_moderationFragment` 2 tab như §4.8 |
| 3 | Stitch dùng nhãn "CATCH HOUSE" và bảng teal cho Organizer | Lệch thương hiệu ETicket | Chỉ lấy bố cục; map màu sang `ds_organizer*`, chữ Việt hoá |
| 4 | Stitch không có U03, A09, "Đánh giá của tôi" | Phải tự thiết kế | Theo nguyên tắc §3 cuối; review nhanh với nhóm trước khi code |
| 5 | Số liệu AI (%, độ tin cậy) có thể bị hiểu là cam kết | Vi phạm BR-017/O27-R02 | Luôn hiển thị kèm `hc_ai_badge_estimate` / `disclaimer`; không dùng từ "chắc chắn" |
| 6 | Dữ liệu hiện là mẫu, chưa có API | Dễ lẫn "đã xong" với "đã nối API" | Mỗi màn ghi rõ nguồn dữ liệu mẫu trong KDoc như `EventDetailViewModel` |

---

## 9. Phụ lục — Tài nguyên đọc Stitch

- Zip gốc: `frontend/stitch/{User,Organizer,Admin}_screen.zip` (25 màn + `dynamic_pulse/DESIGN.md`).
- Bản giải nén tạm dùng để đọc: `frontend/stitch/_extracted/` (kèm `srs.txt` trích từ PDF).
  Đây là thư mục tạm, không commit; xoá khi không cần (cần xác nhận vì nằm trong repo).
