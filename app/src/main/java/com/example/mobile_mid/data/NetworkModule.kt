package com.example.mobile_mid.data

object NetworkModule {
    const val BASE_URL = "http://localhost:8080/"

    // Cờ kiểm soát trạng thái mạng cho mục đích kiểm thử (hỗ trợ cả adb reverse localhost)
    var isOffline = false
}
