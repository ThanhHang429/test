# Event Platform Backend (Kotlin + Spring Boot)

Backend REST API cho ETicket — **module Hữu Cảnh** (khám phá sự kiện, AI & tích hợp nâng cao).

- Kotlin 1.9 · Spring Boot 3.3 · Java 21 · Maven
- MySQL 8.4 (Aiven) · Spring Data JPA (`ddl-auto: validate`, **không** sửa schema)

## Chạy nhanh

```bash
cp .env.example .env      # rồi điền DB_PASSWORD, JWT_SECRET
mvn spring-boot:run       # profile dev: seed dữ liệu mẫu + bật X-User-Id
```

- API: <http://localhost:8080/api/v1>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

Profile `dev` tự tạo dữ liệu mẫu (mật khẩu `Password123!`). Xem tài khoản:
`GET /api/v1/dev/accounts`.

### Dữ liệu mẫu

Hai seeder, cả hai chỉ chạy ở profile `dev` và **không bao giờ xoá** dữ liệu:

| Seeder | Điều kiện | Nội dung |
|---|---|---|
| `DevDataSeeder` | `app.seed.enabled=true` + bảng `users` rỗng | Bộ gốc, id **cố định** (user 7–12, event 14–27) — smoke test dựa vào |
| `DemoDataSeeder` | `app.seed.demo-enabled=true` + chưa có `EVT-DEMO-%` | Bộ lớn, chỉ `INSERT` thêm |

Sau khi cả hai chạy: **34 user, 6 organizer, 59 sự kiện, 108 loại vé, 210 vé,
131 check-in, 98 đơn hàng, 95 đánh giá, 173 yêu thích**. Chi tiết từng nhóm và
cách tạo lại: [`docs/API.md` mục 6](docs/API.md#6-dữ-liệu-mẫu).

Tắt bộ demo: `app.seed.demo-enabled=false` trong `application-dev.yml`.

> `.env` được nạp qua `spring.config.import` — Spring Boot **không** đọc `.env`
> mặc định. File phải là UTF-8 **không BOM**, và dùng `spring.profiles.active=dev`
> (viết thường) chứ không phải `SPRING_PROFILES_ACTIVE`.

## Cấu trúc

```
src/main/kotlin/com/eventapp/
├── common/         response envelope, exception, security (JWT), audit, idempotency, seed
├── identity/       user, role, organizer profile
├── event/          event, media, ticket type
├── catalog/        category, tag, map bảng nối
├── discovery/      FR-U04/U05 — danh sách, tìm kiếm, chi tiết
├── preference/     FR-U03 — sở thích
├── favorite/       FR-U06 — yêu thích
├── review/         FR-U20 — đánh giá
├── moderation/     FR-A14/A07 — kiểm duyệt, bộ lọc nội dung
├── recommendation/ FR-U07 — gợi ý (rule + AI rerank)
├── organizer/      FR-O03/O06/O09/O27 — dashboard, AI, phân loại
├── admin/          FR-A09/A20 — danh mục, giám sát tích hợp
└── ai/             AI Gateway (provider, quota, circuit breaker, validator, logger)
```

## Tài liệu

| File | Nội dung |
|---|---|
| [`docs/API.md`](docs/API.md) | Đặc tả toàn bộ endpoint, mã lỗi, khác biệt schema vs SRS |
| [`postman/ETicket-Backend.postman_collection.json`](postman/ETicket-Backend.postman_collection.json) | 95 request, 13 folder — chạy trực tiếp trong Postman |
| [`postman/ETicket-Local.postman_environment.json`](postman/ETicket-Local.postman_environment.json) | Environment `ETicket Local` (chỉ chứa `baseUrl`) |
| [`postman/README.md`](postman/README.md) | Hướng dẫn import, thứ tự chạy, bản đồ folder, ý nghĩa biến |
| [`scripts/api-smoke.sh`](scripts/api-smoke.sh) | Smoke test 70 ca bằng curl + jq |
| `docs/*.pdf` | SRS và Project Final Business Analysis (nguồn yêu cầu) |

## Kiểm thử

```bash
./scripts/api-smoke.sh                    # cần backend đang chạy
BASE=http://10.0.0.5:8080 ./scripts/api-smoke.sh
```

Script chỉ đọc và tạo dữ liệu nháp, chạy lại nhiều lần không cần reset DB.

## Cấu hình AI

Mặc định dùng **provider giả lập** (`app.ai.provider=mock`) — không cần API key,
kết quả tất định, phù hợp demo và test. Chuyển sang mô hình bên thứ ba bằng cách
đặt `AI_PROVIDER` + `AI_API_KEY` trong `.env` (key chỉ nằm ở backend):

| `AI_PROVIDER` | Nhà cung cấp | `AI_BASE_URL` mặc định | `AI_MODEL` mặc định |
|---|---|---|---|
| `mock` | Giả lập cục bộ | — | `mock-rules-v1` |
| `gemini` | Google Gemini (native REST) | `https://generativelanguage.googleapis.com` | `gemini-2.0-flash` |
| `deepseek` | DeepSeek (chuẩn OpenAI) | `https://api.deepseek.com/v1` | `deepseek-chat` |
| `openrouter` | OpenRouter (chuẩn OpenAI) | `https://openrouter.ai/api/v1` | `deepseek/deepseek-chat` |
| `openai` | OpenAI (chuẩn OpenAI) | `https://api.openai.com/v1` | `gpt-4o-mini` |
| `openai-compatible` | Endpoint tự chọn | bắt buộc đặt `AI_BASE_URL` | bắt buộc đặt `AI_MODEL` |

Để trống `AI_BASE_URL`/`AI_MODEL` thì dùng giá trị mặc định ở bảng trên.
Provider thật yêu cầu `AI_API_KEY`; thiếu key thì ứng dụng báo lỗi ngay lúc khởi động
thay vì âm thầm rơi về mock.

Mọi lần gọi AI đều ghi `ai_requests` + `integration_health_logs`, có quota theo
tác vụ, circuit breaker và cờ bật/tắt khẩn cấp cho admin
(`PUT /api/v1/admin/integrations/ai/features/{taskType}`).

### Chức năng AI theo vai trò

| Vai trò | Chức năng | Mã | Endpoint / nơi dùng |
|---|---|---|---|
| User | Gợi ý sự kiện (rule chấm điểm + AI xếp hạng lại và viết lý do) | FR-U07 | `GET /api/v1/recommendations` |
| Organizer | AI viết mô tả sự kiện (bản nháp, hậu kiểm thông tin bịa) | FR-O06 | `POST /api/v1/organizer/ai/event-description` |
| Organizer | AI soạn thông báo gửi người tham dự (bản nháp, không tự gửi) | FR-O06 | `POST /api/v1/organizer/ai/announcement` |
| Organizer | AI phân tích và gợi ý vận hành | FR-O27 | `POST /api/v1/organizer/events/{eventId}/ai-analysis` |
| Admin | Giám sát tích hợp AI / thanh toán / FCM | FR-A20 | `GET /api/v1/admin/integrations` |

Nguyên tắc chung: AI chỉ tạo đề xuất, không quyết định trạng thái tài chính, duyệt
sự kiện, phát hành vé hay check-in (BR-017). Mọi con số hiển thị lấy từ Backend;
AI lỗi/timeout/bị tắt thì chức năng chính vẫn chạy bằng luật (NFR-03).
