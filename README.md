# Mobile_MID - Android Image Loading Benchmark

Dự án demo Android để benchmark hiệu năng việc tải và hiển thị ảnh trong RecyclerView.

## Yêu cầu

- Android Studio Hedgehog (2023.1.1) trở lên
- Android SDK 37
- Docker và Docker Compose
- Thiết bị Android hoặc emulator đã bật Debug (adb)

## Cài đặt nhanh

```bash
# Build ứng dụng Android
./gradlew assembleDebug
```

## Khởi động Image Server bằng Docker Compose

```bash
docker-compose up -d
```

Docker sẽ:

- Tạo container `local-image-provider`
- Cài đặt fontconfig và dependencies
- Expose cổng 8686
- Lưu cache vào thư mục `./cache`

Để dừng server:

```bash
docker-compose down
```

## Chạy Benchmark Test

### Bước 1: Chuẩn bị môi trường

1. **Khởi động image server** (xem hướng dẫn ở trên)

2. **Kết nối thiết bị** và kiểm tra:

   ```bash
   adb devices
   ```

### Bước 2: Cấu hình ADB Reverse

Để thiết bị Android truy cập được image server trên máy host:

```bash
adb reverse tcp:8686 tcp:8686
```

> **Lưu ý**: Lệnh này cần thiết bị đã được kết nối và đang ở chế độ Debug.

### Bước 3: Cài đặt ứng dụng trên thiết bị

```bash
./gradlew installDebug
```

### Bước 4: Chạy các test case benchmark

Chạy tất cả test case benchmark:

```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mobile_mid.PhotoBenchmarkTest
```

### Bước 5: Lấy kết quả benchmark

Sau khi test hoàn tất, lấy file kết quả:

```bash
adb pull /sdcard/Download/benchmark_results.csv .
```

Mở file `benchmark_results.csv` để xem các chỉ số:

- Thời gian chạy test case tổng cộng
- Thời gian xem ảnh (DetailActivity)
- RAM Heap sử dụng trước và sau khi chạy
- Native Heap cấp phát

## Test Case có sẵn

| Test Case | Số lượng ảnh | Kích thước ảnh |
| ----------- | ------------- | ---------------- |
| `test_10_photos_size_small` | 10 | 500x500 |
| `test_10_photos_size_large` | 10 | 2000x2000 |
| `test_100_photos_size_small` | 100 | 500x500 |
| `test_100_photos_size_large` | 100 | 2000x2000 |
| `test_1000_photos_size_small` | 1000 | 500x500 |
| `test_1000_photos_size_large` | 1000 | 2000x2000 |

## Lưu ý quan trọng

1. **Image Server**: Phải chạy trước khi chạy test, nếu không test sẽ thất bại do không tải được ảnh.

2. **ADB Reverse**: Lệnh `adb reverse tcp:8686 tcp:8686` rất quan trọng - cho phép thiết bị truy cập localhost của máy host.

3. **Quyền lưu trữ**: Thiết bị cần đủ quyền để ghi file vào thư mục Download.

4. **GC và bộ nhớ**: Test sử dụng `Runtime.gc()` để đo lường bộ nhớ, kết quả có thể thay đổi tùy vào trạng thái hệ thống.
