# Mobile_MID - Android Image Loading Benchmark

Dự án demo Android để benchmark hiệu năng việc tải và hiển thị ảnh trong RecyclerView.

## Yêu cầu

- Android Studio Hedgehog (2023.1.1) trở lên
- Android SDK 37
- Thiết bị Android hoặc emulator đã bật Debug (adb)

## Thiết lập kết nối Localhost

Để cho phép ứng dụng trên máy ảo/thiết bị Android truy cập vào các dịch vụ chạy trên máy tính host (`localhost`), sử dụng lệnh ADB Reverse:

```bash
adb reverse tcp:8080 tcp:8080
```

## Chạy Benchmark Test

### Bước 1: Chuẩn bị môi trường

1. **Khởi động image server**

```bash
docker compose up -d
```

1. **Kết nối thiết bị** và kiểm tra:

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
