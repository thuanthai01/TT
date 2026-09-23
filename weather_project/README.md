# HyperOS Weather Widget

Widget thời tiết Android/HyperOS giữ phong cách glassmorphism của bản gốc nhưng lấy dữ liệu thực tế theo vị trí điện thoại.

## Dữ liệu
- Vị trí: Android LocationManager (GPS/network), cần quyền vị trí.
- Thời tiết: Open-Meteo API, không cần API key.
- Dự báo: nhiệt độ cao/thấp và mô tả 3 ngày tiếp theo.
- Tên khu vực: Android Geocoder.
- Cập nhật widget: tối đa theo chu kỳ hệ thống 30 phút.

## Cài đặt
1. Cài APK.
2. Mở app một lần và cấp quyền vị trí.
3. Vào màn hình chính Xiaomi > Tiện ích/Widgets > HyperOS Weather.
4. Nếu Xiaomi trì hoãn cập nhật nền, cho phép ứng dụng hoạt động nền/tắt hạn chế pin cho app.

## Build
Yêu cầu Android Studio/Gradle 8.x và JDK 17.
- `gradle assembleDebug`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

Project cũng có GitHub Actions để build APK tự động.
