# Mobile_MID - Android Image Loading Benchmark

Dự án demo Android để benchmark hiệu năng việc tải và hiển thị ảnh trong RecyclerView.

## Yêu cầu

- Android Studio Hedgehog (2023.1.1) trở lên
- Android SDK 37
- Thiết bị Android hoặc emulator đã bật Debug (adb)

## Chuẩn bị môi trường

### Khởi động image server

```bash
docker compose up -d
```

### Liệt kê máy ảo android

```bash
# Đối với Windows
%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe -list-avds

# Đối với macOS / Linux
~/Library/Android/sdk/emulator/emulator -list-avds
```

### Lệnh khởi chạy máy ảo

```bash
# Đối với Windows
%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe -avd Ten_May_Ao

# Đối với macOS / Linux
~/Library/Android/sdk/emulator/emulator -avd Ten_May_Ao
```

Thêm `-wipe-data` khi muốn xóa dữ liệu của máy ảo

### Kết nối thiết bị và kiểm tra

```bash
adb devices
```

### Thiết lập kết nối Localhost

Để cho phép ứng dụng trên máy ảo/thiết bị Android truy cập vào các dịch vụ chạy trên máy tính host (`localhost`), sử dụng lệnh ADB Reverse:

```bash
adb reverse tcp:8080 tcp:8080
```

## Chạy Benchmark Test

Chạy tất cả test case benchmark:

```bash
./gradlew connectedAndroidTest
```

### Lấy kết quả benchmark

Sau khi test hoàn tất, lấy file kết quả:

```bash
adb pull /sdcard/Download/benchmark_results.csv .
adb pull /sdcard/Download/tc05_offline_list.png .
adb pull /sdcard/Download/tc05_offline_detail.png .
```

## Xuất file apk

```bash
./gradlew assembleRelease
```

File apk được xuất tại `.\app\build\outputs\apk\release\app-release-unsigned.apk`

## Chạy ứng dụng

```bash
./gradlew installDebug
```
