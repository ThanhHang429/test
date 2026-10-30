# ETicket — Frontend (Project 2/2)

Ứng dụng Android Native (Java, MVVM) cho nền tảng ETicket.

| Thuộc tính | Giá trị |
|---|---|
| Application ID | `com.eventapp.app` |
| Package gốc | `com.eventapp.app` |
| minSdk / targetSdk / compileSdk | 24 / 37 / 37 |
| Java | 17 |
| AGP / Gradle | 9.4.1 / 9.6.0 |
| Module | `:app` (duy nhất) |

---

## 1. Yêu cầu môi trường

| Thành phần | Phiên bản | Kiểm tra |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Android SDK Platform | `android-37` | có trong `<SDK>/platforms` |
| Android SDK Build-Tools | 37.x | tự tải qua Gradle nếu thiếu |
| `adb` + emulator (hoặc máy thật) | — | `adb devices` |
| Node.js | 20+ — **chỉ cần** khi chạy script trong `scripts/` | `node -v` |

`local.properties` phải trỏ tới SDK (file này **không commit**, mỗi máy tự tạo):

```properties
sdk.dir=/duong/dan/toi/Android/Sdk
```

Nếu dùng Android Studio thì mở thư mục `frontend/` bằng Studio, file này được tạo tự động.

---

## 2. Chạy nhanh (5 bước)

```bash
cd frontend

# 1. Tạo local.properties nếu chưa có (thay bằng đường dẫn SDK thật)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 2. Build bản debug
./gradlew :app:assembleDebug
#    Windows: gradlew.bat :app:assembleDebug

# 3. Bật emulator (hoặc cắm máy thật đã bật USB debugging)
#    Cách 1: Android Studio → Device Manager → chọn AVD → Run
#    Cách 2: dòng lệnh (<SDK> là đường dẫn trong local.properties)
<SDK>/emulator/emulator -avd Medium_Phone &

# 4. Kiểm tra thiết bị đã kết nối
adb devices          # phải thấy 1 dòng "device", không phải "offline"/"unauthorized"
#    Nếu adb không có trên PATH: <SDK>/platform-tools/adb devices

# 5. Cài và mở app
./gradlew :app:installDebug
adb shell am start -n com.eventapp.app/.ui.host.SplashActivity
```

APK sau khi build nằm ở `app/build/outputs/apk/debug/app-debug.apk`.

Chạy lại sau khi sửa code: chỉ cần `./gradlew :app:installDebug` (Gradle tự build phần thay đổi).

---

## 3. Trạng thái hiện tại — đọc trước khi chạy

Đây là **khung (skeleton)**, chưa phải app hoàn chỉnh, nhưng đã chạy được luồng chính:

- **Luồng khởi động:** `SplashActivity` (600 ms) → `UserHostActivity` → `HomeFragment`.
  Vào thẳng khu người dùng vì tầng `core/session` còn là stub, chưa có kiểm tra đăng nhập.
- `AuthActivity`, `OrganizerHostActivity`, `AdminHostActivity` đã nạp layout + nav graph tương ứng
  nhưng **chưa có đường vào** từ UI (chưa có màn đăng nhập và chưa có chuyển role).
- 4 nav graph (`nav_auth`, `nav_user`, `nav_organizer`, `nav_admin`) và 86 màn hình đã khai báo đủ,
  nhưng phần lớn Fragment/ViewModel còn là stub — bấm vào tab khác có thể ra màn trắng.
- Phần đã có giao diện + logic thật: `ui/user/home/*`, `ui/user/review/*`, `ui/user/favorite/*`,
  `ui/user/profile/*`, `ui/admin/event/*`, `ui/organizer/{event,dashboard,report}/*`, `ui/common/*`.
  Dữ liệu là dữ liệu mẫu trong `ui/fake/` — **chưa gọi API thật** (tầng `data/` còn rỗng).
- Màn đăng nhập (`fragment_auth_login.xml`) vẫn là placeholder rỗng.

Ghi chú kỹ thuật: `targetSdk 37` nên Android 15+ vẽ edge-to-edge, `statusBarColor` bị hệ thống bỏ qua.
Layout `activity_*` đã đặt `android:fitsSystemWindows="true"` và theme đặt `windowLightStatusBar`
để nội dung không bị status bar đè.

---

## 4. Cấu hình API / backend

`BASE_URL` khai báo trong `app/build.gradle`, mỗi build type một giá trị:

| Build type | BASE_URL |
|---|---|
| `debug` | `http://10.0.2.2:8080/api/v1/` |
| `release` | `https://api.example.com/api/v1/` — **placeholder, phải thay trước khi phát hành** |

`10.0.2.2` là alias của máy host nhìn từ Android Emulator. Vì vậy:

- **Emulator** + backend chạy trên máy host cổng 8080 → dùng nguyên giá trị mặc định.
- **Máy thật** → phải đổi `BASE_URL` thành IP LAN của máy tính (ví dụ `http://192.168.1.10:8080/api/v1/`)
  **và** thêm IP đó vào `app/src/debug/res/xml/network_security_config.xml`, nếu không sẽ bị chặn
  cleartext HTTP.

Cleartext HTTP chỉ được phép ở build type `debug`, và chỉ với `10.0.2.2`, `localhost`, `127.0.0.1`
(`app/src/debug/res/xml/network_security_config.xml`). Bản `release` chặn toàn bộ HTTP.

Backend là project `../backend/` (Spring Boot, REST API). App **không** kết nối MySQL trực tiếp.

---

## 5. Firebase (tuỳ chọn)

Đặt `app/google-services.json` thật vào thư mục `app/` nếu cần test FCM.
File này **không được commit** (đã nằm trong `.gitignore`).

Plugin `com.google.gms.google-services` chỉ được apply khi file tồn tại, nên **không có file vẫn build bình thường**.

---

## 6. Kiến trúc

```
Fragment/Activity  →  ViewModel  →  Repository  →  Retrofit Api (remote)
                                                ↘  Room Dao (local cache)
```

| Thư mục | Nội dung |
|---|---|
| `ui/<role>/<area>/` | Một feature = một package (`auth`, `user`, `organizer`, `admin`) |
| `ui/common/` | `adapter/`, `view/`, `nav/`, `notification/` — dùng chung, không phụ thuộc feature |
| `ui/model/` | `*UiModel` dùng chung giữa nhiều feature |
| `ui/fake/` | Dữ liệu mẫu cho màn hình chưa nối API |
| `ui/host/`, `ui/auth/` | Activity host của 3 vùng chức năng + luồng xác thực |
| `data/remote/` | Api (Retrofit) + DTO |
| `data/repository/` | Repository — nơi duy nhất biết mạng và cache |
| `data/local/` | Cache Room (`entity/`, `dao/`, `prefs/`) |
| `data/model/` | Model nghiệp vụ dùng chung |
| `core/` | `common`, `enums`, `network`, `session`, `platform`, `util` |
| `fcm/` | Entry point Firebase Cloud Messaging |

Quy ước phụ thuộc (được `verify-skeleton.mjs` kiểm tra tự động):

`ui → data → core` · `core` không import `data`/`ui` · `ui/common` và `ui/model` không import feature cụ thể.

Chi tiết đầy đủ: [`docs/00-overview.md`](docs/00-overview.md).

---

## 7. Lệnh thường dùng

```bash
cd frontend

# Build
./gradlew :app:assembleDebug          # APK debug
./gradlew :app:assembleRelease        # APK release (cần cấu hình signing)
./gradlew clean                       # xoá output build

# Cài lên thiết bị đang kết nối
./gradlew :app:installDebug

# Kiểm thử
./gradlew :app:testDebugUnitTest      # unit test
./gradlew :app:lintDebug              # Android Lint (báo cáo ở app/build/reports/lint-results-debug.html)

# Log runtime (lọc theo PID của app)
adb logcat --pid=$(adb shell pidof -s com.eventapp.app)

# Gỡ app
adb uninstall com.eventapp.app
```

Script tiện ích (cần Node.js):

```bash
node scripts/scaffold.mjs            # tạo các file khung còn thiếu — KHÔNG ghi đè file đã có
node scripts/scaffold.mjs --force    # ghi đè toàn bộ (xoá code thật, cẩn thận)
node scripts/scaffold.mjs --tree     # in cây thư mục
node scripts/verify-skeleton.mjs     # kiểm tra tính nhất quán khung + quy ước phụ thuộc tầng
```

---

## 8. Xử lý sự cố

| Lỗi | Nguyên nhân / cách sửa |
|---|---|
| `SDK location not found` | Thiếu `local.properties` hoặc sai `sdk.dir` |
| `./gradlew: Permission denied` | `chmod +x gradlew` |
| `Failed to find target with hash string 'android-37'` | Chưa cài SDK Platform 37 — cài qua SDK Manager hoặc `sdkmanager "platforms;android-37"` |
| `Unsupported class file major version` | JDK quá cũ — dùng JDK 17+ |
| `adb devices` rỗng | Chưa bật emulator, hoặc máy thật chưa bật USB debugging / chưa authorize |
| Build ra APK nhưng mở app chỉ thấy một màu xanh dương đặc | App đứng ở `SplashActivity`. Kiểm tra `adb shell dumpsys activity activities \| grep topResumedActivity` — nếu là `SplashActivity` thì handler chuyển màn chưa chạy (xem `SplashActivity.openUserArea`) |
| Status bar đè lên nội dung, icon status bar gần như vô hình | Thiếu `android:fitsSystemWindows="true"` trên root layout của Activity, hoặc thiếu `windowLightStatusBar` trong theme (Android 15+ bỏ qua `statusBarColor`) |
| Gọi API bị `CLEARTEXT communication not permitted` | Đang dùng IP không nằm trong danh sách cho phép của `network_security_config.xml` (debug) |
| `verify-skeleton.mjs` báo lỗi phụ thuộc tầng | `ui/common` hoặc `ui/model` đang import `ui/user|organizer|admin`, hoặc `data`/`core` import `ui` |

---

## 9. Tài liệu

| File | Nội dung |
|---|---|
| [`docs/00-overview.md`](docs/00-overview.md) | Kiến trúc, cây package, quy ước đặt tên, giả định A01–A20 |
| [`docs/01-screen-map.md`](docs/01-screen-map.md) | Bản đồ 86 màn hình ↔ package ↔ layout ↔ nav id |
| [`docs/02-api-contract.md`](docs/02-api-contract.md) | Hợp đồng REST phía app |
| [`docs/03-state-display.md`](docs/03-state-display.md) | Quy tắc 4 trạng thái LOADING/SUCCESS/EMPTY/ERROR |
| [`docs/04-ui-design-plan.md`](docs/04-ui-design-plan.md) | Kế hoạch UI, design token `ds_*`, các phase P1–P6 |
| [`docs/SRS-huucanh.pdf`](docs/SRS-huucanh.pdf) | SRS module khám phá sự kiện, AI & tích hợp nâng cao |
