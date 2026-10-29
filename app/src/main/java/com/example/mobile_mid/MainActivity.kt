package com.example.mobile_mid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mobile_mid.databinding.ActivityMainBinding
import com.example.mobile_mid.databinding.DialogAddPhotoBinding
import com.example.mobile_mid.ui.PhotoAdapter
import com.example.mobile_mid.ui.PhotoViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PhotoViewModel by viewModels()
    private lateinit var photoAdapter: PhotoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        observePhotos()
        setupListeners()
    }

    private fun setupRecyclerView() {
        photoAdapter = PhotoAdapter(
            onDeleteClick = { photo ->
                AlertDialog.Builder(this)
                    .setTitle("Xóa ảnh")
                    .setMessage("Bạn có chắc chắn muốn xóa ảnh này khỏi cơ sở dữ liệu?")
                    .setPositiveButton("Xóa") { _, _ ->
                        viewModel.deletePhoto(photo)
                        Toast.makeText(this, "Đã xóa ảnh", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )

        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = photoAdapter
        }
    }

    private fun observePhotos() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.photosState.collect { photos ->
                    photoAdapter.submitList(photos)
                    if (photos.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.rvPhotos.visibility = View.GONE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvPhotos.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            showAddPhotoDialog()
        }

        binding.btnSampleData.setOnClickListener {
            viewModel.addSamplePhotos()
            Toast.makeText(this, "Đã tải danh sách ảnh mẫu vào cơ sở dữ liệu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddPhotoDialog() {
        val dialogBinding = DialogAddPhotoBinding.inflate(LayoutInflater.from(this))

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Thêm") { _, _ ->
                val url = dialogBinding.etPhotoUrl.text.toString()
                val title = dialogBinding.etPhotoTitle.text.toString()

                if (url.isBlank()) {
                    Toast.makeText(this, "Vui lòng nhập đường dẫn URL ảnh", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addPhoto(url, title)
                    Toast.makeText(this, "Đã thêm ảnh vào cơ sở dữ liệu", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
