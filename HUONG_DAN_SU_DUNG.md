# Hướng Dẫn Sử Dụng GuardVision

## Giới Thiệu
GuardVision là ứng dụng hỗ trợ người khiếm thị thực hiện các tác vụ hàng ngày thông qua điện thoại Android. Ứng dụng sử dụng cử chỉ vuốt màn hình và giọng nói để hướng dẫn người dùng.

## Cài Đặt

### Yêu Cầu Thiết Bị
- Điện thoại Android phiên bản 7.0 trở lên
- Camera sau hoạt động tốt
- Loa hoặc tai nghe để nghe hướng dẫn

### Bước Cài Đặt
1. Tải file APK GuardVision về máy
2. Mở file APK để cài đặt
3. Cho phép cài đặt từ nguồn không xác định (nếu cần)
4. Chấp nhận các quyền cần thiết khi ứng dụng yêu cầu

### Các Quyền Cần Thiết
- **Camera**: Để chụp ảnh và nhận diện
- **SMS**: Để gửi tin nhắn khẩn cấp
- **Đọc trạng thái điện thoại**: Để báo thông tin pin, mạng
- **Rung**: Để cảnh báo khi khẩn cấp

## Bắt Đầu Sử Dụng

### Lần Đầu Mở Ứng Dụng
1. Nhấn vào biểu tượng GuardVision trên màn hình chính
2. Lắng nghe lời chào và hướng dẫn từ ứng dụng
3. Ứng dụng sẽ nói: "Chào mừng đến GuardVision. Vuốt trái một lần: Đọc thành phần..."

### Các Cử Chỉ Cơ Bản
Tất cả cử chỉ đều bắt đầu từ màn hình chính:

```
┌─────────────────────────────────────┐
│         Vuốt Lên ↑                  │
│      (Cầu Cứu)                      │
│                                     │
│  Vuốt Phải →  HOME  ← Vuốt Trái    │
│  (Nhận Diện           (Đọc Thành   │
│   Vật Thể)            Phần/Văn Bản)│
│                                     │
│         Vuốt Xuống ↓                │
│      (Trạng Thái)                   │
└─────────────────────────────────────┘
```

## Chi Tiết Các Tính Năng

### 1. Đọc Thành Phần (Vuốt Trái 1 Lần)

**Mục đích**: Đọc nhãn thành phần trên bao bì sản phẩm

**Cách sử dụng**:
1. Ở màn hình chính, vuốt ngón tay từ phải sang trái **một lần**
2. Nghe: "Đọc thành phần"
3. Camera sẽ mở tự động
4. Giữ sản phẩm trước camera, đảm bảo nhãn thành phần rõ ràng
5. Chạm một lần vào màn hình để chụp
6. Đợi vài giây
7. Ứng dụng sẽ đọc: "Thành phần: [danh sách thành phần]"
8. Tự động quay về màn hình chính

**Mẹo**:
- Giữ sản phẩm cách camera khoảng 15-20cm
- Đảm bảo ánh sáng đủ
- Giữ tay vững khi chụp

### 2. Nhận Diện Văn Bản (Vuốt Trái 2 Lần Nhanh)

**Mục đích**: Đọc mọi loại văn bản (tài liệu, biển báo, thư tín...)

**Cách sử dụng**:
1. Ở màn hình chính, vuốt trái **hai lần liên tiếp trong vòng 0.5 giây**
2. Nghe: "Nhận diện văn bản"
3. Camera sẽ mở
4. Hướng camera về văn bản cần đọc
5. Chạm màn hình để chụp
6. Đợi xử lý
7. Ứng dụng đọc: "Văn bản: [nội dung]"
8. Tự động quay về màn hình chính

**Mẹo**:
- Đặt văn bản trên mặt phẳng
- Tránh văn bản bị cong hoặc nhăn
- Chụp từ trên xuống cho chữ thẳng

### 3. Nhận Diện Vật Thể (Vuốt Phải)

**Mục đích**: Nhận biết đồ vật xung quanh

**Cách sử dụng**:
1. Ở màn hình chính, vuốt từ trái sang phải
2. Nghe: "Nhận diện vật thể"
3. Camera mở
4. Hướng camera về phía vật thể
5. Chạm màn hình
6. Ứng dụng sẽ nói: "Tìm thấy [số lượng] vật thể: [tên các vật thể]"
7. Tự động quay về màn hình chính

**Ví dụ kết quả**:
- "Tìm thấy 2 vật thể: người, ghế"
- "Tìm thấy 1 vật thể: chai"

**Các vật thể có thể nhận diện**:
- Người
- Ghế, bàn
- Chai, cốc
- Sách
- Điện thoại
- Máy tính xách tay
- Và nhiều vật thể khác...

### 4. Tín Hiệu Cầu Cứu (Vuốt Lên)

**Mục đích**: Gửi cảnh báo khẩn cấp khi cần trợ giúp

**Cách sử dụng**:
1. Ở màn hình chính, vuốt từ dưới lên trên
2. Điện thoại sẽ rung mạnh (3 lần ngắn)
3. Nghe: "Cầu cứu. Đang gửi tín hiệu cầu cứu"
4. Ứng dụng sẽ gửi SMS đến số khẩn cấp đã cài đặt
5. Nghe: "Đã gửi tín hiệu cầu cứu"
6. Tự động quay về màn hình chính

**Lưu ý quan trọng**:
- Chỉ sử dụng khi thực sự cần thiết
- Cần cấp quyền gửi SMS lần đầu sử dụng
- Số khẩn cấp mặc định: 113 (Cảnh sát Việt Nam)

**Cài đặt số khẩn cấp**:
- Hiện tại sử dụng số mặc định
- Phiên bản sau sẽ cho phép tùy chỉnh

### 5. Trạng Thái Điện Thoại (Vuốt Xuống)

**Mục đích**: Nghe thông tin về điện thoại

**Cách sử dụng**:
1. Ở màn hình chính, vuốt từ trên xuống
2. Nghe: "Trạng thái điện thoại"
3. Ứng dụng sẽ đọc:
   - Mức pin (phần trăm)
   - Trạng thái sạc
   - Thời gian hiện tại
   - Tên thiết bị
   - Nhà mạng đang kết nối
4. Tự động quay về màn hình chính

**Ví dụ**:
"Pin: 85 phần trăm, đang sạc. Thời gian: 15 giờ 30 phút, 17 tháng 10 năm 2025. Thiết bị: Samsung Galaxy S21. Mạng: Viettel"

## Xử Lý Sự Cố

### Ứng dụng không đọc gì
**Nguyên nhân**: Chưa bật âm thanh hoặc lỗi TTS
**Giải pháp**:
- Kiểm tra âm lượng điện thoại
- Vào Cài đặt → Ngôn ngữ và bàn phím → Đầu ra giọng nói
- Chọn "Google Text-to-Speech" và cài đặt tiếng Việt

### Camera không mở
**Nguyên nhân**: Chưa cấp quyền camera
**Giải pháp**:
- Vào Cài đặt → Ứng dụng → GuardVision → Quyền
- Bật quyền "Camera"

### Không nhận diện được văn bản
**Nguyên nhân**: Ánh sáng yếu hoặc chữ không rõ
**Giải pháp**:
- Bật đèn
- Giữ camera gần hơn
- Làm sạch ống kính camera
- Chụp lại

### Không nhận diện được vật thể
**Nguyên nhân**: Vật thể quá xa hoặc che khuất
**Giải pháp**:
- Di chuyển camera gần vật thể
- Đảm bảo vật thể không bị che
- Thử ở góc độ khác

### Không gửi được tin nhắn khẩn cấp
**Nguyên nhân**: Chưa cấp quyền SMS
**Giải pháp**:
- Vào Cài đặt → Ứng dụng → GuardVision → Quyền
- Bật quyền "SMS"

## Mẹo Sử Dụng Hiệu Quả

### 1. Tập Luyện Cử Chỉ
- Dành thời gian làm quen với các cử chỉ
- Tập vuốt trái 2 lần nhanh cho thành thạo
- Mỗi cử chỉ có phản hồi giọng nói rõ ràng

### 2. Môi Trường Sử Dụng
- Ánh sáng tốt cho camera
- Nơi yên tĩnh để nghe rõ hướng dẫn
- Không gian thoáng cho vuốt cử chỉ

### 3. Bảo Quản Thiết Bị
- Giữ ống kính camera sạch
- Sạc pin đầy trước khi ra ngoài
- Cài nhạc chuông đủ lớn

### 4. An Toàn
- Không sử dụng khi đang đi đường
- Ngồi xuống khi sử dụng tính năng camera
- Nhờ người thân hỗ trợ lần đầu

## Câu Hỏi Thường Gặp

### Q: Ứng dụng có hoạt động offline không?
**A**: Có, tất cả tính năng hoạt động offline. Chỉ cần kết nối mạng lúc đầu để tải ML Kit.

### Q: Ứng dụng có lưu hình ảnh không?
**A**: Không, tất cả hình ảnh chỉ xử lý tạm thời và không được lưu.

### Q: Tôi có thể thay đổi giọng đọc không?
**A**: Giọng đọc phụ thuộc vào cài đặt TTS của hệ thống Android.

### Q: Ứng dụng tiêu tốn pin nhiều không?
**A**: Không, ứng dụng chỉ sử dụng camera khi cần và tự động tắt.

### Q: Có thể sử dụng với TalkBack không?
**A**: Có, GuardVision tương thích với TalkBack và các công cụ hỗ trợ khác.

### Q: Độ chính xác nhận diện thế nào?
**A**: 
- Văn bản rõ ràng: 95%+
- Chữ viết tay: 70-80%
- Vật thể phổ biến: 90%+

### Q: Có cần kết nối internet không?
**A**: Không, sau khi cài đặt xong, tất cả hoạt động offline.

## Liên Hệ và Hỗ Trợ

### Báo Lỗi
- Tạo Issue trên GitHub: https://github.com/nguyenlonh/GuardVision/issues
- Mô tả chi tiết lỗi và cách tái hiện

### Góp Ý Cải Tiến
- Mọi góp ý đều được đón nhận
- Liên hệ qua GitHub Issues

### Đóng Góp
- Fork repository
- Tạo branch mới
- Gửi Pull Request

## Phiên Bản và Cập Nhật

### Phiên Bản Hiện Tại: 1.0
**Tính năng**:
- ✅ Đọc thành phần
- ✅ Nhận diện văn bản
- ✅ Nhận diện vật thể
- ✅ Tín hiệu cầu cứu
- ✅ Đọc trạng thái điện thoại

### Kế Hoạch Tương Lai
- Tùy chỉnh số khẩn cấp
- Lệnh giọng nói
- Chia sẻ vị trí GPS khi khẩn cấp
- Nhận diện thuốc
- Hướng dẫn đường đi

## Kết Luận

GuardVision được thiết kế để giúp người khiếm thị độc lập hơn trong cuộc sống hàng ngày. Với các cử chỉ đơn giản và phản hồi giọng nói rõ ràng, ứng dụng mong muốn trở thành công cụ hữu ích cho cộng đồng.

**Chúc bạn sử dụng hiệu quả và an toàn!**

---

*Tài liệu này có thể được cập nhật khi có phiên bản mới. Kiểm tra GitHub để có thông tin mới nhất.*
