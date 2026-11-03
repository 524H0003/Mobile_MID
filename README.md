# Mobile_MID - Android Image Loading Benchmark

Dự án demo Android để benchmark hiệu năng việc tải và hiển thị ảnh trong RecyclerView.

## Yêu cầu

- Android Studio Hedgehog (2023.1.1) trở lên
- Android SDK 37
- Thiết bị Android hoặc emulator đã bật Debug (adb)

## Cài đặt nhanh

```bash
# Build ứng dụng Android
./gradlew assembleDebug
```

## Chạy Benchmark Test

### Bước 1: Chuẩn bị môi trường

1. **Khởi động image server** (xem hướng dẫn ở trên)

2. **Kết nối thiết bị** và kiểm tra:

   ```bash
   adb devices
   ```

### Bước 2: Chạy các test case benchmark

Chạy tất cả test case benchmark:

```bash
./gradlew connectedAndroidTest
```

### Bước 3: Lấy kết quả benchmark

Sau khi test hoàn tất, lấy file kết quả:

```bash
adb pull /sdcard/Download/benchmark_results.csv .
adb pull /sdcard/Download/tc05_offline_list.png .
adb pull /sdcard/Download/tc05_offline_detail.png .
```

Mở file `benchmark_results.csv` để xem các chỉ số.

## Lưu ý quan trọng

1. **Image Server**: Phải chạy trước khi chạy test, nếu không test sẽ thất bại do không tải được ảnh.

2. **ADB Reverse**: Lệnh `adb reverse tcp:8686 tcp:8686` rất quan trọng - cho phép thiết bị truy cập localhost của máy host.

3. **Quyền lưu trữ**: Thiết bị cần đủ quyền để ghi file vào thư mục Download.

4. **GC và bộ nhớ**: Test sử dụng `Runtime.gc()` để đo lường bộ nhớ, kết quả có thể thay đổi tùy vào trạng thái hệ thống.
