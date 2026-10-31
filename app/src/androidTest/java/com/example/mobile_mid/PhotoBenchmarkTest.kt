package com.example.mobile_mid

import android.os.Bundle
import android.os.Debug
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mobile_mid.data.AppDatabase
import com.example.mobile_mid.data.PhotoEntity
import com.example.mobile_mid.ui.PhotoAdapter
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
     * Kiểm tra xem có ảnh nào trong RecyclerView đang hiển thị ProgressBar (đang tải) hay không
     */
    private fun isAnyImageLoading(recyclerView: RecyclerView): Boolean {
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val progressBar = child.findViewById<ProgressBar>(R.id.progressBar)
            if (progressBar != null && progressBar.visibility == View.VISIBLE) {
                return true
            }
        }
        return false
    }

    private fun runBenchmarkScenario(count: Int, size: Int) {
        val scenarioName = "TC: $count ảnh - Kích thước: $size"
        println("\n----------------------------------------------------------")
        println("Đang thực thi kịch bản: $scenarioName")

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = AppDatabase.getDatabase(context)
        val photoDao = database.photoDao()

        // 1. Chuẩn bị dữ liệu danh sách ảnh thử nghiệm (Xóa sạch CSDL cũ để đảm bảo tính Lũy Đẳng)
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

        // 2. Khởi chạy Activity, chờ tất cả ảnh được tải xong
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val recyclerView = activity.findViewById<RecyclerView>(R.id.rvPhotos)
                val photoAdapter = recyclerView?.adapter as? PhotoAdapter
                val totalItems = photoAdapter?.itemCount ?: 0

                if (totalItems > 0 && photoAdapter != null) {
                    // Chờ cho RecyclerView hoàn tất setup và tất cả ảnh đã tải xong
                    Thread.sleep(3000) // Đợi 3 giây để RecyclerView setup và tải tất cả ảnh

                    // Kiểm tra xem có ảnh nào đang tải không
                    val isLoading = isAnyImageLoading(recyclerView)
                    if (isLoading) {
                        println("⚠️ CẢNH BÁO: Vẫn còn ảnh đang tải sau 3 giây!")
                    } else {
                        println("✓ Tất cả ảnh đã được tải xong")
                    }

                    // Lấy 1 vị trí ngẫu nhiên duy nhất trong danh sách
                    val randomIndex = (0 until totalItems).shuffled().take(1).first()

                    // Mở 1 ảnh ngẫu nhiên vào DetailActivity
                    recyclerView.scrollToPosition(randomIndex)
                    Thread.sleep(1000) // Đợi ảnh được chọn hiển thị xong

                    val photo = photoAdapter.currentList.getOrNull(randomIndex)

                    if (photo != null) {
                        // BẮT ĐẦU KHỞI ĐỘNG THỜI ĐO XEM ẢNH
                        val startTimeDetail = System.currentTimeMillis()

                        DetailActivity.start(activity, photo.url, photo.title)

                        Thread.sleep(3000) // Đợi 3 giây để xem ảnh trong DetailActivity

                        // KẾT THÚC THỜI ĐO XEM ẢNH
                        val endTimeDetail = System.currentTimeMillis()
                        detailDurationMs = endTimeDetail - startTimeDetail

                        // In kết quả riêng biệt thời gian xem ảnh
                        println("\n📊 KẾT QUẢ RIÊNG:")
                        println("⏱️  Thời gian xem ảnh (DetailActivity): $detailDurationMs ms")
                        System.out.println("⏱️  View detail time: $detailDurationMs ms")
                    }
                }
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
        System.out.println(resultTable)

        // Gửi thông số tới Android Studio Test Runner Status
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

    val small = 1000
    val large = 4000

    // --- BỘ TEST CASE CHO 10 ẢNH ---
    @Test
    fun test_10_photos_size_small() {
        runBenchmarkScenario(count = 10, size = small)
    }

    @Test
    fun test_10_photos_size_large() {
        runBenchmarkScenario(count = 10, size = large)
    }

    // --- BỘ TEST CASE CHO 100 ẢNH ---
    @Test
    fun test_100_photos_size_small() {
        runBenchmarkScenario(count = 100, size = small)
    }

    @Test
    fun test_100_photos_size_large() {
        runBenchmarkScenario(count = 100, size = large)
    }

    // --- BỘ TEST CASE CHO 1.000 ẢNH ---
    @Test
    fun test_1000_photos_size_small() {
        runBenchmarkScenario(count = 1000, size = small)
    }

    @Test
    fun test_1000_photos_size_large() {
        runBenchmarkScenario(count = 1000, size = large)
    }
}