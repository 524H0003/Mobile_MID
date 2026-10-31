package com.example.mobile_mid.util

import android.app.Activity
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object PerformanceMonitor {
    const val TAG = "PerformanceMonitor"
    private val jankCount = AtomicInteger(0)
    private val totalFrames = AtomicInteger(0)
    private val lastLogTime = AtomicLong(0)

    // Bổ sung ghi nhận thời gian tải ảnh và dung lượng mạng
    private val totalNetworkBytes = AtomicLong(0)
    private val totalImageLoadTimeMs = AtomicLong(0)
    private val imageLoadCount = AtomicInteger(0)

    /**
     * Bắt đầu theo dõi frame render (phát hiện Jank Frames khi cuộn)
     */
    fun startFrameMetrics(activity: Activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val window: Window = activity.window
            val handler = Handler(Looper.getMainLooper())
            window.addOnFrameMetricsAvailableListener({ _, frameMetrics, _ ->
                val durationNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
                val durationMs = durationNs / 1_000_000f
                totalFrames.incrementAndGet()
                if (durationMs > 16.67f) {
                    jankCount.incrementAndGet()
                }
                val now = System.currentTimeMillis()
                if (now - lastLogTime.get() > 3000 && lastLogTime.compareAndSet(lastLogTime.get(), now)) {
                    Log.i(TAG, "[TELEMETRY] Frame stats: ${jankCount.get()} jank / ${totalFrames.get()} total frames")
                }
            }, handler)
            Log.i(TAG, "[TELEMETRY] FrameMetrics monitoring started.")
        }
    }

    /**
     * Ghi nhận dung lượng mạng đã tải (bytes)
     */
    fun trackNetworkUsage(bytes: Long) {
        if (bytes > 0) {
            totalNetworkBytes.addAndGet(bytes)
        }
    }

    /**
     * Ghi nhận thời gian tải của một bức ảnh (milliseconds)
     */
    fun trackImageLoadTime(durationMs: Long) {
        if (durationMs >= 0) {
            totalImageLoadTimeMs.addAndGet(durationMs)
            imageLoadCount.incrementAndGet()
        }
    }

    /**
     * Ghi nhận và log dung lượng RAM tiêu thụ thực tế
     */
    fun logMemoryUsage(tag: String = TAG) {
        val runtime = Runtime.getRuntime()
        val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val totalMemMb = runtime.totalMemory() / (1024 * 1024)
        val maxMemMb = runtime.maxMemory() / (1024 * 1024)
        val nativeHeapMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

        Log.i(tag, "=== [REAL-TIME RAM METRICS] ===")
        Log.i(tag, "JVM Heap Used : $usedMemMb MB / $totalMemMb MB (Max: $maxMemMb MB)")
        Log.i(tag, "Native Heap   : $nativeHeapMb MB")
    }

    /**
     * Xuất báo cáo đo lường thông số thực tế duy nhất khi đóng ứng dụng
     */
    fun logSessionSummary() {
        val total = totalFrames.get()
        val janks = jankCount.get()
        val jankPercent = if (total > 0) (janks.toFloat() / total * 100) else 0f
        val smoothness = 100f - jankPercent

        val runtime = Runtime.getRuntime()
        val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemMb = runtime.maxMemory() / (1024 * 1024)
        val nativeHeapMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

        val totalKb = totalNetworkBytes.get() / 1024.0
        val totalMb = totalKb / 1024.0
        val count = imageLoadCount.get()
        val avgLoadTime = if (count > 0) (totalImageLoadTimeMs.get().toDouble() / count) else 0.0

        Log.i(TAG, "==========================================================")
        Log.i(TAG, "===        BÁO CÁO THÔNG SỐ PHIÊN LÀM VIỆC THỰC TẾ      ===")
        Log.i(TAG, "==========================================================")
        Log.i(TAG, String.format("%-32s : %d", "Tổng số khung hình", total))
        Log.i(TAG, String.format("%-32s : %d (%.2f%%)", "Khung hình bị chậm", janks, jankPercent))
        Log.i(TAG, String.format("%-32s : %.2f%%", "Độ mượt mà rendering", if (smoothness < 0) 0f else smoothness))
        Log.i(TAG, String.format("%-32s : %d MB / %d MB", "RAM JVM Heap sử dụng", usedMemMb, maxMemMb))
        Log.i(TAG, String.format("%-32s : %d MB", "RAM Native Heap cấp phát", nativeHeapMb))
        Log.i(TAG, String.format("%-32s : %d ảnh", "Số lượng ảnh đã tải", count))
        Log.i(TAG, String.format("%-32s : %.2f ms", "Thời gian tải ảnh trung bình", avgLoadTime))
        Log.i(TAG, String.format("%-32s : %.2f MB (%.2f KB)", "Dung lượng mạng đã sử dụng", totalMb, totalKb))
        Log.i(TAG, "==========================================================")
    }

    /**
     * Đo lường thời gian thực thi thao tác thực tế
     */
    inline fun <T> measureTime(actionName: String, block: () -> T): T {
        val start = System.nanoTime()
        val result = block()
        val durationMs = (System.nanoTime() - start) / 1_000_000.0
        Log.i(TAG, "[TELEMETRY] $actionName thực thi hết: ${String.format("%.2f", durationMs)} ms")
        return result
    }
}
