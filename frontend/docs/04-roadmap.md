# 04 — Lộ trình triển khai

Giai đoạn hiện tại: **khung (skeleton) đã dựng và biên dịch thành công**.
Các giai đoạn sau bổ sung UI thật và logic nghiệp vụ theo từng mã FR.

## Giai đoạn 0 — Khung dự án ✅

- [x] Gradle wrapper, AGP 9.4.1, Gradle 9.6.0, JDK 17, `compileSdk/targetSdk 37`, `minSdk 24`
- [x] `AndroidManifest.xml`, theme, màu, chiều, chuỗi
- [x] Network security config (release chặn, debug cho phép `10.0.2.2`)
- [x] `assembleDebug`, `testDebugUnitTest`, `lintDebug` đều thành công

## Giai đoạn 1 — Khung UI ✅

- [x] 86 Fragment + 86 ViewModel + 119 layout placeholder
- [x] 4 nav graph + 3 bottom menu (id khớp nav graph)
- [x] 5 Activity (Splash, Auth, User/Organizer/Admin host)

## Giai đoạn 2 — Thành phần dùng chung ✅ (khung)

- [x] 5 lớp base
- [x] 22 Adapter (đã cài đặt 3 phương thức trừu tượng, thân rỗng)
- [x] 6 Dialog, 6 custom View, `Resource`

## Giai đoạn 3 — Tầng dữ liệu ✅ (khung)

- [x] 23 Retrofit Api + 95 DTO + 23 Repository
- [x] 9 file cache Room (chỉ cache)
- [x] 4 file session/token (EncryptedSharedPreferences)
- [x] 4 mapper
- [x] 8 lớp hạ tầng mạng

## Giai đoạn 4 — Nền tảng hệ thống ✅ (khung)

- [x] 5 lớp FCM
- [x] 17 enum + `EventStatus`
- [x] 6 tiện ích
- [x] `EventApp`

## Giai đoạn 5 — Tài liệu ✅

- [x] `00-overview`, `01-screen-map`, `02-api-contract`, `03-state-display`, `04-roadmap`, `05-test-checklist`
- [x] Script sinh khung + script kiểm tra

## Giai đoạn 6 — Hoàn thiện theo tính năng ⏳

Ưu tiên theo giá trị và mức phụ thuộc:

| Thứ tự | Hạng mục | FR | Ghi chú |
|---|---|---|---|
| 1 | Xác thực: đăng nhập/đăng ký/quên mật khẩu | FR-U01, FR-U02 | Mở đường cho mọi tính năng |
| 2 | Khám phá sự kiện: danh sách, lọc, chi tiết | FR-U04, FR-U05, FR-U07 | UI đầu tiên người dùng thấy |
| 3 | Đăng ký miễn phí + mua vé + thanh toán | FR-U09–FR-U12 | Cần hợp đồng payment chốt |
| 4 | Vé của tôi + QR + lịch | FR-U14, FR-U18 | Sau khi có vé |
| 5 | Nhà tổ chức: tạo sự kiện, loại vé, gửi duyệt | FR-O04–FR-O13 | Phụ thuộc duyệt admin |
| 6 | Admin: duyệt sự kiện, quản lý người dùng | FR-A02, FR-A05, FR-A06 | Đóng vòng đời sự kiện |
| 7 | Check-in QR + giám sát | FR-O19–FR-O22 | CameraX + ML Kit |
| 8 | Thông báo + FCM | FR-U19, FR-O23, FR-A15 | Sau cùng vì phụ thuộc nhiều bên |
| 9 | Đánh giá, hỗ trợ, báo cáo, ví, khuyến mãi | FR-U16–FR-U22, FR-O25–FR-O29, FR-A10–FR-A20 | Hoàn thiện hệ sinh thái |

## Giai đoạn 7 — Chất lượng ⏳

- [ ] Unit test cho `MoneyUtils`, `DateTimeUtils`, `Validators`, mapper
- [ ] Instrumentation test cho luồng xác thực và mua vé
- [ ] Bật R8/minify ở release và viết đủ proguard rule
- [ ] Accessibility: kích thước chạm tối thiểu 48dp, nội dung mô tả
- [ ] Đa ngôn ngữ (hiện chỉ tiếng Việt)

## Điều kiện chuyển giao

Một màn hình được coi là "xong" khi:

1. Xử lý đủ 4 trạng thái (`LOADING/SUCCESS/EMPTY/ERROR`).
2. Dữ liệu lấy từ Repository, không hardcode.
3. Có ít nhất 1 test cho logic quan trọng.
4. `gradlew assembleDebug testDebugUnitTest lintDebug` thành công.
5. Sự kiện hiển thị đúng cả hai trường trạng thái khi liên quan.
