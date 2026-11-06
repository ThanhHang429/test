# Postman — ETicket Backend

Bộ request cho toàn bộ API backend module Hữu Cảnh
(FR-U03, U04, U05, U06, U07, U20, O03, O06, O09, O27, A07, A09, A14, A20).

| File | Dùng để |
|---|---|
| `ETicket-Backend.postman_collection.json` | 95 request / 13 folder, có test script cho từng request |
| `ETicket-Local.postman_environment.json` | Environment `ETicket Local` — chỉ chứa `baseUrl` |

## 1. Import

1. Backend chạy profile `dev`:

   ```bash
   cd backend && mvn spring-boot:run
   ```

   Kiểm tra: `curl -H 'X-User-Id: 10' http://localhost:8080/api/v1/me/preferences`

2. Postman → **Import** → kéo cả 2 file JSON ở trên.
3. Chọn environment **ETicket Local** ở góc trên phải.
4. Đổi cổng/host khác thì sửa `baseUrl` trong environment (collection cũng có
   biến `baseUrl` dự phòng, environment được ưu tiên).

## 2. Thứ tự chạy

**Luôn chạy folder `00 · Setup` trước.** Folder này nạp toàn bộ ID mà các folder
sau dùng:

| Request | Việc nó làm |
|---|---|
| 1. Nạp ID tài khoản | Đọc `/dev/accounts`, map **theo email** → `adminId`, `organizer1Id`, `organizer2Id`, `user1Id`, `user2Id`, `user3Id` |
| 2. Cấp JWT cho user1 | Lưu `accessToken` |
| 3. Cấp JWT cho admin | Lưu `adminToken` |
| 4. Cây danh mục công khai | Lưu `categoryId` = danh mục gốc đầu tiên |
| 5. Tìm sự kiện thuộc organizer1 | Dò `/organizer/events/{id}/classification` để lưu `organizerEventId` (của organizer1) và `otherOrganizerEventId` (của organizer khác) |

Sau đó chạy lần lượt **01 → 12**. Thứ tự này quan trọng vì các folder dùng lại biến
do folder trước lưu (`pastEventId`, `reviewId`, `flagId`, `createdCategoryId`…).

Chạy tất cả một lần: chuột phải collection → **Run collection** → **Run**.
Thời gian khoảng 60–90 giây (folder 04 và 00 có dò dữ liệu bằng `pm.sendRequest`).

## 3. Bản đồ folder

| Folder | FR | Nội dung |
|---|---|---|
| `00 · Setup` | — | Nạp biến, cấp JWT, dò sự kiện mẫu |
| `01 · Khám phá & tìm kiếm` | U04, U05 | Danh sách, tìm không dấu, lọc, sắp xếp, chi tiết |
| `02 · Sở thích` | U03 | Đọc/ghi sở thích, idempotent, xung đột version, reset |
| `03 · Yêu thích` | U06 | Thêm/bỏ, danh sách, lọc, quyền riêng tư |
| `04 · Đánh giá` | U20 | Danh sách, tổng hợp, tạo/sửa, các nhánh lỗi, báo cáo |
| `05 · Kiểm duyệt` | A14, A07 | Hàng chờ, duyệt/từ chối, quét nội dung, cờ vi phạm |
| `06 · Gợi ý AI` | U07 | Gợi ý, làm mới, phản hồi, không quan tâm |
| `07 · AI cho Organizer` | O06, O27 | Sinh mô tả, phân tích sự kiện |
| `08 · Phân loại & nhãn` | O09 | Gắn danh mục/nhãn, từ khoá cấm, gợi ý nhãn |
| `09 · Dashboard Organizer` | O03 | 7d/30d/custom, tham số sai, chặn quyền |
| `10 · Giám sát tích hợp` | A20 | Trạng thái, request AI, bật/tắt tính năng |
| `11 · Quản lý danh mục` | A09 | Tạo/sửa/ẩn/bỏ ẩn, cấp 3, sắp xếp, dọn dẹp |
| `12 · Hạ tầng chung` | — | Envelope, `X-Correlation-Id`, route không tồn tại |

## 4. Quy ước test

Mỗi request kiểm tra ít nhất:

- đúng HTTP status,
- envelope `{ success, data, meta?, correlationId }` (hoặc `{ success:false, error:{code,…} }`),
- `correlationId` là chuỗi,
- header `X-Correlation-Id` có trong response (script cấp collection).

Một số request **chấp nhận nhiều mã** vì đó là hành vi đúng khi chạy lại:

| Mã | Khi nào |
|---|---|
| `409` | Đã đánh giá rồi, danh mục đã ẩn, mã danh mục trùng, version sở thích lệch |
| `429` | Hết quota AI (mô tả 10 lượt/giờ) — xem `error.extra.retryAfterSeconds` |
| `403` | Không phải chủ sự kiện, organizer chưa duyệt, chưa tham dự, hết hạn sửa |

Collection **chạy lại được nhiều lần** mà không cần reset DB:

- mã danh mục sinh tự động theo timestamp (`newCategoryCode`, `newChildCode`, `newLevel3Code`),
- folder 11 tự ẩn 2 danh mục vừa tạo ở 2 request cuối (giữ số danh mục ACTIVE không tăng),
- `pastEventId` / `notAttendedEventId` / `organizerEventId` được dò lại mỗi lần chạy.

## 5. Xác thực

Mặc định dùng **đường dev**: header `X-User-Id: {{user1Id}}`. Backend kiểm tra user
có thật trong DB rồi gán quyền — không cần mật khẩu.

Muốn test **JWT thật**:

1. Chạy `2. Cấp JWT cho user1` để lấy `accessToken`.
2. Ở từng request: tab **Authorization** → Bearer Token → `{{accessToken}}`,
   và **tắt** header `X-User-Id`.

`/api/v1/dev/*` chỉ tồn tại ở profile `dev`.

## 6. Biến collection

| Biến | Nguồn |
|---|---|
| `baseUrl` | Environment / collection |
| `adminId`, `organizer1Id`, `organizer2Id`, `user1Id`, `user2Id`, `user3Id` | Folder 00 request 1 |
| `accessToken`, `adminToken` | Folder 00 request 2, 3 |
| `categoryId` | Folder 00 request 4 |
| `organizerEventId`, `otherOrganizerEventId` | Folder 00 request 5 |
| `eventId` | Folder 01 request 1 (sự kiện công khai đầu tiên) |
| `pastEventId`, `notAttendedEventId` | Folder 04 request 1 |
| `reviewId` | Folder 04 request 4 (khi tạo mới thành công) |
| `flagId` | Folder 05 |
| `recommendedEventId` | Folder 06 |
| `createdCategoryId`, `childCategoryId` | Folder 11 |
| `prefVersion`, `staleVersion` | Folder 02 |

Giá trị mặc định trong collection là dữ liệu của lần seed gần nhất — chỉ dùng khi
bạn chạy lẻ một request mà không qua folder 00.
