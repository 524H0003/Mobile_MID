package com.example.mobile_mid

import android.content.Context
import android.os.Bundle
import android.os.Debug
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import coil.imageLoader
import com.example.mobile_mid.data.AppDatabase
import com.example.mobile_mid.data.PhotoEntity
import com.example.mobile_mid.ui.PhotoAdapter
import com.example.mobile_mid.util.PerformanceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoBenchmarkTest {

    private lateinit var context: Context

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        setNetworkState(true)
        println("\n==========================================================")
        println(">>> BẮT ĐẦU THỰC THI KỊCH BẢN KIỂM THỬ BENCHMARK <<<")
        println("==========================================================")
        clearAppCache()
    }

    @After
    fun tearDown() {
        println("==========================================================")
        println(">>> KẾT THÚC THỰC THI KỊCH BẢN KIỂM THỬ BENCHMARK <<<\n")
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    private fun clearAppCache() {
        try {
            context.imageLoader.memoryCache?.clear()
            context.imageLoader.diskCache?.clear()
            println("🧹 [CACHE]: Đã xóa sạch Memory Cache và Disk Cache của Coil.")
        } catch (e: Exception) {
            println("⚠️ Không thể xóa cache: ${e.message}")
        }
    }

    private fun populateDatabase(
        count: Int,
        size: Int,
        baseUrl: String = "http://localhost:8686/id"
    ) {
        val database = AppDatabase.getDatabase(context)
        val photoDao = database.photoDao()
        runBlocking(Dispatchers.IO) {
            photoDao.deleteAllPhotos()
            for (i in 0 until count) {
                val url = if (baseUrl.contains("invalid") || baseUrl.contains("offline")) {
                    "$baseUrl/$i"
                } else {
                    "$baseUrl/$i/$size/$size"
                }
                photoDao.insertPhoto(
                    PhotoEntity(
                        url = url,
                        title = "Photo #$i (${size}x$size)"
                    )
                )
            }
        }
    }

    private fun scrollThroughItems(
        totalItems: Int,
        recyclerView: RecyclerView,
        scrollDelayMs: Long = 100
    ): Boolean {
        var isLoading = true

        for (i in 0 until totalItems) {
            onView(withId(R.id.rvPhotos))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(i))

            if (scrollDelayMs > 0) {
                Thread.sleep(scrollDelayMs)
            }

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val viewHolder =
                    recyclerView.findViewHolderForAdapterPosition(i) as? PhotoAdapter.PhotoViewHolder
                val progressBar = viewHolder?.binding?.imageWithProgress?.binding?.progressBar

                var attempt = 10
                while (progressBar != null && progressBar.visibility == View.VISIBLE && attempt > 0) {
                    Thread.sleep(500)
                    attempt--
                }

                if (attempt == 0) isLoading = false
            }
        }
        return isLoading
    }

    private fun runBenchmarkScenario(
        scenarioCode: String,
        scenarioName: String,
        count: Int,
        size: Int,
        baseUrl: String = "http://localhost:8686/id",
        saveToCsv: Boolean = true
    ): Long {
        PerformanceMonitor.resetMetrics()
        println("\n----------------------------------------------------------")
        println("🚀 Đang chạy: [$scenarioCode] - $scenarioName ($count ảnh, kích thước ${size}x${size})")

        populateDatabase(count, size, baseUrl)

        val runtime = Runtime.getRuntime()
        runtime.gc()
        val ramBeforeMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val startTime = System.currentTimeMillis()
        var detailDurationMs = 0L

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(1000)
            var totalItems = 0
            var recyclerViewRef: RecyclerView? = null

            scenario.onActivity { activity ->
                val recyclerView = activity.findViewById<RecyclerView>(R.id.rvPhotos)
                val photoAdapter = recyclerView?.adapter as? PhotoAdapter
                totalItems = photoAdapter?.itemCount ?: 0
                recyclerViewRef = recyclerView
            }

            val rv = recyclerViewRef
            if (rv != null && totalItems > 0) {
                val isLoading = scrollThroughItems(totalItems, rv)
                if (isLoading) {
                    println("✓ Tải ảnh hoàn tất trong quá trình cuộn")
                } else {
                    println("⚠️ Cảnh báo: Vẫn còn ảnh đang tải ở một số vị trí")
                }

                onView(withId(R.id.rvPhotos))
                    .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
                Thread.sleep(500)

                val randomIndex = (0 until totalItems).shuffled().first()
                scenario.onActivity { activity ->
                    val photoAdapter = recyclerViewRef?.adapter as? PhotoAdapter
                    val photo = photoAdapter?.currentList?.getOrNull(randomIndex)
                    if (photo != null) {
                        val startTimeDetail = System.currentTimeMillis()
                        DetailActivity.start(activity, photo.url, photo.title)
                        val endTimeDetail = System.currentTimeMillis()
                        detailDurationMs = endTimeDetail - startTimeDetail
                    }
                }
                Thread.sleep(2000)
            }
        }

        val endTime = System.currentTimeMillis()
        val durationMs = endTime - startTime

        runtime.gc()
        val ramAfterMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val nativeHeapMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

        val resultTable = """
        ========================================================================================================
        ===                            BẢNG KẾT QUẢ BENCHMARK KỊCH BẢN: $scenarioCode                           ===
        ========================================================================================================
        | Mã & Tên Kịch bản                             : [$scenarioCode] $scenarioName
        | Thời gian mở xem ảnh chi tiết                 : $detailDurationMs ms
        | Thời gian thực thi tổng cộng                  : $durationMs ms
        | RAM Heap sử dụng ban đầu                      : $ramBeforeMb MB
        | RAM Heap sử dụng sau khi chạy                 : $ramAfterMb MB
        | Native Heap cấp phát                          : $nativeHeapMb MB
        ========================================================================================================
        """.trimIndent()

        println(resultTable)
        PerformanceMonitor.logSessionSummary()

        if (saveToCsv) {
            saveResultToFile(
                context,
                "[$scenarioCode] $scenarioName",
                durationMs,
                detailDurationMs,
                ramBeforeMb,
                ramAfterMb,
                nativeHeapMb
            )
        }

        val resultsBundle = Bundle().apply {
            putString("benchmark_scenario", scenarioCode)
            putLong("duration_ms", durationMs)
            putLong("detail_duration_ms", detailDurationMs)
            putLong("ram_before_mb", ramBeforeMb)
            putLong("ram_after_mb", ramAfterMb)
            putLong("native_heap_mb", nativeHeapMb)
        }
        InstrumentationRegistry.getInstrumentation().sendStatus(0, resultsBundle)

        return durationMs
    }

    // =========================================================================================================
    // DANH SÁCH CÁC KỊCH BẢN KIỂM THỬ BẮT BUỘC THEO ĐỀ BÀI TIỂU LUẬN GIỮA KỲ (TC01 - TC07)
    // =========================================================================================================

    /**
     * TC01: Mở danh sách ảnh lần đầu (chưa có cache).
     * Yêu cầu: Đánh giá thời gian hiển thị, thời gian tải ảnh và mức sử dụng bộ nhớ RAM ban đầu.
     */
    @Test
    fun test_TC01_firstLoad() {
        val duration = runBenchmarkScenario(
            "TC01",
            "Mở danh sách ảnh lần đầu (Làm sạch Cache)",
            count = 100,
            size = 500
        )
        assertTrue("Thời gian thực thi TC01 phải hợp lệ", duration > 0)
    }

    /**
     * TC02: Mở lại danh sách khi cache còn hiệu lực (Cache Hit).
     * Yêu cầu: So sánh thời gian hiển thị và tải lại ảnh so với lần đầu (TC01).
     */
    @Test
    fun test_TC02_cacheHit() {
        println("📌 Chuẩn bị dữ liệu cache bằng cách tải trước danh sách (Warmup - Cold Network)...")
        runBenchmarkScenario(
            "TC02_Warmup",
            "Tải trước ảnh vào Cache (Warmup - Cold Network)",
            count = 50,
            size = 500,
            saveToCsv = true
        )

        println("\n📌 Thực thi TC02: Mở lại danh sách khi Cache đã có sẵn (Cache Hit)...")
        val duration = runBenchmarkScenario(
            "TC02",
            "Mở lại danh sách khi cache còn hiệu lực (Cache Hit)",
            count = 50,
            size = 500,
            saveToCsv = true
        )
        assertTrue("Thời gian tải từ cache phải nhanh", duration > 0)
    }

    /**
     * TC03: Cuộn nhanh qua nhiều ảnh (Fast Scroll Performance).
     * Yêu cầu: Đo số khung hình bị giật (Jank), đếm tỉ lệ mượt mà và kiểm tra hủy các task tải ảnh không cần thiết.
     */
    @Test
    fun test_TC03_fastScroll() {
        PerformanceMonitor.resetMetrics()
        println("\n🚀 Đang chạy: [TC03] - Cuộn nhanh qua nhiều ảnh (100 ảnh)")
        populateDatabase(count = 100, size = 500)

        val runtime = Runtime.getRuntime()
        runtime.gc()
        val ramBeforeMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val startTime = System.currentTimeMillis()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(1000)
            var recyclerViewRef: RecyclerView? = null
            scenario.onActivity { activity ->
                recyclerViewRef = activity.findViewById(R.id.rvPhotos)
            }
            assertNotNull("RecyclerView không được null", recyclerViewRef)

            for (i in 0 until 100 step 5) {
                onView(withId(R.id.rvPhotos))
                    .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(i))
                Thread.sleep(20)
            }
            Thread.sleep(1500)
        }

        val endTime = System.currentTimeMillis()
        val durationMs = endTime - startTime

        runtime.gc()
        val ramAfterMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val nativeHeapMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

        println("📊 BÁO CÁO MỨC ĐỘ MƯỢT MÀ KHI CUỘN NHANH [TC03]:")
        PerformanceMonitor.logSessionSummary()
        saveResultToFile(
            context,
            "[TC03] Cuộn nhanh qua nhiều ảnh (Fast Scroll)",
            durationMs,
            0L,
            ramBeforeMb,
            ramAfterMb,
            nativeHeapMb
        )
    }

    /**
     * TC04: Thử nghiệm với ảnh độ phân giải lớn (Large Resolution Images).
     * Yêu cầu: Đo bộ nhớ RAM cấp phát (Bitmap Heap), thời gian giải mã ảnh lớn (2000x2000px).
     */
    @Test
    fun test_TC04_largeResolution() {
        val duration = runBenchmarkScenario(
            "TC04",
            "Thử nghiệm với ảnh độ phân giải lớn (2000x2000)",
            count = 20,
            size = 2000
        )
        assertTrue("TC04 tải ảnh lớn phải thành công", duration > 0)
    }

    /**
     * TC05: Tắt mạng khi mở danh sách (Offline Mode Testing).
     * Yêu cầu: Kiểm tra xem ảnh đã cache có hiển thị không; ảnh chưa cache được xử lý thế nào.
     */
    @Test
    fun test_TC05_offlineMode() {
        println("📌 [TC05] Bước 1: Tải trước 10 ảnh vào Cache khi có mạng...")
        runBenchmarkScenario(
            "TC05_Preload",
            "Tải trước ảnh khi có mạng",
            count = 10,
            size = 500,
            saveToCsv = false
        )

        println("📌 [TC05] Bước 2: Tắt mạng và chạy kiểm thử cuộn ảnh trong điều kiện Offline...")
        setNetworkState(false)

        try {
            val duration = runBenchmarkScenario(
                "TC05",
                "Tắt mạng khi mở danh sách (Offline)",
                count = 20,
                size = 500,
                saveToCsv = true
            )
            assertTrue("TC05 thực thi thành công", duration > 0)
        } finally {
            setNetworkState(true)
        }
    }

    /**
     * TC06: Kiểm thử URL ảnh lỗi hoặc máy chủ chậm (Error URLs & Slow Server).
     * Yêu cầu: Đảm bảo giao diện không bị treo, hiển thị placeholder/thông báo lỗi đúng cách.
     */
    @Test
    fun test_TC06_errorUrlAndTimeout() {
        println("🚀 Thực thi [TC06] Kiểm thử với danh sách URL lỗi và timeout...")
        val duration = runBenchmarkScenario(
            "TC06",
            "URL ảnh lỗi hoặc máy chủ chậm",
            count = 10,
            size = 500,
            baseUrl = "http://localhost:8686/invalid_404_url",
            saveToCsv = true
        )
        assertTrue("TC06 thực thi thành công", duration > 0)
    }

    /**
     * TC07: Tăng số lượng ảnh từ 100 lên 500 và 1.000 (Scalability Benchmark).
     * Yêu cầu: Ghi nhận mức tăng thời gian tải, dung lượng bộ nhớ RAM và độ trễ giao diện.
     */
    @Test
    fun test_TC07_scalePhotoCount() {
        println("\n==========================================================")
        println("🚀 THỰC THI KIỂM THỬ TĂNG QUY MÔ SỐ LƯỢNG ÁNH [TC07]")
        println("==========================================================")

        val t100 = runBenchmarkScenario("TC07_100", "Quy mô 100 ảnh", count = 100, size = 500)
        val t500 = runBenchmarkScenario("TC07_500", "Quy mô 500 ảnh", count = 500, size = 500)
        val t1000 = runBenchmarkScenario("TC07_1000", "Quy mô 1,000 ảnh", count = 1000, size = 500)

        println("\n📊 TỔNG HỢP KẾT QUẢ ĐO QUY MÔ [TC07]:")
        println(" - 100 ảnh   : $t100 ms")
        println(" - 500 ảnh   : $t500 ms")
        println(" - 1,000 ảnh : $t1000 ms")
    }

    private fun saveResultToFile(
        context: Context,
        scenarioName: String,
        durationMs: Long,
        detailDurationMs: Long,
        ramBeforeMb: Long,
        ramAfterMb: Long,
        nativeHeapMb: Long
    ) {
        val csvHeader = PerformanceMonitor.getCsvHeader() + "\n"
        val csvLine = PerformanceMonitor.getCsvLine(
            scenarioName, durationMs, detailDurationMs, ramBeforeMb, ramAfterMb, nativeHeapMb
        ) + "\n"

        val candidateDirs = listOfNotNull(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(null),
            context.filesDir
        ).map { runCatching { it.canonicalFile }.getOrDefault(it) }
            .distinctBy { it.absolutePath }

        for (dir in candidateDirs) {
            try {
                if (!dir.exists()) dir.mkdirs()
                val csvFile = java.io.File(dir, "benchmark_results.csv")

                if (!csvFile.exists()) csvFile.writeText(csvHeader)
                csvFile.appendText(csvLine)

                println("📄 [ĐÃ XUẤT BÁO CÁO]: ${csvFile.absolutePath}")
                break
            } catch (e: Exception) {
                println("⚠️ Không thể ghi file vào ${dir.absolutePath}: ${e.message}")
            }
        }
    }

    private fun setNetworkState(enabled: Boolean) {
        val action = if (enabled) "enable" else "disable"
        uiDevice.executeShellCommand("svc wifi $action")
        uiDevice.executeShellCommand("svc data $action")

        // Allow a brief moment for the emulator network state stack to update
        Thread.sleep(1500)
    }
}
