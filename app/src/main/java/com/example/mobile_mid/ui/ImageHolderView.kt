package com.example.mobile_mid.ui

import android.content.Context
import android.util.Log
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import coil.load
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.example.mobile_mid.databinding.LayoutImageWithProgressBinding
import com.example.mobile_mid.util.PerformanceMonitor

/**
 * A compound view that loads images efficiently using Coil.
 * Displays a ProgressBar while loading and handles errors gracefully.
 */
class ImageHolderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutImageWithProgressBinding =
        LayoutImageWithProgressBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * Loads an image from the given URL using Coil with optimizations.
     *
     * @param url The URL of the image to load
     * @param onError Optional callback invoked when loading fails
     * @param placeHolderResId Optional placeholder drawable resource ID
     */
    fun loadUrl(
        url: String,
        placeHolderResId: Int? = null,
        onError: ((String) -> Unit)? = null,
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
            placeHolderResId?.let { placeholder(it) }

            // Listen for success/error states to toggle the progress bar visibility
            listener(
                onSuccess = { _, _ ->
                    val endTime = System.currentTimeMillis()
                    val loadTimeMs = endTime - startTime
                    PerformanceMonitor.trackImageLoadTime(loadTimeMs)
                    progressBar.visibility = GONE
                    Log.d(TAG, "Image loaded successfully in ${loadTimeMs}ms")
                },
                onError = { _, result ->
                    val endTime = System.currentTimeMillis()
                    val loadTimeMs = endTime - startTime
                    PerformanceMonitor.trackImageLoadTime(loadTimeMs)
                    val errorMsg = result.throwable.message ?: "Lỗi tải ảnh"
                    Log.e(TAG, "Error loading image: $errorMsg", result.throwable)
                    progressBar.visibility = GONE
                    onError?.invoke(errorMsg) ?: run {
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
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

    companion object {
        private const val TAG = "ImageHolderView"
    }
}