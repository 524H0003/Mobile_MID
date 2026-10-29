package com.example.mobile_mid

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.mobile_mid.databinding.ActivityDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val photoUrl = intent.getStringExtra(EXTRA_URL)
        val photoTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Chi tiết ảnh"

        binding.toolbar.title = photoTitle
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.tvDetailTitle.text = photoTitle
        binding.tvDetailUrl.text = photoUrl ?: ""

        if (!photoUrl.isNullOrBlank()) {
            loadFullPhoto(photoUrl)
        } else {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Không có URL ảnh hợp lệ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFullPhoto(urlString: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = downloadLargeBitmap(urlString)
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                if (bitmap != null) {
                    binding.ivFullPhoto.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(this@DetailActivity, "Không thể tải ảnh kích thước lớn", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadLargeBitmap(urlString: String): Bitmap? {
        return try {
            var currentUrl = urlString
            var connection: HttpURLConnection? = null
            var redirects = 0

            while (redirects < 5) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == 307 || status == 308
                ) {
                    val newUrl = connection.getHeaderField("Location")
                    if (newUrl.isNullOrEmpty()) break
                    currentUrl = newUrl
                    connection.disconnect()
                    redirects++
                } else {
                    break
                }
            }

            connection?.inputStream?.use { stream ->
                val bytes = stream.readBytes()
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                // Scale to fit screen max 1920px to avoid OOM while preserving high quality
                val reqWidth = 1920
                val reqHeight = 1920
                var inSampleSize = 1
                if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"

        fun start(context: Context, url: String, title: String) {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
            }
            context.startActivity(intent)
        }
    }
}
