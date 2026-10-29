# 05 — Danh mục kiểm tra (test checklist)

Dùng khi phát triển và khi review. Mục **[x]** là đã đạt ở giai đoạn khung.

## A. Toàn vẹn khung

- [x] 86 Fragment + 86 ViewModel tồn tại, mỗi màn hình đúng 1 cặp
- [x] 119 layout: 86 fragment + 22 item + 6 dialog + 5 activity
- [x] 4 nav graph (auth/user/organizer/admin), 86 nav id không trùng
- [x] 3 bottom menu, mỗi menu đúng 5 id khớp nav graph
- [x] 5 Activity, manifest khai báo đủ, `SplashActivity` là LAUNCHER
- [x] 23 Retrofit Api, 23 Repository, 95 DTO, 23 package DTO
- [x] 17 enum + `EventStatus`, 22 Adapter, 6 Dialog, 6 custom View, 5 base
- [x] Không có file Java trùng tên class trên toàn project
- [x] Mọi file text không có BOM; mọi class stub `public` và có `// TODO:`

Chạy: `node scripts/verify-skeleton.mjs` (thoát mã 0, in "KHUNG HỢP LỆ").

## B. Build và tĩnh

- [x] `gradlew assembleDebug` thành công, sinh `app-debug.apk`
- [x] `gradlew testDebugUnitTest` thành công
- [x] `gradlew lintDebug` thành công, không lỗi chặn build
- [ ] `gradlew assembleRelease` với R8 bật, proguard rule đầy đủ
- [ ] Không có cảnh báo deprecation mới khi nâng Gradle

## C. Bảo mật

- [x] `usesCleartextTraffic=false` ở manifest
- [x] Release chặn cleartext; debug chỉ cho `10.0.2.2`, `localhost`, `127.0.0.1`
- [x] `google-services.json` bị chặn trong `.gitignore`
- [x] `local.properties`, `*.jks`, `*.keystore` bị chặn
- [x] `backup_rules.xml` / `data_extraction_rules.xml` loại trừ sharedpref và database
- [ ] Token lưu qua `EncryptedSharedPreferences`, không xuất hiện trong log (A18)
- [ ] App không chứa khóa bí mật nào; mọi bí mật nằm ở backend

## D. Kiến trúc

- [x] Luồng `Fragment → ViewModel → Repository → (Api | Room)` được tôn trọng trong tên gói
- [ ] ViewModel không import `Context`
- [ ] UI không tự đọc cache để quyết định vé/thanh toán
- [ ] Repository là nơi duy nhất biết cả mạng và cache
- [ ] Cache có dấu thời gian, làm mới khi mở màn hình

## E. Nghiệp vụ bất biến

- [ ] Sự kiện hiển thị **cả hai** `reviewStatus` và `lifecycleStatus` (BR-003)
- [ ] App không tự xác nhận thanh toán; trạng thái đọc từ backend (A07)
- [ ] Tạo QR thanh toán không được coi là đã trả tiền
- [ ] FCM không phải nguồn sự thật; bấm thông báo gọi API lấy trạng thái mới (A14)
- [ ] AI không tự duyệt sự kiện, không phát hành vé, không check-in
- [ ] Không xóa cứng dữ liệu có giao dịch (dùng ẩn/soft delete)
- [ ] Phân quyền quyết định ở backend; app chỉ ẩn/hiện UI (A17)

## F. Trạng thái hiển thị

- [ ] Mỗi màn hình dữ liệu xử lý đủ `LOADING/SUCCESS/EMPTY/ERROR`
- [ ] `EMPTY` có hành động gợi ý qua `EmptyStateView`
- [ ] `ERROR` có nút thử lại qua `ErrorStateView`, không xóa dữ liệu cũ khi làm mới lỗi
- [ ] `CheckinStatus.DUPLICATE` hiển thị kèm thời gian check-in trước đó
- [ ] Check-in offline hiển thị "Chờ đồng bộ" và đồng bộ khi có mạng (A15)

## G. Hợp đồng API

- [ ] Tên DTO, tên trường khớp `02-api-contract.md` và backend
- [ ] Mọi thay đổi hợp đồng ghi vào Changelog **cả hai** file docs
- [ ] Header `Authorization` chỉ gắn cho request cần đăng nhập (A02)
- [ ] 401 làm mới token; thất bại → đăng xuất sạch (A03)
- [ ] `X-Request-Id` gửi kèm để truy vết

## H. Kiểm thử chức năng (khi hoàn thiện)

- [ ] Đăng ký → xác thực email → đăng nhập → vào đúng vùng theo vai trò (FR-U01, FR-U02, FR-U03)
- [ ] Lọc sự kiện theo danh mục/ngày/giá cho kết quả đúng (FR-U04)
- [ ] Chi tiết sự kiện hiển thị lịch trình và loại vé (FR-U05)
- [ ] Đăng ký miễn phí sinh vé đúng, không thanh toán (FR-U09)
- [ ] Mua vé → QR → chờ thanh toán → xác nhận từ backend → vé xuất hiện (FR-U10–FR-U12)
- [ ] Hủy vé hiển thị đúng chính sách hoàn tiền (FR-U15)
- [ ] Check-in QR: lần đầu SUCCESS, lần hai DUPLICATE (FR-O21)
- [ ] Nhà tổ chức tạo sự kiện → gửi duyệt → admin duyệt → sự kiện công khai (FR-O13, FR-A05, FR-A06)
- [ ] Nhà tổ chức hủy sự kiện → hoàn tiền theo chính sách (FR-O24, FR-U15)
- [ ] Admin khóa người dùng → phiên của người đó bị vô hiệu (FR-A02)
- [ ] Bấm thông báo FCM điều hướng đúng màn hình (A14)
- [ ] Xuất báo cáo tạo đúng tệp (FR-O28)

## I. Trước khi phát hành

- [ ] Đổi `BASE_URL` release sang URL thật
- [ ] Bật R8/minify, kiểm tra proguard rule giữ DTO và Fragment
- [ ] Kiểm thử trên minSdk 24 và targetSdk 37
- [ ] Kích thước chạm ≥ 48dp, có nội dung mô tả cho trợ năng
- [ ] Đã xác nhận mọi giả định A01–A20 với backend/PM
