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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoBenchmarkTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        setNetworkState(true)
        clearAppCache()
    }

    @After
    fun tearDown() {
        PerformanceMonitor.logSessionSummary()
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    private fun clearAppCache() {
        try {
            context.imageLoader.memoryCache?.clear()
            context.imageLoader.diskCache?.clear()
        } catch (_: Exception) {}
    }

    /**
     * Thống nhất lưu file vào /sdcard/Download (thư mục công khai dễ dàng adb pull)
     * với cơ chế fallback tự động sang external/internal files dir nếu cần.
     */
    private fun writeToFileInDirs(fileName: String, action: (java.io.File) -> Unit) {
        val candidateDirs = listOfNotNull(
            java.io.File("/sdcard/Download"),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(null),
            context.filesDir
        ).map { runCatching { it.canonicalFile }.getOrDefault(it) }
            .distinctBy { it.absolutePath }

        for (dir in candidateDirs) {
            try {
                if (!dir.exists()) dir.mkdirs()
                val targetFile = java.io.File(dir, fileName)
                action(targetFile)
                println("✅ [ĐÃ LƯU THÀNH CÔNG]: ${targetFile.absolutePath}")
                break
            } catch (e: Exception) {
                println("⚠️ Thử lưu vào ${dir.absolutePath} thất bại: ${e.message}")
            }
        }
    }

    private fun captureScreenshot(name: String) {
        try {
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            if (bitmap != null) {
                writeToFileInDirs("$name.png") { file ->
                    java.io.FileOutputStream(file).use { fos ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos)
                        fos.flush()
                    }
                }
                bitmap.recycle()
            }
        } catch (e: Exception) {
            println("⚠️ Lỗi chụp màn hình: ${e.message}")
        }
    }

    private fun populateDatabase(count: Int, size: Int, isError: Boolean = false) {
        val photoDao = AppDatabase.getDatabase(context).photoDao()
        runBlocking(Dispatchers.IO) {
            photoDao.deleteAllPhotos()
            for (i in 0 until count) {
                val url = if (isError) "https://invalid-picsum-404.test/$i" else "https://picsum.photos/id/${i % 1084}/$size"
                val title = if (isError) "Error #$i" else "Photo #${i % 1084} (${size}x$size)"
                photoDao.insertPhoto(PhotoEntity(url = url, title = title))
            }
        }
    }

    private fun scrollThroughItems(totalItems: Int, recyclerView: RecyclerView): Boolean {
        var complete = true
        for (i in 0 until totalItems) {
            onView(withId(R.id.rvPhotos)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(i))
            Thread.sleep(80)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val holder = recyclerView.findViewHolderForAdapterPosition(i) as? PhotoAdapter.PhotoViewHolder
                val pb = holder?.binding?.imageWithProgress?.binding?.progressBar
                var attempts = 10
                while (pb != null && pb.visibility == View.VISIBLE && attempts > 0) {
                    Thread.sleep(300)
                    attempts--
                }
                if (attempts == 0) complete = false
            }
        }
        return complete
    }

    private fun runBenchmarkScenario(code: String, name: String, count: Int, size: Int, isError: Boolean = false, saveCsv: Boolean = true): Long {
        PerformanceMonitor.resetMetrics()
        populateDatabase(count, size, isError)

        val runtime = Runtime.getRuntime()
        runtime.gc()
        val ramBefore = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val startTime = System.currentTimeMillis()
        var detailDuration = 0L

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(800)
            var totalItems = 0
            var rvRef: RecyclerView? = null
            scenario.onActivity {
                rvRef = it.findViewById(R.id.rvPhotos)
                totalItems = (rvRef?.adapter as? PhotoAdapter)?.itemCount ?: 0
            }

            rvRef?.let { rv ->
                if (totalItems > 0) {
                    scrollThroughItems(totalItems, rv)
                    if (code == "TC05") captureScreenshot("tc05_offline_list")

                    onView(withId(R.id.rvPhotos)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
                    Thread.sleep(400)

                    val rand = (0 until totalItems).shuffled().first()
                    scenario.onActivity { activity ->
                        val photo = (rv.adapter as? PhotoAdapter)?.currentList?.getOrNull(rand)
                        if (photo != null) {
                            val detailStart = System.currentTimeMillis()
                            DetailActivity.start(activity, photo.url, photo.title)
                            detailDuration = System.currentTimeMillis() - detailStart
                        }
                    }
                    Thread.sleep(1500)
                    if (code == "TC05") captureScreenshot("tc05_offline_detail")
                }
            }
        }

        val duration = System.currentTimeMillis() - startTime
        runtime.gc()
        val ramAfter = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val nativeHeap = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

        if (saveCsv) saveResult(code, name, duration, detailDuration, ramBefore, ramAfter, nativeHeap)
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("scenario", code)
            putLong("duration", duration)
        })
        return duration
    }

    @Test
    fun test_TC01_firstLoad() {
        assertTrue(runBenchmarkScenario("TC01", "Cold Start (Clean Cache)", 100, 500) > 0)
    }

    @Test
    fun test_TC02_cacheHit() {
        runBenchmarkScenario("TC02_Warmup", "Warmup Cache", 50, 500)
        assertTrue(runBenchmarkScenario("TC02", "Cache Hit", 50, 500) > 0)
    }

    @Test
    fun test_TC03_fastScroll() {
        PerformanceMonitor.resetMetrics()
        populateDatabase(100, 500)
        val start = System.currentTimeMillis()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(800)
            var rv: RecyclerView? = null
            scenario.onActivity { rv = it.findViewById(R.id.rvPhotos) }
            rv?.let {
                for (i in 0 until 100 step 5) {
                    onView(withId(R.id.rvPhotos)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(i))
                    Thread.sleep(15)
                }
            }
            Thread.sleep(1000)
        }
        val duration = System.currentTimeMillis() - start
        val rt = Runtime.getRuntime()
        rt.gc()
        saveResult("[TC03]", "Fast Scroll", duration, 0L, (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024), (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024), Debug.getNativeHeapAllocatedSize() / (1024 * 1024))
    }

    @Test
    fun test_TC04_largeResolution() {
        assertTrue(runBenchmarkScenario("TC04", "Large Resolution (2000x2000)", 20, 2000) > 0)
    }

    @Test
    fun test_TC05_offlineMode() {
        runBenchmarkScenario("TC05_Preload", "Preload Cache", 10, 500, saveCsv = false)
        setNetworkState(false)
        try {
            assertTrue(runBenchmarkScenario("TC05", "Offline Mode", 20, 500) > 0)
        } finally {
            setNetworkState(true)
        }
    }

    @Test
    fun test_TC06_errorUrlAndTimeout() {
        assertTrue(runBenchmarkScenario("TC06", "Error URLs / Timeout", 10, 500, isError = true) > 0)
    }

    @Test
    fun test_TC07_scalePhotoCount() {
        runBenchmarkScenario("TC07_100", "Scale 100", 100, 500)
        runBenchmarkScenario("TC07_500", "Scale 500", 500, 500)
        runBenchmarkScenario("TC07_1000", "Scale 1000", 1000, 500)
    }

    private fun saveResult(code: String, name: String, dur: Long, detailDur: Long, ramBefore: Long, ramAfter: Long, native: Long) {
        val line = PerformanceMonitor.getCsvLine("[$code] $name", dur, detailDur, ramBefore, ramAfter, native) + "\n"
        val header = PerformanceMonitor.getCsvHeader() + "\n"
        writeToFileInDirs("benchmark_results.csv") { file ->
            if (!file.exists()) file.writeText(header)
            file.appendText(line)
        }
    }

    private fun setNetworkState(enabled: Boolean) {
        val action = if (enabled) "enable" else "disable"
        uiDevice.executeShellCommand("svc wifi $action")
        uiDevice.executeShellCommand("svc data $action")
        Thread.sleep(1200)
    }
}
