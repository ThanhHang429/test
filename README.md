# ETicket — Nền tảng khám phá, quản lý sự kiện, đăng ký tham dự và bán vé

Monorepo chứa **2 project** của cùng một hệ thống:

| Thư mục | Project | Công nghệ | Trạng thái |
|---|---|---|---|
| [`frontend/`](frontend/) | **Project 2/2 — Ứng dụng di động** | Android Native Java, MVVM, Retrofit/OkHttp, Room (SQLite cache), Navigation Component, CameraX + ML Kit, Firebase Cloud Messaging | Khung (skeleton) — đang xây dựng |
| `backend/` | **Project 1/2 — REST API** | Spring Boot 3.x, Java 17, Maven, Spring Web/Security/Data JPA/Validation, JWT, springdoc-openapi, firebase-admin, MySQL 8 (remote) | Dựng ở phiên làm việc khác |

Hai project **không dùng chung code**, chỉ giao tiếp qua **REST API** theo hợp đồng:

- Hợp đồng phía app: [`frontend/docs/02-api-contract.md`](frontend/docs/02-api-contract.md)
- Hợp đồng phía server: `backend/docs/02-api-endpoints.md`

> Mọi thay đổi hợp đồng phải sửa **cả hai file cùng lúc** và ghi vào mục `Changelog` cuối mỗi file.

## Quy tắc bất di bất dịch của hệ thống

1. **Backend là nguồn dữ liệu chính thức** cho tài khoản, quyền, trạng thái duyệt, số lượng vé, đơn hàng, thanh toán, vé và check-in.
2. **SQLite (Room) trên app chỉ là cache** — không quyết định số vé, thanh toán, vé hợp lệ hay số dư.
3. **App không bao giờ kết nối MySQL** và không nhúng API key bí mật.
4. **Sự kiện có 2 trường trạng thái tách biệt**: `review_status` (duyệt) và `lifecycle_status` (vận hành) — không dùng 1 trường `status` duy nhất.
5. **Tạo QR thanh toán ≠ đã trả tiền.** Chỉ webhook đã xác thực chữ ký hoặc quy trình đối soát có quyền mới xác nhận tiền.
6. **Thông báo (FCM) không phải nguồn trạng thái chính thức** — mở thông báo phải gọi API lấy trạng thái mới.
7. **AI chỉ hỗ trợ** — không tự duyệt sự kiện, không tự xác nhận thanh toán, không phát hành vé, không check-in.
8. **Không xóa cứng dữ liệu có giao dịch** — dùng ẩn / ngừng hoạt động / soft delete.

## Cấu trúc repo

```
ETicket/
├── .gitignore                 # dùng chung cho cả frontend/ và backend/
├── README.md
├── frontend/                  # ← project Android (repo này đang xây dựng)
│   ├── app/                   # module app (Java, MVVM)
│   ├── docs/                  # tài liệu kiến trúc & hợp đồng API phía app
│   ├── scripts/               # script sinh & kiểm tra khung
│   └── gradle/ app/ settings.gradle build.gradle
└── backend/                   # ← project Spring Boot (dựng ở phiên khác)
```

## Nhánh git

- `Test` — nhánh chính hiện tại của repo.
- `feature/<module>-<fr>` — nhánh tính năng theo mã chức năng, ví dụ `feature/event-fr-o04`.
- Quy ước commit: `<type>(<scope>): <mô tả ngắn>` — `feat`, `fix`, `chore`, `docs`, `refactor`.

## Yêu cầu môi trường

| Thành phần | Phiên bản |
|---|---|
| JDK | 17 |
| Android Gradle Plugin | 9.4.1 (fallback 8.13.2) |
| Gradle | 9.6.0 (fallback 8.14) |
| Android SDK | compileSdk 36, build-tools 36.0.0, minSdk 24 |
| Node.js | 20+ (chỉ để chạy script trong `frontend/scripts/`) |
