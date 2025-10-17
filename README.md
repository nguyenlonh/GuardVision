# GuardVision

GuardVision là một ứng dụng Android hỗ trợ người khiếm thị, sử dụng các cử chỉ vuốt màn hình để tương tác với các tính năng khác nhau.

## Tính năng

### Điều hướng bằng cử chỉ
Ứng dụng sử dụng các cử chỉ vuốt đơn giản để truy cập các tính năng:

1. **Vuốt trái 1 lần** - Đọc thành phần (Ingredient Reader)
   - Sử dụng camera và ML Kit để nhận diện và đọc thành phần trên nhãn sản phẩm

2. **Vuốt trái 2 lần** - Nhận diện văn bản (Text Recognition)
   - Nhận diện và đọc văn bản từ tài liệu, biển báo, và các nguồn văn bản khác

3. **Vuốt phải** - Nhận diện vật thể (Object Detection)
   - Nhận diện và thông báo các vật thể xung quanh người dùng

4. **Vuốt lên** - Tín hiệu cầu cứu (Emergency Signal)
   - Gửi tín hiệu khẩn cấp và rung điện thoại để cảnh báo

5. **Vuốt xuống** - Đọc trạng thái điện thoại (Phone Status)
   - Đọc thông tin về pin, thời gian, thiết bị và kết nối mạng

## Công nghệ sử dụng

- **Android SDK 24+** (Android 7.0 trở lên)
- **CameraX** - Xử lý camera hiện đại
- **ML Kit** - Nhận diện văn bản và vật thể
- **Text-to-Speech** - Đọc thông tin cho người dùng
- **Gesture Detection** - Nhận diện cử chỉ vuốt màn hình

## Cài đặt

### Yêu cầu
- Android Studio Arctic Fox hoặc mới hơn
- Android SDK 24 trở lên
- Gradle 8.2+

### Các bước cài đặt

1. Clone repository:
```bash
git clone https://github.com/nguyenlonh/GuardVision.git
cd GuardVision
```

2. Mở project trong Android Studio

3. Sync Gradle và build project

4. Chạy trên thiết bị hoặc emulator

## Quyền truy cập

Ứng dụng yêu cầu các quyền sau:
- **CAMERA** - Để chụp ảnh và nhận diện
- **SEND_SMS** - Để gửi tin nhắn khẩn cấp (tùy chọn)
- **READ_PHONE_STATE** - Để đọc trạng thái điện thoại
- **VIBRATE** - Để rung khi có tín hiệu khẩn cấp

## Cấu trúc project

```
GuardVision/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/guardvision/app/
│   │       │   ├── MainActivity.java
│   │       │   ├── IngredientReaderActivity.java
│   │       │   ├── TextRecognitionActivity.java
│   │       │   ├── ObjectDetectionActivity.java
│   │       │   ├── EmergencyActivity.java
│   │       │   └── PhoneStatusActivity.java
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   ├── values/
│   │       │   └── mipmap/
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

## Hướng dẫn sử dụng

1. **Khởi động ứng dụng**: Mở GuardVision từ màn hình chính
2. **Nghe hướng dẫn**: Ứng dụng sẽ đọc các hướng dẫn cử chỉ
3. **Sử dụng cử chỉ**: Vuốt theo hướng dẫn để truy cập tính năng mong muốn
4. **Tương tác**: Chạm vào màn hình để chụp ảnh khi cần
5. **Quay lại**: Ứng dụng tự động quay về trang chủ sau mỗi tác vụ

## Đóng góp

Mọi đóng góp đều được chào đón! Vui lòng:
1. Fork repository
2. Tạo branch cho tính năng mới (`git checkout -b feature/TinhNangMoi`)
3. Commit thay đổi (`git commit -m 'Thêm tính năng mới'`)
4. Push lên branch (`git push origin feature/TinhNangMoi`)
5. Tạo Pull Request

## Giấy phép

Project này được phát hành dưới giấy phép MIT.

## Liên hệ

Nếu có câu hỏi hoặc đề xuất, vui lòng tạo issue trên GitHub.