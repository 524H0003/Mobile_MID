package com.example.mobile_mid

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
import com.example.mobile_mid.data.AppDatabase
import com.example.mobile_mid.data.PhotoEntity
import com.example.mobile_mid.ui.PhotoAdapter
import com.example.mobile_mid.util.PerformanceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoBenchmarkTest {

    @Before
    fun setUp() {
        println("\n>>> BẮT ĐẦU TEST CASE BENCHMARK <<<")
    }

    @After
    fun tearDown() {
        println(">>> KẾT THÚC TEST CASE BENCHMARK <<<\n")
    }

    /**
     */
    private fun scrollThroughItems(totalItems: Int, recyclerView: RecyclerView): Boolean {
        var isLoading = true

        for (i in 0 until totalItems) {
            // Cuộn màn hình tới item vị trí i bằng Espresso
            onView(withId(R.id.rvPhotos))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(i))

            Thread.sleep(100)

            // Kiểm tra trạng thái ProgressBar của các child đang hiển thị trên UI Thread
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val viewHolder =
                    recyclerView.findViewHolderForAdapterPosition(i) as? PhotoAdapter.PhotoViewHolder

                val progressBar =
                    viewHolder?.binding?.imageWithProgress?.binding?.progressBar // hoặc viewHolder?.binding?.progressBar nếu bạn dùng ViewBinding

                var attempt = 1000
                while (progressBar != null && progressBar.visibility == View.VISIBLE && attempt > 0) {
                    Thread.sleep(1000)
                    attempt--
                }

                if (attempt == 0) isLoading = false
            }
        }
        return isLoading
    }

    private fun runBenchmarkScenario(count: Int, size: Int) {
        val scenarioName = "TC: $count ảnh - Kích thước: $size"
        PerformanceMonitor.resetMetrics()
        println("\n----------------------------------------------------------")
        println("Đang thực thi kịch bản: $scenarioName")

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = AppDatabase.getDatabase(context)
        val photoDao = database.photoDao()

        runBlocking(Dispatchers.IO) {
            photoDao.deleteAllPhotos()
            for (i in 0 until count) {
                val url = "http://localhost:8686/id/$i/$size/$size"
                photoDao.insertPhoto(
                    PhotoEntity(
                        url = url, title = "Benchmark #$i (${size}x$size)"
                    )
                )
            }
        }

        val runtime = Runtime.getRuntime()
        runtime.gc()
        val ramBeforeMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

        val startTime = System.currentTimeMillis()
        var detailDurationMs = 0L

        // Khởi chạy Activity
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

            if (recyclerViewRef != null && totalItems > 0) {
                // TÁC VỤ 1: Cuộn qua tất cả item trong vòng lặp và kiểm tra loading
                val isLoading = scrollThroughItems(totalItems, recyclerViewRef)
                if (isLoading) {
                    println("⚠️ CẢNH BÁO: Vẫn còn ảnh đang tải trong quá trình cuộn!")
                } else {
                    println("✓ Tất cả ảnh hiển thị đã được tải xong")
                }

                // Chọn ngẫu nhiên 1 vị trí
                val randomIndex = (0 until totalItems).shuffled().first()

                // TÁC VỤ 2: CUỘN MÀN HÌNH TỚI ITEM ĐƯỢC CHỌN NGẪU NHIÊN
                println("🔄 Đang cuộn tới item ngẫu nhiên tại vị trí: $randomIndex")
                onView(withId(R.id.rvPhotos))
                    .perform(
                        RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(
                            randomIndex
                        )
                    )

                Thread.sleep(1000) // Nghỉ 1s để giao diện dừng hẳn ở vị trí đã cuộn tới

                // Mở DetailActivity cho item đã cuộn tới
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

                // Đợi 3 giây xem ảnh trong DetailActivity
                Thread.sleep(3000)

                println("\n📊 KẾT QUẢ RIÊNG:")
                println("⏱️  Thời gian xem ảnh (DetailActivity): $detailDurationMs ms")
            }
        }

        val endTime = System.currentTimeMillis()
        val durationMs = endTime - startTime

        runtime.gc()
        val ramAfterMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val nativeHeapMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

        // In bảng kết quả tổng hợp
        val resultTable = """
        ========================================================================================================
        ===                                BẢNG KẾT QUẢ BENCHMARK TEST CASE                                  ===
        ========================================================================================================
        | Kịch bản kiểm thử                             : $scenarioName
        | Thời gian xem ảnh                             : $detailDurationMs ms
        | Thời gian chạy test case tổng cộng            : $durationMs ms
        | RAM Heap sử dụng trước khi chạy               : $ramBeforeMb MB
        | RAM Heap sử dụng sau khi chạy                 : $ramAfterMb MB
        | Native Heap cấp phát                          : $nativeHeapMb MB
        ========================================================================================================
        """.trimIndent()

        println(resultTable)
        PerformanceMonitor.logSessionSummary()

        // Ghi tổng hợp kết quả ra file CSV trên bộ nhớ thiết bị
        saveResultToFile(context, scenarioName, durationMs, detailDurationMs, ramBeforeMb, ramAfterMb, nativeHeapMb)

        val resultsBundle = Bundle().apply {
            putString("benchmark_scenario", scenarioName)
            putLong("duration_ms", durationMs)
            putLong("detail_duration_ms", detailDurationMs)
            putLong("ram_before_mb", ramBeforeMb)
            putLong("ram_after_mb", ramAfterMb)
            putLong("native_heap_mb", nativeHeapMb)
        }
        InstrumentationRegistry.getInstrumentation().sendStatus(0, resultsBundle)
    }

    val small = 500
    val large = 2000

    @Test
    fun test_10_photos_size_small() {
        runBenchmarkScenario(count = 10, size = small)
    }

    @Test
    fun test_10_photos_size_large() {
        runBenchmarkScenario(count = 10, size = large)
    }

    @Test
    fun test_100_photos_size_small() {
        runBenchmarkScenario(count = 100, size = small)
    }

    @Test
    fun test_100_photos_size_large() {
        runBenchmarkScenario(count = 100, size = large)
    }

    @Test
    fun test_1000_photos_size_small() {
        runBenchmarkScenario(count = 1000, size = small)
    }

    @Test
    fun test_1000_photos_size_large() {
        runBenchmarkScenario(count = 1000, size = large)
    }

    private fun saveResultToFile(
        context: android.content.Context,
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

        val targetDirs = listOfNotNull(
            java.io.File("/sdcard/Download"),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(null),
            context.filesDir
        ).distinct()

        for (dir in targetDirs) {
            try {
                if (!dir.exists()) dir.mkdirs()
                val csvFile = java.io.File(dir, "benchmark_results.csv")

                if (!csvFile.exists()) csvFile.writeText(csvHeader)
                csvFile.appendText(csvLine)

                println("📄 [ĐÃ XUẤT BÁO CÁO]: ${csvFile.absolutePath}")
            } catch (e: Exception) {
                println("⚠️ Không thể ghi file vào ${dir.absolutePath}: ${e.message}")
            }
        }
    }
}