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

Profile `dev` tự tạo 6 tài khoản seed (mật khẩu `Password123!`), 14 sự kiện,
19 loại vé, đơn hàng, thanh toán, đánh giá và yêu thích. Xem danh sách:
`GET /api/v1/dev/accounts`.

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
| [`postman/ETicket-Backend.postman_collection.json`](postman/ETicket-Backend.postman_collection.json) | 85 request, 13 folder — chạy trực tiếp trong Postman |
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
kết quả tất định, phù hợp demo và test. Đổi sang provider thật bằng cách đặt
`AI_PROVIDER`, `AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL` trong `.env`.

Mọi lần gọi AI đều ghi `ai_requests` + `integration_health_logs`, có quota theo
tác vụ, circuit breaker và cờ bật/tắt khẩn cấp cho admin
(`PUT /api/v1/admin/integrations/ai/features/{taskType}`).
