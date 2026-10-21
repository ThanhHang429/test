# ETicket — Frontend (Project 2/2)

Ứng dụng Android Native (Java, MVVM) cho nền tảng ETicket.

| Thuộc tính | Giá trị |
|---|---|
| Application ID | `com.eventapp.app` |
| Package gốc | `com.eventapp.app` |
| minSdk / targetSdk / compileSdk | 24 / 36 / 36 |
| Java | 17 |
| AGP / Gradle | 9.4.1 / 9.6.0 (fallback 8.13.2 / 8.14) |

## Kiến trúc

```
Fragment/Activity  →  ViewModel  →  Repository  →  Retrofit Api (remote)
                                                ↘  Room Dao (local cache)
```

- `app/src/main/java/com/eventapp/app/ui/` — Fragment, ViewModel, Activity, Adapter, Dialog, View dùng chung
- `app/src/main/java/com/eventapp/app/data/remote/` — Api (Retrofit) + DTO
- `app/src/main/java/com/eventapp/app/data/repository/` — Repository
- `app/src/main/java/com/eventapp/app/data/local/` — cache Room
- `app/src/main/java/com/eventapp/app/data/session/` — phiên đăng nhập / token
- `app/src/main/java/com/eventapp/app/core/` — utils, network, enums
- `app/src/main/java/com/eventapp/app/fcm/` — Firebase Cloud Messaging
- `app/docs/` — tài liệu kiến trúc & hợp đồng API

## Cấu hình API

`BASE_URL` được khai báo trong `app/build.gradle` (mỗi build type một giá trị):

- debug: `http://10.0.2.2:8080/api/v1/` (emulator Android trỏ về máy host)
- release: `https://api.example.com/api/v1/` — **phải thay bằng URL thật trước khi phát hành**

Cleartext HTTP chỉ được phép ở build type `debug` (`app/src/debug/res/xml/network_security_config.xml`).

## Firebase

Đặt file `app/google-services.json` thật vào `app/` trước khi build. File này **không được commit**.
Plugin `com.google.gms.google-services` chỉ được apply khi file tồn tại (xem `app/build.gradle`).

## Lệnh thường dùng

```bash
cd frontend
gradlew.bat assembleDebug     # build debug
gradlew.bat test              # unit test
gradlew.bat lint              # lint
node scripts/scaffold.mjs     # sinh lại khung (idempotent)
node scripts/verify-skeleton.mjs   # kiểm tra tính nhất quán của khung
```

Tài liệu chi tiết: [`docs/00-overview.md`](docs/00-overview.md).
