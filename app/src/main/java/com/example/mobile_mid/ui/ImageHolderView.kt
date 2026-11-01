package com.example.mobile_mid.ui

import android.content.Context
import android.util.Log
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import coil.ImageLoader
import com.example.mobile_mid.databinding.LayoutImageWithProgressBinding
import com.example.mobile_mid.util.PerformanceMonitor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import coil.load

/**
 * A compound view that loads images efficiently using Coil with OkHttp network stack.
 * Displays a ProgressBar while loading and handles errors gracefully.
 */
class ImageHolderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val binding: LayoutImageWithProgressBinding =
        LayoutImageWithProgressBinding.inflate(LayoutInflater.from(context), this, true)

    companion object {
        private const val TAG = "ImageHolderView"
        private const val CONNECT_TIMEOUT_SEC = 5L
        private const val READ_TIMEOUT_SEC = 15L

        @Volatile
        private var sharedImageLoader: ImageLoader? = null

        private fun getImageLoader(context: Context): ImageLoader {
            return sharedImageLoader ?: synchronized(this) {
                sharedImageLoader ?: run {
                    val okHttpClient = OkHttpClient.Builder()
                        .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                        .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                        .build()

                    ImageLoader.Builder(context.applicationContext)
                        .okHttpClient(okHttpClient)
                        .build().also { sharedImageLoader = it }
                }
            }
        }
    }

    /**
     * Loads an image from the given URL using Coil with optimizations.
     * Uses caching to reduce load time and optimize memory.
     *
     * @param url The URL of the image to load
     * @param onError Optional callback invoked when loading fails
     */
    fun loadUrl(
        url: String,
        onError: ((String) -> Unit)? = null
    ) {
        val iv = binding.ivImage
        val progressBar = binding.progressBar

        if (url.isBlank()) {
            progressBar.visibility = GONE
            onError?.invoke("Không có URL ảnh hợp lệ") ?: run {
                Toast.makeText(context, "Không có URL ảnh hợp lệ", Toast.LENGTH_SHORT).show()
            }
            return
        }

        progressBar.visibility = VISIBLE
        iv.setImageDrawable(null)

        val startTime = System.currentTimeMillis()

        // Pass the URL string directly, and configure inside the trailing lambda
        iv.load(url) {
            crossfade(true)

            // Listen for success/error states to toggle the progress bar visibility
            listener(
                onSuccess = { _, result ->
                    val endTime = System.currentTimeMillis()
                    val loadTimeMs = endTime - startTime
                    val dataSource = result.dataSource.name
                    val bitmapBytes = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.allocationByteCount?.toLong() ?: 0L
                    PerformanceMonitor.trackImageLoadTime(loadTimeMs, dataSource, bitmapBytes)
                    progressBar.visibility = GONE
                    Log.d(TAG, "Image loaded in ${loadTimeMs}ms from $dataSource")
                },
                onError = { _, errorResult ->
                    val endTime = System.currentTimeMillis()
                    val loadTimeMs = endTime - startTime
                    PerformanceMonitor.trackImageLoadTime(loadTimeMs, "ERROR")

                    val errorMsg = errorResult.throwable?.message ?: "Lỗi tải ảnh"
                    Log.e(TAG, "Error loading image: $errorMsg", errorResult.throwable)

                    val friendlyError = when {
                        errorMsg.contains("timeout", ignoreCase = true) -> "Tải ảnh quá thời gian. Vui lòng thử lại."
                        errorMsg.contains("failed to connect", ignoreCase = true) -> "Không kết nối được server. Kiểm tra mạng."
                        else -> errorMsg
                    }

                    progressBar.visibility = GONE
                    onError?.invoke(friendlyError) ?: run {
                        Toast.makeText(context, friendlyError, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    /**
     * Cancels any ongoing image loading and clears the current image.
     */
    fun clear() {
        binding.ivImage.setImageDrawable(null)
        binding.progressBar.visibility = GONE
    }
}