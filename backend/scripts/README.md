# Scripts

## `api-smoke.sh`

Smoke test toàn bộ API của module Hữu Cảnh bằng `curl` + `jq` (không cần Postman,
không cần newman). 70 ca, 12 nhóm.

### Chuẩn bị

```bash
# 1. Backend chạy profile dev (seed + cờ X-User-Id)
cd backend && mvn spring-boot:run

# 2. Cần curl và jq
which curl jq
```

### Chạy

```bash
./scripts/api-smoke.sh                              # mặc định http://localhost:8080
BASE=http://192.168.1.10:8080 ./scripts/api-smoke.sh
```

Mã thoát: `0` = tất cả đạt, `1` = có ca lỗi (danh sách in ở cuối).

### Nguyên tắc

- Script **tự dò** `userId` từ `GET /api/v1/dev/accounts` — không hard-code id.
- Xác thực bằng header `X-User-Id` (đường dev), không cần lấy JWT.
- Chỉ đọc và tạo dữ liệu nháp; **không xoá** dữ liệu sẵn có.
- **Chạy lại được nhiều lần**: các ca có thể vướng giới hạn nghiệp vụ khi chạy lần
  hai (hết lượt sửa đánh giá, hết quota AI theo giờ, danh mục đã tồn tại) đều
  chấp nhận mã `409`/`429` — đó là hành vi đúng, không phải lỗi.

### Các nhóm kiểm thử

| # | Nhóm | FR |
|---|---|---|
| 0 | Kết nối, dữ liệu seed, tài khoản dev | — |
| 1 | Danh sách, tìm không dấu, lọc, chi tiết | U04, U05 |
| 2 | Sở thích (version, giới hạn 10 danh mục) | U03 |
| 3 | Yêu thích (idempotent, filter) | U06 |
| 4 | Đánh giá (điều kiện, idempotency, sửa, báo cáo) | U20 |
| 5 | Kiểm duyệt (duyệt/ẩn/khôi phục, cờ, rescan) | A14, A07 |
| 6 | Gợi ý (cá nhân hoá, preview khách, dismiss) | U07 |
| 7 | AI cho organizer (mô tả, phân tích, quota) | O06, O27 |
| 8 | Phân loại & nhãn | O09 |
| 9 | Dashboard organizer | O03 |
| 10 | Giám sát tích hợp, bật/tắt AI khẩn cấp | A20 |
| 11 | Quản lý danh mục | A09 |
| 12 | Hạ tầng chung (correlationId, 401/404) | — |

### Thêm ca kiểm thử

Dùng các helper có sẵn ở đầu file:

```bash
st=$(call GET "/events?size=3" "$U1")          # call METHOD PATH [USER] [BODY] [HEADER...]
expect "tên ca" 200 "$st"
expect_one_of "tên ca" "200 409" "$st"          # chấp nhận nhiều mã
skip "tên ca" "lý do bỏ qua"
jqv '.data.id'                                  # đọc body JSON của lần gọi gần nhất
```

`call` ghi body vào `/tmp/smoke_body.json` và in ra HTTP status.
