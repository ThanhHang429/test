# ETicket Backend — API Reference (Module Hữu Cảnh)

Tài liệu này mô tả **toàn bộ API đã triển khai** cho phạm vi module Hữu Cảnh
(FR-U03, U04, U05, U06, U07, U20, O03, O06, O09, O27, A07, A09, A14, A20).

- Base URL: `http://localhost:8080`
- Prefix: `/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Postman collection: `backend/postman/ETicket-Backend.postman_collection.json`
- Smoke test: `backend/scripts/api-smoke.sh`

---

## 1. Xác thực

### 1.1 JWT (đường chính)

```
Authorization: Bearer <jwt>
```

JWT HS256, claim `sub` = `userId`, claim `roles` = danh sách mã quyền
(`USER`, `ORGANIZER`, `ADMIN`). Thời hạn mặc định 24 giờ (`JWT_TTL`).

Ở profile `dev`, lấy token cho một tài khoản seed:

```bash
curl -X POST "http://localhost:8080/api/v1/dev/token?userId=10"
```

### 1.2 Đường tắt cho dev — `X-User-Id`

Bật bằng `app.security.dev-header-enabled=true` (mặc định `true` ở profile `dev`,
`false` ở mọi profile khác). Khi bật, request có header `X-User-Id: <id>` được coi
như đã đăng nhập bởi user đó, quyền đọc từ DB.

```bash
curl -H "X-User-Id: 10" http://localhost:8080/api/v1/me/preferences
```

- `X-User-Id` trỏ tới user **không tồn tại** → bị coi là **chưa đăng nhập** (401),
  không để lỗi FK nổ ở tầng DB.
- `Authorization: Bearer` được ưu tiên hơn `X-User-Id` nếu gửi cả hai.
- **Không dùng được ở profile `prod`.**

### 1.3 Tài khoản seed (profile `dev`, mật khẩu `Password123!`)

| userId | email | quyền | ghi chú |
|---|---|---|---|
| 7 | admin@eticket.local | ADMIN | duyệt nội dung, quản lý danh mục, giám sát tích hợp |
| 8 | organizer1@eticket.local | ORGANIZER | organizer APPROVED (profile 3), có 10 sự kiện |
| 9 | organizer2@eticket.local | ORGANIZER | organizer PENDING → dùng để test 403 |
| 10 | user1@eticket.local | USER | có sở thích + yêu thích seed |
| 11 | user2@eticket.local | USER | |
| 12 | user3@eticket.local | USER | đã check-in sự kiện COMPLETED → viết được đánh giá |

Danh sách đầy đủ: `GET /api/v1/dev/accounts`.

---

## 2. Định dạng phản hồi

### 2.1 Thành công

```json
{
  "success": true,
  "data": { },
  "meta": { "page": 0, "size": 20, "totalItems": 42, "hasNext": true },
  "correlationId": "8f0c2b1e-..."
}
```

- `meta` chỉ xuất hiện ở API phân trang.
- `correlationId` luôn có; nếu client gửi header `X-Correlation-Id` thì server
  dùng lại đúng giá trị đó và echo lại trong response header.

### 2.2 Lỗi

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Dữ liệu không hợp lệ",
    "details": [
      { "field": "rating", "message": "Số sao tối đa là 5" }
    ],
    "extra": { "retryAfterSeconds": 3600 }
  },
  "correlationId": "8f0c2b1e-..."
}
```

`details` và `extra` có thể vắng mặt. `extra` hiện dùng cho `retryAfterSeconds`
khi bị giới hạn tần suất.

### 2.3 Bảng mã lỗi

| HTTP | code | Ý nghĩa |
|---|---|---|
| 400 | `BAD_REQUEST` | Tham số/JSON không đọc được |
| 400 | `VALIDATION_ERROR` | Vi phạm ràng buộc dữ liệu (xem `details`) |
| 400 | `CATEGORY_DEPTH_EXCEEDED` | Danh mục chỉ hỗ trợ 2 cấp |
| 401 | `UNAUTHENTICATED` | Chưa đăng nhập |
| 401 | `TOKEN_EXPIRED` / `TOKEN_INVALID` | JWT hết hạn / sai |
| 403 | `FORBIDDEN` | Thiếu quyền |
| 403 | `EVENT_NOT_ENDED` | Đánh giá khi sự kiện chưa kết thúc |
| 403 | `NOT_ATTENDED` | Chưa check-in nên không được đánh giá |
| 403 | `REVIEW_WINDOW_CLOSED` | Quá 30 ngày kể từ khi sự kiện kết thúc |
| 404 | `NOT_FOUND` | Không tìm thấy tài nguyên |
| 404 | `EVENT_NOT_AVAILABLE` | Sự kiện không còn công khai |
| 404 | `CATEGORY_NOT_FOUND` | Danh mục không tồn tại |
| 409 | `CONFLICT` | Xung đột trạng thái chung |
| 409 | `EVENT_NOT_APPROVED` | Sự kiện chưa được duyệt |
| 409 | `CATEGORY_INACTIVE` | Danh mục đã bị ẩn |
| 409 | `CATEGORY_IN_USE` | Danh mục đang được dùng, không xoá được |
| 409 | `FAVORITE_LIMIT_REACHED` | Đã đạt 500 sự kiện yêu thích |
| 409 | `REVIEW_ALREADY_EXISTS` | Đã đánh giá sự kiện này |
| 409 | `REVIEW_EDIT_LIMIT` | Đã sửa 3 lần trong 7 ngày |
| 409 | `IDEMPOTENCY_CONFLICT` | Cùng `Idempotency-Key` nhưng nội dung khác |
| 429 | `RATE_LIMITED` | Vượt hạn mức (gợi ý làm mới, quota AI) |
| 500 | `INTERNAL_ERROR` | Lỗi hệ thống |
| 502 | `AI_INVALID_OUTPUT` | AI trả kết quả sai schema |
| 503 | `AI_UNAVAILABLE` | AI lỗi/timeout |
| 503 | `AI_DISABLED` | Tác vụ AI đang bị tắt bởi admin |

---

## 3. Nhóm API

| # | Nhóm | FR | Quyền |
|---|---|---|---|
| 3.1 | Dev helper | — | công khai (chỉ profile dev) |
| 3.2 | Danh mục công khai | U04 | công khai |
| 3.3 | Khám phá & tìm kiếm | U04, U05 | công khai (token tuỳ chọn) |
| 3.4 | Sở thích | U03 | USER |
| 3.5 | Yêu thích | U06 | USER |
| 3.6 | Đánh giá | U20 | công khai / USER |
| 3.7 | Kiểm duyệt | A14, A07 | ADMIN |
| 3.8 | Gợi ý AI | U07 | USER (preview: công khai) |
| 3.9 | AI cho Organizer | O06, O27 | ORGANIZER đã duyệt |
| 3.10 | Phân loại & nhãn | O09 | ORGANIZER (chủ sự kiện) |
| 3.11 | Dashboard Organizer | O03 | ORGANIZER |
| 3.12 | Giám sát tích hợp | A20 | ADMIN |
| 3.13 | Quản lý danh mục | A09 | ADMIN |

---

### 3.1 Dev helper (chỉ profile `dev`)

#### `GET /api/v1/dev/accounts`
Danh sách tài khoản seed kèm quyền. Dùng để tra `userId` cho `X-User-Id`.

```json
{ "success": true, "data": [
  { "userId": 10, "email": "user1@eticket.local", "fullName": "Nguyễn Văn An", "roles": ["USER"] }
]}
```

#### `POST /api/v1/dev/token?userId=10`
Cấp JWT cho `userId`. 404 nếu không có user.

```json
{ "success": true, "data": {
  "userId": 10, "email": "user1@eticket.local", "roles": ["USER"],
  "token": "eyJhbGciOi...", "tokenType": "Bearer", "expiresInSeconds": 86400
}}
```

---

### 3.2 Danh mục công khai

#### `GET /api/v1/categories`
Cây danh mục **đang hoạt động**, tối đa 2 cấp, sắp theo `sortOrder`.

```json
{ "success": true, "data": [
  { "id": 9, "code": "MUSIC", "name": "Âm nhạc", "slug": "am-nhac",
    "description": null, "parentId": null, "sortOrder": 1,
    "children": [ { "id": 15, "code": "LIVE_MUSIC", "name": "Nhạc sống", "...": "..." } ] }
]}
```

---

### 3.3 Khám phá & tìm kiếm sự kiện

Chỉ trả sự kiện `review_status = APPROVED`, `deleted_at IS NULL`,
và `lifecycle_status = PUBLISHED` (thêm `COMPLETED` khi `includePast=true`).
`CANCELLED` / `SUSPENDED` / `DRAFT` luôn bị loại (U04-R03).

#### `GET /api/v1/events`

| Tham số | Kiểu | Mặc định | Ghi chú |
|---|---|---|---|
| `q` | string | — | Tìm không dấu: `hoi nhac` khớp "Hội nhạc"/"Hội Nhạc" |
| `categoryIds` | long[] | — | Tối đa 10, khớp cả danh mục con |
| `dateFrom` / `dateTo` | yyyy-MM-dd | — | Theo múi giờ `Asia/Ho_Chi_Minh`, tối đa 366 ngày |
| `city` | string | — | Khớp tương đối |
| `format` | `OFFLINE`\|`ONLINE`\|`HYBRID` | — | |
| `priceType` | `FREE`\|`PAID` | — | |
| `priceMin` / `priceMax` | long | — | VND; `priceMin > priceMax` → 400 |
| `availableOnly` | bool | `false` | Chỉ sự kiện còn vé |
| `includePast` | bool | `false` | Bao gồm sự kiện đã kết thúc |
| `sort` | `RELEVANCE`\|`START_TIME_ASC`\|`NEWEST`\|`PRICE_ASC`\|`PRICE_DESC`\|`POPULAR` | `START_TIME_ASC` (có `q` → `RELEVANCE`) | |
| `page` | int | `0` | |
| `size` | int | `20` | **> 50 bị hạ về 50, không báo lỗi** (U04-R08) |

```bash
curl "http://localhost:8080/api/v1/events?q=hoi%20nhac&city=H%E1%BB%93%20Ch%C3%AD%20Minh&size=5"
```

Phần tử trong `data`:

```json
{
  "id": 14, "code": "EVT-...", "title": "Đêm nhạc Acoustic",
  "slug": "dem-nhac-acoustic", "shortDescription": "...", "coverUrl": "https://...",
  "startAt": "2026-12-20T13:00:00Z", "endAt": "2026-12-20T16:00:00Z",
  "timezone": "Asia/Ho_Chi_Minh", "city": "Hồ Chí Minh", "venueName": "Nhà hát Thành phố",
  "eventFormat": "OFFLINE",
  "categoryIds": [9, 15], "categoryNames": ["Âm nhạc", "Nhạc sống"], "tags": ["Acoustic"],
  "minPrice": 200000.00, "maxPrice": 500000.00, "isFree": false,
  "available": true, "availabilityUnknown": false,
  "saleStatus": "ON_SALE",
  "statusLabel": "Đang mở bán",
  "isFavorite": false,
  "reason": null, "reasonSource": null
}
```

`statusLabel` do server tính: `Đang mở bán`, `Sắp mở bán`, `Hết vé`, `Đã kết thúc bán`.
`reason` / `reasonSource` chỉ có ở API gợi ý (U07).

#### `GET /api/v1/events/{id}`
Chi tiết sự kiện. Token tuỳ chọn — nếu gửi, `isFavorite` / `registrationStatus` /
`allowedActions` được tính theo user đó.

Bổ sung so với thẻ: `description`, `mediaUrls`, `organizer`, `ticketTypes`,
`reviewSummary`, `canReview`, `allowedActions`, `disabledReason`, `serverTime`, `fetchedAt`.

- `allowedActions`: `REGISTER_FREE`, `BUY_TICKET`, `FAVORITE`, `UNFAVORITE`,
  `VIEW_MY_TICKET`, `WRITE_REVIEW` (U05-R02: server tính, app không tự suy luận).
- `disabledReason`: `SOLD_OUT`, `SALES_NOT_STARTED`, `SALES_ENDED`, `EVENT_CANCELLED`,
  `EVENT_COMPLETED`, `LOGIN_REQUIRED`, `ALREADY_REGISTERED`.
- `ticketTypes[].saleStatus`: `NOT_STARTED` | `ON_SALE` | `ENDED` | `SOLD_OUT`.
- Không tồn tại / chưa duyệt / đã huỷ → **404**.

---

### 3.4 Sở thích (FR-U03)

#### `GET /api/v1/me/preferences`
```json
{ "success": true, "data": {
  "categoryIds": [9, 15], "eventFormat": "ONSITE", "priceMode": "PAID_ONLY",
  "maxBudgetVnd": 500000, "preferredCities": ["Hồ Chí Minh"],
  "preferredDays": ["WEEKEND"], "onboardingCompleted": true,
  "personalizationEnabled": true, "version": 3, "removedCategoryIds": []
}}
```

#### `PUT /api/v1/me/preferences`
| Trường | Ràng buộc |
|---|---|
| `categoryIds` | ≤ 10 |
| `eventFormat` | `ANY` \| `ONSITE` \| `ONLINE` |
| `priceMode` | `ANY` \| `FREE_ONLY` \| `PAID_ONLY` |
| `maxBudgetVnd` | 0 … 100.000.000 |
| `preferredCities` | ≤ 3 (schema thật chỉ lưu **thành phố đầu tiên**) |
| `preferredDays` | `WEEKDAY` \| `WEEKEND` |
| `version` | gửi `version` hiện tại; lệch → **409** |

Idempotent: gửi lại cùng nội dung + cùng `version` không tăng version.
Lưu sở thích sẽ **xoá cache gợi ý** của user.

#### `DELETE /api/v1/me/preferences`
Đặt lại mặc định, trả về DTO sau khi reset.

---

### 3.5 Yêu thích (FR-U06)

#### `PUT /api/v1/me/favorites/{eventId}`
Idempotent. Chỉ thêm được sự kiện `APPROVED` + `PUBLISHED`; nếu không → 404.
Đạt 500 sự kiện → **409 `FAVORITE_LIMIT_REACHED`**.

```json
{ "success": true, "data": { "eventId": 14, "isFavorite": true } }
```

#### `DELETE /api/v1/me/favorites/{eventId}`
Idempotent, xoá khi chưa có vẫn trả 200 với `isFavorite: false`.

#### `GET /api/v1/me/favorites?filter=ALL|UPCOMING|PAST&page=0&size=20`
`filter` sai → 400. Mỗi phần tử:

```json
{ "eventId": 14, "available": true, "statusLabel": "Đang mở bán",
  "favoritedAt": "2026-11-01T02:00:00Z", "event": { "...EventCardDto": "..." } }
```

Thêm/xoá yêu thích cũng **xoá cache gợi ý**.

---

### 3.6 Đánh giá (FR-U20)

#### `GET /api/v1/events/{eventId}/reviews?sort=NEWEST|RATING_DESC|RATING_ASC&page&size`
Công khai, chỉ trả `VISIBLE`. Nếu đã đăng nhập, đánh giá của chính mình được
**ghim đầu danh sách** (U20-R03) và có `mine: true`.

```json
{ "id": 3, "authorName": "Lê Anh", "rating": 5, "text": "...",
  "createdAt": "2026-10-20T10:00:00Z", "mine": false, "edited": false }
```

Tên tác giả được rút gọn còn **họ + tên đệm đầu + tên** (che email/SĐT).

#### `GET /api/v1/events/{eventId}/reviews/summary`
```json
{ "averageRating": 4.5, "totalReviews": 2,
  "distribution": { "1": 0, "2": 0, "3": 0, "4": 1, "5": 1 },
  "highlights": [ { "id": 2, "authorName": "Phạm Hằng", "rating": 5, "text": "...", "createdAt": "..." } ] }
```

#### `POST /api/v1/events/{eventId}/reviews`
Header tuỳ chọn `Idempotency-Key` (cùng key + cùng nội dung → trả lại kết quả cũ;
cùng key + nội dung khác → **409 `IDEMPOTENCY_CONFLICT`**).

```json
{ "rating": 5, "text": "Sự kiện rất hay, tổ chức chu đáo." }
```

| Ràng buộc | Lỗi |
|---|---|
| `rating` 1…5 (bắt buộc), `text` ≤ 2000 | 400 |
| Sự kiện phải đã kết thúc | 403 `EVENT_NOT_ENDED` |
| Phải có check-in | 403 `NOT_ATTENDED` |
| Trong 30 ngày kể từ `endAt` | 403 `REVIEW_WINDOW_CLOSED` |
| Mỗi user 1 đánh giá / sự kiện | 409 `REVIEW_ALREADY_EXISTS` |

Nội dung chứa **từ khoá cấm, link, số điện thoại** → lưu ở trạng thái `PENDING`
(chờ admin duyệt) và trả `notice` giải thích; ngược lại `VISIBLE` ngay.

```json
{ "success": true, "data": {
  "review": { "id": 4, "status": "VISIBLE", "rating": 5, "...": "..." },
  "notice": "Đánh giá đã được đăng."
}}
```

#### `PUT /api/v1/events/{eventId}/reviews/me`
Sửa đánh giá của mình. Tối đa **3 lần trong 7 ngày** → quá hạn mức **409 `REVIEW_EDIT_LIMIT`**.
`version` = `updatedAt` dạng epoch millis; lệch → 409.

```json
{ "rating": 4, "text": "Nội dung đã cập nhật.", "version": 1762000000000 }
```

Nội dung sửa cũng bị lọc lại; vi phạm → quay về `PENDING`.

#### `GET /api/v1/me/reviews?page&size`
Mọi trạng thái (`PENDING`, `VISIBLE`, `HIDDEN`, `REMOVED`) kèm `editCount`,
`canEdit`, `editDaysLeft`.

#### `POST /api/v1/reviews/{reviewId}/report`
```json
{ "reasonCode": "SPAM", "note": "Nội dung quảng cáo" }
```
`reasonCode`: `SPAM`, `OFFENSIVE`, `PERSONAL_DATA`, `FAKE_REVIEW`, `OFF_TOPIC`, `OTHER`.

- 1 báo cáo / user / đánh giá → lần 2 **409**
- Tối đa 10 báo cáo / ngày / user → **429**
- Không báo cáo đánh giá của chính mình → **403**
- Organizer chỉ báo cáo được đánh giá thuộc sự kiện của mình → **403**
- **≥ 3 người báo cáo khác nhau**, hoặc **1 báo cáo từ organizer chủ sự kiện**
  → đánh giá tự chuyển `PENDING` để admin xem.

---

### 3.7 Kiểm duyệt (FR-A14 / A07) — `ADMIN`

Tất cả dưới `/api/v1/admin`, yêu cầu `ROLE_ADMIN` (403 nếu không).

#### `GET /admin/reviews?status&eventId&rating&page&size`
Hàng chờ đánh giá. `status`: `PENDING` \| `VISIBLE` \| `HIDDEN` \| `REMOVED`.

```json
{ "id": 4, "eventId": 27, "eventTitle": "...", "authorName": "Lê Anh",
  "authorEmail": "user3@eticket.local", "rating": 5, "text": "...",
  "status": "PENDING", "moderationReason": "PHONE_NUMBER",
  "flaggedCodes": ["HELD"], "reportCount": 0,
  "createdAt": "...", "updatedAt": "..." }
```

#### `POST /admin/reviews/{reviewId}/approve` · `/hide` · `/restore`
Body: `{ "reasonCode": "SPAM", "note": "..." }` — `reasonCode` **bắt buộc** (400 nếu thiếu).

| Hành động | Chuyển trạng thái |
|---|---|
| `approve` | `PENDING`/`HIDDEN` → `VISIBLE` |
| `hide` | → `HIDDEN` (ẩn khỏi công khai, vẫn còn dữ liệu) |
| `restore` | `HIDDEN` → `VISIBLE` |

Mọi hành động ghi `audit_logs` với `actorUserId`, lý do, giá trị cũ/mới.

#### `GET /admin/moderation/flags?source=ALL|HELD|REPORTED`
Cờ được **suy ra** từ dữ liệu thật (schema không có bảng `moderation_flags`):

- `HELD` ← đánh giá `PENDING` do bộ lọc nội dung giữ lại
- `REPORTED` ← báo cáo trong `audit_logs`

```json
{ "id": "review:3", "targetType": "REVIEW", "targetId": "3", "eventId": 27,
  "eventTitle": "...", "reasonCode": "PHONE_NUMBER", "reasonLabel": "Số điện thoại",
  "evidence": "090***12", "severity": "HIGH", "status": "OPEN",
  "createdAt": "..." }
```

`severity`: `LOW` | `MEDIUM` | `HIGH`.

#### `POST /admin/moderation/flags/{flagId}/confirm` · `/dismiss`
`flagId` dạng `review:{reviewId}`. `confirm` = ẩn nội dung, `dismiss` = cho hiển thị lại.

#### `POST /admin/moderation/rescan`
```json
{ "targetType": "REVIEW", "sinceDays": 30 }
```
Quét lại nội dung theo bộ lọc hiện tại (từ khoá cấm đọc từ `platform_settings`
key `content.blocked_keywords`, cache 60 giây).

```json
{ "success": true, "data": { "scanned": 3, "flagged": 1, "flagIds": ["review:3"] } }
```

---

### 3.8 Gợi ý AI (FR-U07)

#### `GET /api/v1/recommendations?limit=10&refresh=false`
- `limit` 1…20 (mặc định 10)
- Kết quả cache **15 phút**; `refresh=true` bỏ cache nhưng **tối đa 1 lần/phút**
  → vượt **429 `RATE_LIMITED`**

```json
{ "success": true, "data": {
  "items": [ { "...EventCardDto": "...", "reason": "Phù hợp với sở thích Nhạc sống của bạn",
               "reasonSource": "AI" } ],
  "meta": { "title": "Gợi ý cho bạn", "generatedAt": "2026-11-01T02:00:00Z",
            "isFallback": false, "personalized": true,
            "engineVersion": "rule-1.0.0", "modelCode": "mock-rules-v1",
            "aiRequestId": 1, "correlationId": "..." }
}}
```

- `reasonSource`: `AI` (AI viết lại lý do) hoặc `RULE` (lý do do rule sinh).
- `isFallback: true` khi AI bị tắt/lỗi — **danh sách vẫn trả về**, chỉ lý do là rule.
- Lý do do AI sinh bị hậu kiểm: chứa số (giá/ngày bịa) → bị thay bằng lý do rule.

#### `POST /api/v1/recommendations/preview`
Công khai, dùng sở thích tạm thời, **không** lưu lịch sử, không gọi AI.

```json
{ "categoryIds": [9], "city": "Hà Nội", "priceMode": "PAID_ONLY",
  "maxBudgetVnd": 500000, "eventFormat": "ONSITE",
  "preferredDays": ["WEEKEND"], "limit": 5 }
```

#### `POST /api/v1/recommendations/{eventId}/dismiss`
"Không quan tâm" — loại sự kiện khỏi gợi ý trong **30 ngày**.
`{ "eventId": 16, "dismissed": true }`

---

### 3.9 AI cho Organizer (FR-O06 / O27)

Yêu cầu organizer **đã duyệt** (`APPROVED`); organizer `PENDING`/`SUSPENDED`
hoặc user không có profile → **403**.

#### `POST /api/v1/organizer/ai/event-description`
Sinh bản nháp mô tả, **không tự lưu** vào sự kiện.

```json
{
  "title": "Đêm nhạc Acoustic mùa đông",
  "categoryId": 9,
  "format": "OFFLINE",
  "startTime": "20:00 20/12/2026",
  "location": "Nhà hát Thành phố",
  "onlineInfo": null,
  "audience": "Sinh viên và người yêu nhạc",
  "keyPoints": ["Dàn nhạc giao hưởng", "Khách mời quốc tế", "Không gian ngoài trời"],
  "tone": "FRIENDLY",
  "length": "MEDIUM",
  "variants": 2,
  "eventId": null
}
```

| Trường | Ràng buộc |
|---|---|
| `title` | 5…120 ký tự |
| `categoryId` | bắt buộc, phải `ACTIVE` (404 `CATEGORY_NOT_FOUND` / 409 `CATEGORY_INACTIVE`) |
| `format` | `OFFLINE` \| `ONLINE` \| `HYBRID` |
| `audience` | ≤ 200 |
| `keyPoints` | **3…10** ý, mỗi ý 3…200 ký tự |
| `tone` | `PROFESSIONAL` \| `FRIENDLY` \| `FORMAL` |
| `length` | `SHORT` \| `MEDIUM` \| `LONG` |
| `variants` | 1…3 |

```json
{ "success": true, "data": {
  "descriptions": [ { "text": "...", "shortSummary": "..." } ],
  "missingInfo": ["Chưa có thông tin giá vé"],
  "warnings": [],
  "source": "AI",
  "aiRequestId": 4,
  "correlationId": "..."
}}
```

- `source`: `AI` hoặc `RULE_UNAVAILABLE` (AI lỗi/tắt → `descriptions` rỗng,
  client hiển thị form thủ công).
- `warnings` là kết quả **hậu kiểm**: câu chứa số tiền / ngày / SĐT / email / URL
  không có trong dữ liệu nhập sẽ bị cảnh báo.
- Quota **10 lượt/giờ** → **429** với `extra.retryAfterSeconds`.

#### `POST /api/v1/organizer/events/{eventId}/ai-analysis`
Chỉ sự kiện của chính organizer (403 nếu không).

```json
{ "success": true, "data": {
  "eventId": 14,
  "metrics": { "registrations": 0, "capacity": 500, "fillRate": 0.0,
               "revenue": 300000.0, "checkedIn": 0, "checkinRate": 0.0,
               "avgRating": 3.0, "reviewCount": 1,
               "daysToEvent": 6, "daysSinceSaleOpened": 20,
               "insufficientData": false },
  "summary": ["...", "...", "..."],
  "insights": [ { "title": "...", "detail": "...", "confidence": "MEDIUM" } ],
  "disclaimer": "Phân tích mang tính tham khảo...",
  "dataLimitations": ["Số đánh giá còn ít nên điểm trung bình chưa ổn định."],
  "insufficientData": false,
  "source": "AI",
  "generatedAt": "2026-11-01T02:00:00Z"
}}
```

- `insufficientData: true` khi mở bán < 7 ngày **và** < 20 đăng ký → AI trả phân tích
  dè dặt, kèm cảnh báo.
- Quota **5 lượt/ngày**; hết quota → trả phân tích rule-based kèm ghi chú
  (không lỗi 429, vì dashboard cần số liệu).
- `confidence`: `LOW` | `MEDIUM` | `HIGH`.

---

### 3.10 Phân loại & nhãn (FR-O09)

#### `GET /api/v1/organizer/events/{eventId}/classification`
```json
{ "eventId": 14,
  "primaryCategory": { "id": 15, "code": "LIVE_MUSIC", "name": "Nhạc sống" },
  "secondaryCategories": [ { "id": 16, "code": "TECH", "name": "Công nghệ" } ],
  "tags": [ { "id": 7, "name": "Acoustic", "usageCount": 3 } ],
  "requiresReReview": false,
  "updatedAt": "..." }
```

#### `PUT /api/v1/organizer/events/{eventId}/classification`
```json
{ "primaryCategoryId": 15, "secondaryCategoryIds": [16], "tags": ["Acoustic", "live music"] }
```

| Ràng buộc | Lỗi |
|---|---|
| Chỉ chủ sự kiện | 403 |
| `primaryCategoryId` bắt buộc, phải `ACTIVE` | 400 / 404 / 409 |
| `secondaryCategoryIds` ≤ 2, không trùng primary | 400 |
| `tags` ≤ 5, mỗi nhãn 2…30 ký tự | 400 |
| Nhãn chứa từ khoá cấm | 400 |
| Sự kiện `CANCELLED` / `COMPLETED` bị khoá | 409 |

Nhãn được **chuẩn hoá**: chữ thường, gộp khoảng trắng, khử trùng lặp; nhãn mới
được tạo tự động. Đổi **danh mục chính** của sự kiện đã `APPROVED` →
`requiresReReview: true` (sự kiện phải duyệt lại).

#### `GET /api/v1/tags/suggest?q=ac&limit=10`
Gợi ý nhãn theo mức độ dùng: `[{ "id": 7, "name": "Acoustic", "usageCount": 3 }]`.

---

### 3.11 Dashboard Organizer (FR-O03)

#### `GET /api/v1/organizer/dashboard?range=7d|30d|custom&from=&to=`
- `range=7d` \| `30d` (mặc định) \| `custom` (kèm `from`, `to` dạng `yyyy-MM-dd`)
- Khoảng tối đa **366 ngày**, `from > to` → 400
- Organizer `PENDING` → 403

```json
{ "success": true, "data": {
  "range": { "from": "2026-10-02", "to": "2026-11-01", "days": 30 },
  "asOf": "2026-11-01T02:00:00Z",
  "events": { "status": "OK", "asOf": "...", "note": null,
              "total": 10,
              "byStatus": { "DRAFT": 1, "PENDING_REVIEW": 1, "REJECTED": 1,
                            "UPCOMING": 4, "COMPLETED": 1, "SUSPENDED": 1, "CANCELLED": 1 } },
  "registrations": { "status": "OK", "total": 3, "confirmed": 3, "cancelled": 0, "pending": 0 },
  "finance": { "status": "OK", "verifiedAmount": 600000, "pendingAmount": 300000,
               "refundedAmount": 0, "feeAmount": 0, "netAmount": 600000, "currency": "VND" },
  "checkin": { "status": "OK", "checkedIn": 3, "totalTickets": 3, "rate": 1.0 },
  "reviews": { "status": "OK", "averageRating": 4.25, "visible": 4, "newInRange": 4 },
  "tasks": [ { "type": "EVENT_REJECTED", "eventId": 22, "title": "...",
               "detail": "...", "dueAt": null, "priority": 3 } ],
  "topEvents": [ { "eventId": 14, "title": "...", "verifiedAmount": 300000,
                   "registrations": 0, "fillRate": 0.0 } ]
}}
```

Mỗi khối có `status`: `OK` | `ERROR`. Một khối lỗi **không** làm hỏng cả dashboard
(`note` ghi lý do).

`tasks[].priority`: `1` = sự kiện diễn ra trong 48h, `2` = cần sửa hồ sơ,
`3` = bị từ chối, `4` = loại vé sắp đóng bán.

---

### 3.12 Giám sát tích hợp (FR-A20) — `ADMIN`

#### `GET /admin/integrations`
Trạng thái 3 dịch vụ ngoài: `AI`, `PAYMENT`, `FCM`.

```json
[ { "name": "AI", "status": "HEALTHY", "errorRate": 0.0, "requestCount": 20,
    "lastSuccessAt": "2026-11-01T02:00:00Z", "lastErrorAt": null,
    "lastErrorCode": null, "p95LatencyMs": 14,
    "notes": "provider=mock, apiKeyConfigured=không, circuit=CLOSED" },
  { "name": "PAYMENT", "status": "UNKNOWN", "...": "..." } ]
```

`status`: `HEALTHY` | `DEGRADED` (lỗi ≥ 20% hoặc p95 > 10s) | `DOWN` (lỗi ≥ 50%) | `UNKNOWN` (0 request).

#### `GET /admin/integrations/ai/requests?range=1h|24h|7d`
```json
{ "range": "24h", "total": 20, "success": 20, "errors": 0, "successRate": 1.0,
  "byTaskType": { "RECOMMEND": 8, "DESCRIPTION": 10, "ANALYTICS": 2 },
  "errorBreakdown": {},
  "inputTokens": 1200, "outputTokens": 2400, "estimatedCostUsd": 0.0011,
  "dailyBudgetUsd": 2.0, "budgetUsedRatio": 0.0005,
  "p95LatencyMs": 14, "circuitState": "CLOSED",
  "features": { "RECOMMEND": true, "CHAT": true, "DESCRIPTION": true,
                "ANALYTICS": true, "MODERATION": true } }
```

#### `GET /admin/integrations/ai/requests/errors?limit=20`
Danh sách lỗi gần đây (mã lỗi, task, thời điểm, correlationId). User được **che**
dạng `1*0`.

#### `GET /admin/integrations/ai/requests/{correlationId}`
Tra toàn bộ lần gọi AI theo `correlationId`; không có → 404.

#### `PUT /admin/integrations/ai/features/{taskType}`
`taskType`: `RECOMMEND` \| `CHAT` \| `DESCRIPTION` \| `ANALYTICS` \| `MODERATION`.

```json
{ "enabled": false, "reason": "Provider lỗi liên tục, tắt tạm" }
```

- `reason` **bắt buộc** (A20-R04) → thiếu thì 400.
- Hiệu lực **ngay lập tức** (cache 5 giây), không cần restart.
- Tắt AI **không** làm sập tính năng: gợi ý chuyển sang lý do rule
  (`isFallback: true`), mô tả trả `source: RULE_UNAVAILABLE`.

```json
{ "taskType": "RECOMMEND", "enabled": false,
  "features": { "RECOMMEND": false, "...": "..." }, "effectiveImmediately": true }
```

---

### 3.13 Quản lý danh mục (FR-A09) — `ADMIN`

#### `GET /admin/categories?includeInactive=true`
Cây đầy đủ (kể cả danh mục đã ẩn) kèm `eventCount`.

#### `POST /admin/categories`
```json
{ "categoryCode": "FOOD", "categoryName": "Ẩm thực & Đồ uống",
  "slug": null, "description": "...", "parentCategoryId": null, "sortOrder": 7 }
```

| Ràng buộc | Lỗi |
|---|---|
| `categoryCode` bắt buộc, ≤ 60, duy nhất | 400 / 409 |
| `categoryName` bắt buộc, ≤ 120 | 400 |
| `slug` sinh tự động nếu bỏ trống, phải duy nhất | 409 |
| `parentCategoryId` trỏ danh mục cấp 1 | 400 `CATEGORY_DEPTH_EXCEEDED` |
| Tổng danh mục `ACTIVE` ≤ 50 | 409 |

#### `PUT /admin/categories/{categoryId}`
```json
{ "categoryName": "Ẩm thực", "slug": "am-thuc", "description": "...",
  "sortOrder": 7, "version": 1762000000000 }
```
`version` = `updatedAt` epoch millis; lệch → **409** (khoá lạc quan).

#### `POST /admin/categories/{categoryId}/hide`
```json
{ "reason": "Tạm ẩn để sáp nhập danh mục" }
```
`reason` bắt buộc. **Không có API xoá cứng** — luôn ẩn để giữ toàn vẹn dữ liệu lịch sử.

#### `POST /admin/categories/{categoryId}/unhide`
Bỏ ẩn; vẫn phải tôn trọng giới hạn 50 danh mục `ACTIVE`.

#### `PUT /admin/categories/order`
```json
{ "items": [ { "categoryId": 9, "sortOrder": 1 }, { "categoryId": 10, "sortOrder": 2 } ] }
```
1…200 mục.

---

## 4. Ghi chú: khác biệt giữa SRS và schema thật

Backend chạy trên **schema Aiven 64 bảng có sẵn**, không sửa DDL
(`ddl-auto: validate`). Vì vậy một số khái niệm trong SRS được **ánh xạ** lại:

| SRS | Thực tế triển khai |
|---|---|
| `moderation_flags` | Suy ra từ `reviews.status = PENDING` + `audit_logs` |
| `review_reports` | Ghi vào `audit_logs` (`action_code = REVIEW_REPORTED`) |
| `content_rules` | `platform_settings` key `content.blocked_keywords` |
| `ai_feature_flags` | `platform_settings` key `ai.feature.flags` |
| `faq_entries` | Chưa triển khai (không có bảng tương ứng) |
| `idempotency_keys` | Bộ nhớ trong, TTL 24 giờ |
| `search_text` (tìm không dấu) | Sinh regex từ truy vấn + `REGEXP_LIKE(col, pattern, 'i')` |
| `reviews.edit_count` / `edited_at` | Suy ra từ `audit_logs` |
| `user_preference_categories` / `_cities` | Cột JSON trong `user_preferences`; **chỉ lưu 1 thành phố** |
| `event_category_map.is_primary` | Không có cột → danh mục đầu tiên coi là chính |
| `reviews.status = 'HELD'` | Enum thật là `PENDING` |
| `event_format = 'ONSITE'` | Enum thật là `OFFLINE` (map ở tầng DTO) |

---

## 5. Kiểm thử

```bash
# 1. Chạy backend (profile dev, có seed)
cd backend && mvn spring-boot:run

# 2. Chạy smoke test toàn bộ 70 ca
./scripts/api-smoke.sh

# 3. Chạy với host khác
BASE=http://192.168.1.10:8080 ./scripts/api-smoke.sh
```

Smoke test tự dò `userId` từ `/api/v1/dev/accounts`, dùng `X-User-Id`, và
**chạy lại được nhiều lần** mà không cần reset DB.
