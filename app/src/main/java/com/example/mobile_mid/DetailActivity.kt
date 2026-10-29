package com.example.mobile_mid

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobile_mid.databinding.ActivityDetailBinding

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
            Toast.makeText(this, "Không có URL ảnh hợp lệ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFullPhoto(urlString: String) {
        binding.imageWithProgress.loadUrl(urlString) { errorMessage ->
            Toast.makeText(this@DetailActivity, errorMessage, Toast.LENGTH_SHORT).show()
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
