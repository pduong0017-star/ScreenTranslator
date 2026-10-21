# Screen Translator (Android) - Dịch Màn Hình Truyện & Tin Nhắn

Ứng dụng Android Native (Kotlin + Jetpack Compose) hỗ trợ dịch trực tiếp trên màn hình:
- **Dịch truyện tranh (Manga / Manhwa / Manhua / Comic):** Nhận diện ô thoại, che chữ gốc và in chữ tiếng Việt đè lên đúng tọa độ.
- **Dịch tin nhắn hội thoại:** Sử dụng AI để dịch văn phong tự nhiên, bắt trend, hiểu tiếng lóng thay vì dịch thô từ vựng.

---

## 🛠 Cấu trúc dự án
- **`OcrManager.kt`**: Sử dụng Google ML Kit OCR (On-device) nhận diện chữ tiếng Anh/Latin, tiếng Trung, Nhật, Hàn và lấy tọa độ chính xác từng ô chữ (`BoundingBox`).
- **`SmartAiTranslator.kt`**: Dùng Gemini/LLM API với prompt tối ưu hóa cho truyện tranh và tin nhắn (tự động fallback sang Google Translate miễn phí nếu không điền API Key).
- **`ScreenTranslateService.kt`**: Foreground Service quản lý bong bóng nổi (`SYSTEM_ALERT_WINDOW`) và luồng chụp màn hình ngầm (`MediaProjection`).
- **`ComicOverlayView`**: Custom View vẽ đè bản dịch lên đúng vị trí bóng thoại trên màn hình.
- **`MainActivity.kt`**: Giao diện Jetpack Compose cấp quyền và cấu hình API Key.

---

## 🚀 Cách mở và Build dự án
1. Dự án nằm tại thư mục: `/storage/emulated/0/Documents/ScreenTranslator`
2. Bạn có thể mở trực tiếp thư mục này trong **Android Studio** trên máy tính hoặc build bằng các công cụ Gradle trên điện thoại.
