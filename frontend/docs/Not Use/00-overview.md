# 00 — Tổng quan kiến trúc Frontend (Project 2/2)

Ứng dụng Android Native (Java) cho nền tảng ETicket. Tài liệu này mô tả kiến trúc,
quy ước và **giả định** đang được dùng trong giai đoạn khung (skeleton).

## 1. Vai trò trong hệ thống

| | Project 1/2 | Project 2/2 (repo này) |
|---|---|---|
| Thành phần | REST API (Spring Boot) | Ứng dụng di động (Android) |
| Nguồn sự thật | **Có** — tài khoản, quyền, trạng thái, số vé, đơn hàng, thanh toán | Không |
| Lưu trữ | MySQL 8 (remote) | SQLite/Room **chỉ để cache** |

## 2. Luồng dữ liệu

```
Fragment / Activity
      │  (quan sát LiveData / gọi hàm)
      ▼
ViewModel ── giữ trạng thái màn hình, không giữ Context
      │
      ▼
Repository ── quyết định đọc từ mạng hay cache
      │
      ├──► Retrofit Api  ──► Backend REST (nguồn sự thật)
      └──► Room Dao      ──► SQLite cache (chỉ đọc khi offline)
```

Quy tắc:

- ViewModel **không** import `android.content.Context`.
- Repository là nơi duy nhất biết về cả mạng và cache.
- UI **không** tự đọc cache để quyết định số vé, thanh toán hay vé hợp lệ.

## 3. Cấu trúc package `com.eventapp.app`

```
com.eventapp.app
├── EventApp.java              Application: khởi tạo container phụ thuộc
├── ui/
│   ├── base/                  BaseActivity, BaseFragment, BaseViewModel, BaseAdapter, BaseDialogFragment
│   ├── common/
│   │   ├── adapter/           22 RecyclerView Adapter
│   │   ├── dialog/            6 DialogFragment
│   │   ├── view/              6 custom View
│   │   └── Resource.java      enum LOADING / SUCCESS / EMPTY / ERROR
│   ├── SplashActivity, AuthActivity, UserHostActivity, OrganizerHostActivity, AdminHostActivity
│   ├── auth/                  5 màn hình xác thực
│   ├── user/                  29 màn hình người dùng (home, ticket, order, checkin, calendar, profile, review, support, notification, favorite)
│   ├── organizer/             29 màn hình nhà tổ chức (onboarding, dashboard, event, ticket, attendee, checkin, notification, refund, report, promotion)
│   └── admin/                 23 màn hình quản trị (dashboard, user, organizer, event, category, finance, support, notification, report, system)
├── data/
│   ├── remote/api/            23 Retrofit interface
│   ├── remote/dto/<module>/   95 DTO (giữ nguyên tên §11.1)
│   ├── repository/            23 Repository
│   ├── local/                 9 file cache Room (entity, dao, database, cleaner)
│   ├── session/               4 file phiên đăng nhập và token
│   └── mapper/                4 mapper DTO → model hiển thị
├── core/
│   ├── enums/                 17 enum + EventStatus
│   ├── utils/                 6 tiện ích
│   ├── network/               8 lớp hạ tầng mạng
│   └── common/                AppLogger, CoreConstants
└── fcm/                       5 lớp Firebase Cloud Messaging
```

## 4. Quy ước đặt tên

| Đối tượng | Quy ước | Ví dụ |
|---|---|---|
| Fragment màn hình | `<Name>Fragment.java`, package `ui.<role>.<area>` | `ui/user/order/CheckoutFragment.java` |
| ViewModel màn hình | `<Name>ViewModel.java` | `CheckoutViewModel.java` |
| Layout màn hình | `fragment_<area>_<snake>.xml` | `fragment_order_checkout.xml` |
| Nav id | `snake(<Name>) + "Fragment"`, **trùng giữa 2 vai trò → thêm tiền tố role** | `checkoutFragment`, `user_order_detailFragment` |
| Item layout | `item_<snake>.xml` | `item_event.xml` |
| Dialog layout | `dialog_<snake>.xml` | `dialog_confirm.xml` |
| Activity layout | `activity_<snake>.xml` | `activity_user_host.xml` |

> **Quan trọng:** khi 2 màn hình khác vai trò trùng tên class (ví dụ `OrderDetail`),
> class phải được đổi tên để tránh trùng (`UserOrderDetail`, `OrganizerOrderDetail`,
> `AdminOrderDetail`) nhưng nav id vẫn giữ `snake(<Name>)Fragment`.

## 5. Trạng thái hiển thị

Mọi màn hình có dữ liệu phải xử lý 4 trạng thái qua `Resource`:

| Trạng thái | Hiển thị |
|---|---|
| `LOADING` | Progress + khung xương |
| `SUCCESS` | Nội dung |
| `EMPTY` | `EmptyStateView` + hành động gợi ý |
| `ERROR` | `ErrorStateView` + nút thử lại |

## 6. Sự kiện có 2 trường trạng thái (BR-003)

`EventStatus` gộp **hai** trường tách biệt:

- `reviewStatus` — `EventReviewStatus`: PENDING / APPROVED / REJECTED
- `lifecycleStatus` — `EventLifecycleStatus`: DRAFT / PUBLISHED / SUSPENDED / CANCELLED / COMPLETED

Không được dùng 1 trường `status` đơn cho sự kiện.

## 7. Cấu hình môi trường

| | Debug | Release |
|---|---|---|
| `BASE_URL` | `http://10.0.2.2:8080/api/v1/` | `https://api.example.com/api/v1/` (placeholder) |
| Cleartext HTTP | Cho phép `10.0.2.2`, `localhost`, `127.0.0.1` | Chặn hoàn toàn |
| Firebase | plugin chỉ apply khi có `google-services.json` | như debug |

## 8. Giả định (cần xác nhận với backend/PM)

| Mã | Giả định | Ảnh hưởng |
|---|---|---|
| A01 | Backend expose tất cả endpoint dưới tiền tố `/api/v1/` | `BASE_URL`, `ApiClient` |
| A02 | Header `Authorization: Bearer <access_token>` cho mọi request cần đăng nhập | `AuthInterceptor`, `TokenProvider` |
| A03 | Access token hết hạn → backend trả `401`; app dùng refresh token để lấy token mới | `SessionManager`, `TokenProvider` |
| A04 | Response lỗi có cấu trúc thống nhất (`code`, `message`, `fieldErrors`) | `ApiException`, `ErrorMapper` |
| A05 | Định danh tài khoản dùng `userId` dạng chuỗi UUID | DTO, session |
| A06 | QR vé là ảnh PNG do backend sinh, không phải app tự sinh | `QrImageView`, `TicketResponse` |
| A07 | Webhook thanh toán do backend xử lý; app **không** xác nhận thanh toán | `paymentApi`, `PaymentStatus` |
| A08 | `google-services.json` đặt tại `app/` và không commit | `app/build.gradle` |
| A09 | Phân số tiền dùng đơn vị nhỏ nhất (VND, số nguyên) | `MoneyUtils` |
| A10 | Múi giờ hiển thị theo thiết bị, dữ liệu API là UTC ISO-8601 | `DateTimeUtils` |
| A11 | Tên enum giữ đúng như §5 của đặc tả | `core/enums` |
| A12 | Danh mục sự kiện do admin cấu hình, app chỉ đọc | `categoryApi` |
| A13 | Cổng thanh toán: MoMo / VNPAY / ZaloPay / chuyển khoản / thẻ / tiền mặt | `PaymentMethod` |
| A14 | FCM chỉ mang `type` + `id`; mở thông báo phải gọi API lấy trạng thái mới | `NotificationClickHandler` |
| A15 | Check-in offline được phép nhưng phải đồng bộ khi có mạng | `ManualCheckin`, `CheckinMethod` |
| A16 | Hủy vé/hoàn tiền phụ thuộc chính sách do backend quyết định | `CancelTicket`, `refundApi` |
| A17 | Phân quyền quyết định ở backend; app chỉ ẩn/hiện UI tương ứng | `RoleStore`, `RoleGuardLayout` |
| A18 | SĐT và email là thông tin nhạy cảm, không đưa vào log | `HttpLogger` |
| A19 | `compileSdk`/`targetSdk` = 37; `minSdk` = 24 | `app/build.gradle` |
| A20 | `EventStatus` là class giữ 2 enum, không phải enum thứ 18 | `core/enums/EventStatus.java` |

## 9. Rủi ro đã nhận diện

| Rủi ro | Giảm thiểu |
|---|---|
| Sixteen KB page size bắt buộc khi targetSdk ≥ 37 | Đã cài platform `android-37.2`, giữ dependency ở bản hỗ trợ |
| Token bị lộ qua backup hoặc log | EncryptedSharedPreferences + chặn backup + không log token |
| App tự quyết định trạng thái thanh toán | UI chỉ hiển thị trạng thái đọc từ backend |
| Cache gây hiểu nhầm "còn vé" | Cache có nhãn thời điểm; luôn làm mới khi mở màn hình |
| Trùng tên class giữa 3 vai trò | `scripts/verify-skeleton.mjs` chặn trùng tên khi chạy |
| Hợp đồng API lệch giữa 2 project | Sửa cả `frontend/docs/02-api-contract.md` và `backend/docs/02-api-endpoints.md` cùng lúc |
