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

    // Frame rendering metrics
    private val jankCount = AtomicInteger(0)
    private val totalFrames = AtomicInteger(0)
    private val lastLogTime = AtomicLong(0)

    // Image loading metrics
    private val totalImageLoadTimeMs = AtomicLong(0)
    private val imageLoadCount = AtomicInteger(0)

    // Cache classification metrics
    private val memoryCacheHits = AtomicInteger(0)
    private val diskCacheHits = AtomicInteger(0)
    private val networkLoads = AtomicInteger(0)
    private val unknownSourceLoads = AtomicInteger(0)

    // Bitmap memory footprint tracking
    private val totalBitmapMemoryBytes = AtomicLong(0)
    private val maxSingleBitmapBytes = AtomicLong(0)

    /**
     * Resets all accumulated metrics for scenario-isolated benchmarking.
     */
    fun resetMetrics() {
        jankCount.set(0)
        totalFrames.set(0)
        lastLogTime.set(0)
        totalImageLoadTimeMs.set(0)
        imageLoadCount.set(0)
        memoryCacheHits.set(0)
        diskCacheHits.set(0)
        networkLoads.set(0)
        unknownSourceLoads.set(0)
        totalBitmapMemoryBytes.set(0)
        maxSingleBitmapBytes.set(0)
        Log.i(TAG, "[TELEMETRY] All performance metrics have been reset.")
    }

    /**
     * Starts frame metrics monitoring (detects Jank Frames > 16.67ms during scrolling).
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
                if (now - lastLogTime.get() > 3000 && lastLogTime.compareAndSet(
                        lastLogTime.get(),
                        now
                    )
                ) {
                    Log.i(
                        TAG,
                        "[TELEMETRY] Frame stats: ${jankCount.get()} jank / ${totalFrames.get()} total frames"
                    )
                }
            }, handler)
            Log.i(TAG, "[TELEMETRY] FrameMetrics monitoring started.")
        }
    }

    /**
     * Tracks image load duration, data source (Memory/Disk/Network), and allocated bitmap size in bytes.
     */
    fun trackImageLoadTime(
        durationMs: Long,
        dataSourceName: String = "UNKNOWN",
        bitmapAllocationBytes: Long = 0L
    ) {
        if (durationMs >= 0) {
            totalImageLoadTimeMs.addAndGet(durationMs)
            imageLoadCount.incrementAndGet()

            when {
                dataSourceName.contains("MEMORY", ignoreCase = true) -> memoryCacheHits.incrementAndGet()
                dataSourceName.contains("DISK", ignoreCase = true) -> diskCacheHits.incrementAndGet()
                dataSourceName.contains("NETWORK", ignoreCase = true) -> networkLoads.incrementAndGet()
                else -> unknownSourceLoads.incrementAndGet()
            }

            if (bitmapAllocationBytes > 0) {
                totalBitmapMemoryBytes.addAndGet(bitmapAllocationBytes)
                var currentMax = maxSingleBitmapBytes.get()
                while (bitmapAllocationBytes > currentMax) {
                    if (maxSingleBitmapBytes.compareAndSet(currentMax, bitmapAllocationBytes)) break
                    currentMax = maxSingleBitmapBytes.get()
                }
            }
        }
    }

    /**
     * Logs real-time system RAM consumption (JVM Heap and Native Heap).
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
     * Prints a comprehensive session telemetry report matching all project PDF requirements.
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

        val count = imageLoadCount.get()
        val avgLoadTime = if (count > 0) (totalImageLoadTimeMs.get().toDouble() / count) else 0.0

        val memHits = memoryCacheHits.get()
        val diskHits = diskCacheHits.get()
        val netLoads = networkLoads.get()
        val cacheHitsTotal = memHits + diskHits
        val cacheHitRatio = if (count > 0) (cacheHitsTotal.toFloat() / count * 100) else 0f

        val totalBitmapMb = totalBitmapMemoryBytes.get().toDouble() / (1024 * 1024)
        val maxBitmapMb = maxSingleBitmapBytes.get().toDouble() / (1024 * 1024)
        val avgBitmapMb = if (count > 0) totalBitmapMb / count else 0.0

        Log.i(TAG, "==========================================================")
        Log.i(TAG, "===        BÁO CÁO THÔNG SỐ PHIÊN LÀM VIỆC THỰC TẾ      ===")
        Log.i(TAG, "==========================================================")
        Log.i(TAG, String.format("%-36s : %d", "Tổng số khung hình", total))
        Log.i(TAG, String.format("%-36s : %d (%.2f%%)", "Khung hình bị chậm (Jank)", janks, jankPercent))
        Log.i(TAG, String.format("%-36s : %.2f%%", "Độ mượt mà rendering", if (smoothness < 0) 0f else smoothness))
        Log.i(TAG, String.format("%-36s : %d MB / %d MB", "RAM JVM Heap sử dụng", usedMemMb, maxMemMb))
        Log.i(TAG, String.format("%-36s : %d MB", "RAM Native Heap cấp phát", nativeHeapMb))
        Log.i(TAG, String.format("%-36s : %d ảnh", "Số lượng ảnh đã tải", count))
        Log.i(TAG, String.format("%-36s : %.2f ms", "Thời gian tải ảnh trung bình", avgLoadTime))
        Log.i(TAG, "--- BỘ NHỚ ĐỆM & NGUỒN TẢI (CACHE METRICS) ---")
        Log.i(TAG, String.format("%-36s : %d (%.1f%%)", "Memory Cache (RAM) Hits", memHits, if (count > 0) memHits.toFloat() / count * 100 else 0f))
        Log.i(TAG, String.format("%-36s : %d (%.1f%%)", "Disk Cache Hits", diskHits, if (count > 0) diskHits.toFloat() / count * 100 else 0f))
        Log.i(TAG, String.format("%-36s : %d (%.1f%%)", "Network Fetch (Tải từ mạng)", netLoads, if (count > 0) netLoads.toFloat() / count * 100 else 0f))
        Log.i(TAG, String.format("%-36s : %.2f%%", "Tỉ lệ dùng Cache (Hit Ratio)", cacheHitRatio))
        Log.i(TAG, "--- GIẢI MÃ BITMAP & BỘ NHỚ (BITMAP DECODING) ---")
        Log.i(TAG, String.format("%-36s : %.2f MB", "Tổng dung lượng Bitmap cấp phát", totalBitmapMb))
        Log.i(TAG, String.format("%-36s : %.2f MB", "Dung lượng Bitmap lớn nhất", maxBitmapMb))
        Log.i(TAG, String.format("%-36s : %.2f MB", "Dung lượng Bitmap trung bình/ảnh", avgBitmapMb))
        Log.i(TAG, "==========================================================")
    }

    /**
     * Helper đo thời gian thực thi tác vụ.
     */
    inline fun <T> measureTime(actionName: String, block: () -> T): T {
        val start = System.nanoTime()
        val result = block()
        val durationMs = (System.nanoTime() - start) / 1_000_000.0
        Log.i(TAG, "[TELEMETRY] $actionName thực thi hết: ${String.format("%.2f", durationMs)} ms")
        return result
    }
}
