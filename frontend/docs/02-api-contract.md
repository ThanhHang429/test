# 02 — Hợp đồng API phía ứng dụng Android

> File này **phải khớp** với `backend/docs/02-api-endpoints.md`.
> Mọi thay đổi phải sửa **cả hai file cùng lúc** và ghi vào [Changelog](#changelog).

## 1. Quy ước chung

| Hạng mục | Giá trị |
|---|---|
| Base URL (debug) | `http://10.0.2.2:8080/api/v1/` |
| Base URL (release) | `https://api.example.com/api/v1/` (placeholder — thay trước khi phát hành) |
| Định dạng | JSON, `Content-Type: application/json` |
| Xác thực | `Authorization: Bearer <access_token>` (A02) |
| Thời gian | ISO-8601 UTC, ví dụ `2026-10-21T09:30:00Z` (A10) |
| Tiền | Số nguyên, đơn vị nhỏ nhất của VND (A09) |
| Định danh | Chuỗi UUID (A05) |
| Phân trang | `page` (0-based), `size`, `sort`; response có `content`, `page`, `size`, `totalElements`, `totalPages` |
| Truy vết | Header `X-Request-Id` sinh bởi `RequestIdProvider` gửi kèm mỗi request |

### Khuôn dạng response thành công

```json
{ "data": { }, "meta": { } }
```

### Khuôn dạng response lỗi (A04)

```json
{
  "code": "EVENT_NOT_FOUND",
  "message": "Không tìm thấy sự kiện",
  "fieldErrors": [ { "field": "email", "message": "Email không hợp lệ" } ],
  "requestId": "3f750f03-..."
}
```

Ánh xạ mã HTTP trong `ErrorMapper`:

| HTTP | Ý nghĩa | Xử lý ở app |
|---|---|---|
| 400 | Dữ liệu sai | Hiển thị `fieldErrors` tại form |
| 401 | Access token hết hạn (A03) | Làm mới bằng refresh token; thất bại → đăng xuất |
| 403 | Không đủ quyền | Hiển thị thông báo, không cho thao tác (A17) |
| 404 | Không tìm thấy | Trạng thái `EMPTY` |
| 409 | Xung đột (hết vé, đã check-in) | Hiển thị lý do từ `message` |
| 422 | Vi phạm quy tắc nghiệp vụ | Hiển thị `message` |
| 429 | Quá nhiều request | Chờ và thử lại |
| 5xx | Lỗi hệ thống | Trạng thái `ERROR` + nút thử lại |

## 2. 23 module API

| # | Module | Api | Repository | Số DTO |
|---|---|---|---|---|
| 1 | `auth` | `AuthApi` | `AuthRepository` | 7 |
| 2 | `user` | `UserApi` | `UserRepository` | 7 |
| 3 | `organizer` | `OrganizerApi` | `OrganizerRepository` | 6 |
| 4 | `event` | `EventApi` | `EventRepository` | 11 |
| 5 | `category` | `CategoryApi` | `CategoryRepository` | 2 |
| 6 | `media` | `MediaApi` | `MediaRepository` | 2 |
| 7 | `tickettype` | `TicketTypeApi` | `TicketTypeRepository` | 4 |
| 8 | `registration` | `RegistrationApi` | `RegistrationRepository` | 3 |
| 9 | `order` | `OrderApi` | `OrderRepository` | 4 |
| 10 | `payment` | `PaymentApi` | `PaymentRepository` | 5 |
| 11 | `refund` | `RefundApi` | `RefundRepository` | 3 |
| 12 | `ticket` | `TicketApi` | `TicketRepository` | 2 |
| 13 | `checkin` | `CheckinApi` | `CheckinRepository` | 7 |
| 14 | `favorite` | `FavoriteApi` | `FavoriteRepository` | 1 |
| 15 | `review` | `ReviewApi` | `ReviewRepository` | 3 |
| 16 | `notification` | `NotificationApi` | `NotificationRepository` | 4 |
| 17 | `support` | `SupportApi` | `SupportRepository` | 4 |
| 18 | `report` | `ReportApi` | `ReportRepository` | 6 |
| 19 | `audit` | `AuditApi` | `AuditRepository` | 2 |
| 20 | `ai` | `AiApi` | `AiRepository` | 5 |
| 21 | `systemconfig` | `SystemConfigApi` | `SystemConfigRepository` | 3 |
| 22 | `wallet` | `WalletApi` | `WalletRepository` | 2 |
| 23 | `promotion` | `PromotionApi` | `PromotionRepository` | 2 |
| | **Tổng** | **23** | **23** | **95** |

## 3. Danh sách DTO theo module (giữ nguyên tên §11.1)

### auth (7)
`RegisterRequest`, `VerifyEmailRequest`, `LoginRequest`, `LoginResponse`, `RefreshTokenRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`

### user (7)
`UserProfileResponse`, `UpdateProfileRequest`, `UpdatePreferenceRequest`, `PrivacySettingDto`, `RegisterDeviceTokenRequest`, `LockUserRequest`, `AdminUserResponse`

### organizer (6)
`OrganizerApplicationRequest`, `OrganizerProfileResponse`, `UpdateOrganizerProfileRequest`, `OrganizerDashboardResponse`, `OrganizerDecisionRequest`, `SuspendOrganizerRequest`

### event (11)
`EventSummaryResponse`, `EventDetailResponse`, `EventScheduleResponse`, `EventSearchRequest`, `CreateEventRequest`, `UpdateEventRequest`, `UpdateScheduleRequest`, `UpdateCategoriesRequest`, `ReviewDecisionRequest`, `CancelEventRequest`, `EventVersionResponse`

> `EventDetailResponse` và `EventSummaryResponse` chứa **hai** trường trạng thái:
> `reviewStatus` (`EventReviewStatus`) và `lifecycleStatus` (`EventLifecycleStatus`) — BR-003.

### category (2)
`CategoryResponse`, `CategoryRequest`

### media (2)
`MediaResponse`, `ReorderMediaRequest`

### tickettype (4)
`TicketTypeRequest`, `TicketTypeResponse`, `UpdateQuantityRequest`, `SaleWindowRequest`

### registration (3)
`FreeRegisterRequest`, `RegistrationResponse`, `ManualAdjustRequest`

### order (4)
`CreateOrderRequest`, `OrderResponse`, `OrderItemResponse`, `OrderSummaryResponse`

### payment (5)
`PaymentQrResponse`, `PaymentStatusResponse`, `WebhookPayload`, `ReconcileRequest`, `AdminPaymentResponse`

> App **không** xác nhận thanh toán. `PaymentQrResponse` chỉ để hiển thị QR (A06, A07).

### refund (3)
`CreateRefundRequest`, `RefundResponse`, `RefundDecisionRequest`

### ticket (2)
`TicketResponse`, `TicketDetailResponse`

### checkin (7)
`ScanCheckinRequest`, `CheckinResponse`, `EventQrResponse`, `ManualCheckinRequest`, `CheckinStatsResponse`, `TicketLookupResponse`, `StaffRequest`

### favorite (1)
`FavoriteResponse`

### review (3)
`ReviewRequest`, `ReviewResponse`, `ModerationRequest`

### notification (4)
`NotificationResponse`, `SendNoticeRequest`, `BroadcastRequest`, `CalendarItemResponse`

### support (4)
`CreateSupportRequest`, `SupportTicketResponse`, `SupportMessageRequest`, `AssignSupportRequest`

### report (6)
`EventSummaryReport`, `SalesReport`, `AttendanceReport`, `PlatformOverviewReport`, `FinanceReport`, `ExportRequest`

### audit (2)
`AuditLogResponse`, `AuditSearchRequest`

### ai (5)
`RecommendationRequest`, `RecommendationResponse`, `DescribeEventRequest`, `DescribeEventResponse`, `InsightResponse`

> AI chỉ gợi ý; mọi thao tác duyệt, phát hành vé, check-in đều do người dùng xác nhận.

### systemconfig (3)
`ConfigResponse`, `UpdateConfigRequest`, `AssignRoleRequest`

### wallet (2)
`WalletResponse`, `TopupRequest`

### promotion (2)
`PromotionRequest`, `PromotionResponse`

## 4. Ghi chú trạng thái

Các enum dùng trong DTO và UI (chi tiết tại `core/enums`):

| Enum | Giá trị |
|---|---|
| `EventReviewStatus` | PENDING, APPROVED, REJECTED |
| `EventLifecycleStatus` | DRAFT, PUBLISHED, SUSPENDED, CANCELLED, COMPLETED |
| `OrderStatus` | PENDING, PAID, CANCELLED, REFUNDED, EXPIRED |
| `PaymentStatus` | PENDING, SUCCESS, FAILED, CANCELLED, EXPIRED, REFUNDED |
| `PaymentMethod` | MOMO, VNPAY, ZALOPAY, BANK_TRANSFER, CARD, CASH |
| `RefundStatus` | PENDING, APPROVED, REJECTED, PROCESSING, COMPLETED |
| `CheckinMethod` | QR_SCAN, MANUAL |
| `CheckinStatus` | SUCCESS, DUPLICATE, INVALID, NOT_FOUND |
| `Role` | USER, ORGANIZER, ADMIN |

## 5. Changelog

| Ngày | Thay đổi | Ảnh hưởng |
|---|---|---|
| 2026-10-21 | Khởi tạo hợp đồng từ §11 + §11.1; thêm `EventScheduleResponse` để tách lịch trình khỏi chi tiết sự kiện | Sinh 23 Api, 95 DTO, 23 Repository |
