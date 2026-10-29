# 03 — Trạng thái hiển thị và xử lý lỗi

## 1. Bốn trạng thái chuẩn

Mọi màn hình có dữ liệu dùng enum `ui/common/Resource.java`:

```java
public enum Resource { LOADING, SUCCESS, EMPTY, ERROR }
```

| Trạng thái | Hiển thị | Thành phần |
|---|---|---|
| `LOADING` | Khung xương / progress | shimmer hoặc `CircularProgressIndicator` |
| `SUCCESS` | Danh sách hoặc nội dung | `RecyclerView` / nội dung chi tiết |
| `EMPTY` | Thông điệp + hành động gợi ý | `EmptyStateView` |
| `ERROR` | Thông điệp lỗi + nút thử lại | `ErrorStateView` |

Quy tắc:

- Mỗi màn hình **phải** xử lý đủ 4 trạng thái; không được để màn hình trắng.
- `LOADING` và `ERROR` phải hủy/khôi phục đúng sau khi ViewModel bị clear.
- Khi người dùng đã có dữ liệu cũ, lỗi làm mới **không** được xóa dữ liệu cũ (hiển thị kèm thông báo).

## 2. Ánh xạ lỗi

`core/utils/ErrorMapper` chuyển `ApiException` (từ `core/network`) thành thông báo tiếng Việt:

| Tình huống | Thông báo |
|---|---|
| Thiết bị mất mạng | "Không có kết nối mạng, vui lòng kiểm tra và thử lại" |
| Timeout | "Máy chủ phản hồi quá lâu, vui lòng thử lại" |
| 401 không làm mới được | "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại" |
| 403 | "Bạn không có quyền thực hiện thao tác này" |
| 404 | "Không tìm thấy dữ liệu" |
| 409 | Hiển thị `message` từ backend (ví dụ "Vé đã được check-in") |
| 422 | Hiển thị `message` từ backend |
| 5xx | "Hệ thống đang bận, vui lòng thử lại sau" |

## 3. Trạng thái đặc thù theo nghiệp vụ

### 3.1 Sự kiện (BR-003)

Phải hiển thị **cả hai** trạng thái độc lập:

| Trạng thái duyệt | Nhãn | Màu |
|---|---|---|
| PENDING | Chờ duyệt | `status_pending` |
| APPROVED | Đã duyệt | `status_approved` |
| REJECTED | Từ chối | `status_rejected` |

| Trạng thái vận hành | Nhãn | Màu |
|---|---|---|
| DRAFT | Bản nháp | `brand_text_secondary` |
| PUBLISHED | Đang bán | `status_approved` |
| SUSPENDED | Tạm ngưng | `status_suspended` |
| CANCELLED | Đã hủy | `status_cancelled` |
| COMPLETED | Đã kết thúc | `status_completed` |

### 3.2 Đơn hàng và thanh toán

- App **không** được tự chuyển đơn sang `PAID`. Chỉ trạng thái đọc từ backend mới quyết định.
- Màn hình QR thanh toán hiển thị `PaymentStatus.PENDING`; khi có kết quả, hỏi lại backend (A07).
- Hết thời gian thanh toán → `EXPIRED`, hiển thị lối tạo lại đơn.

### 3.3 Vé và check-in

- `CheckinStatus.DUPLICATE` → "Vé này đã được check-in lúc …" (kèm `checkinTime`).
- `CheckinStatus.INVALID` → "Vé không hợp lệ hoặc không thuộc sự kiện này".
- `CheckinStatus.NOT_FOUND` → "Không tìm thấy vé".
- Check-in offline (A15): lưu hàng đợi cục bộ, đồng bộ khi có mạng; trạng thái tạm hiển thị "Chờ đồng bộ".

## 4. Thông báo (A14)

FCM chỉ mang `type` + `id`. Khi người dùng bấm thông báo:

1. `NotificationClickHandler` đọc `type` và `id`.
2. Điều hướng tới màn hình tương ứng.
3. Màn hình **gọi API** lấy trạng thái mới nhất — không dùng nội dung notification làm nguồn sự thật.

## 5. Khóa UI theo vai trò (A17)

- `RoleStore` giữ vai trò hiện tại (`Role.USER/ORGANIZER/ADMIN`).
- `RoleGuardLayout` chỉ hiển thị con khi vai trò khớp.
- Ẩn/hiện UI chỉ để gọn gàng; **backend mới là nơi quyết định quyền**.

## 6. Kiểm tra nhất quán

`scripts/verify-skeleton.mjs` bảo đảm:

- Mỗi nav destination có class và layout tồn tại.
- 5 id bottom navigation khớp nav graph.
- Không trùng tên layout, class.
- Đủ 23 Api / 23 Repository / 95 DTO.
- Manifest khớp Application, 5 Activity, FCM service.
