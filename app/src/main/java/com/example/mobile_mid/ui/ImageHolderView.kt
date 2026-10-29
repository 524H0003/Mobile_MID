package com.example.mobile_mid.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import com.example.mobile_mid.databinding.LayoutImageWithProgressBinding
import java.net.HttpURLConnection
import java.net.URL

/**
 * A simple custom compound view holding an ImageView and a loading ProgressBar,
 * with no redirect handling or bitmap optimization.
 */
class ImageHolderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutImageWithProgressBinding =
        LayoutImageWithProgressBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * Loads an image from the given URL simply and displays it on the ImageView.
     */
    fun loadUrl(url: String, onError: ((String) -> Unit)? = null) {
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

        // Use a Thread to avoid blocking the main thread
        Thread {
            var connection: HttpURLConnection? = null
            var bitmap: Bitmap? = null
            try {
                val urlObj = URL(url)
                connection = urlObj.openConnection() as HttpURLConnection
                connection.connectTimeout = REQUEST_TIMEOUT
                connection.readTimeout = REQUEST_TIMEOUT
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { inputStream ->
                        bitmap = BitmapFactory.decodeStream(inputStream)
                    }
                } else {
                    Log.w(TAG, "Server returned HTTP status: ${connection.responseCode}")
                }

                // Update UI on main thread
                iv.post {
                    progressBar.visibility = GONE
                    if (bitmap != null) {
                        iv.setImageBitmap(bitmap)
                    } else {
                        val errorMsg = "Không thể tải ảnh"
                        onError?.invoke(errorMsg) ?: run {
                            Toast.makeText(
                                context,
                                errorMsg,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception loading bitmap from $url: ${e.message}", e)
                progressBar.post {
                    progressBar.visibility = GONE
                    onError?.invoke("Không thể tải ảnh: ${e.message}") ?: run {
                        Toast.makeText(
                            context,
                            "Không thể tải ảnh",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } finally {
                try {
                    connection?.disconnect()
                } catch (ignored: Exception) {}
            }
        }.start()
    }

    companion object {
        private const val TAG = "ImageHolderView"
        private const val REQUEST_TIMEOUT = 15000
    }
}
