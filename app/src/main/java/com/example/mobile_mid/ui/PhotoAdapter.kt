package com.example.mobile_mid.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_mid.data.PhotoEntity
import com.example.mobile_mid.databinding.ItemPhotoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class PhotoAdapter(
    private val onDeleteClick: (PhotoEntity) -> Unit
) : ListAdapter<PhotoEntity, PhotoAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var imageLoadJob: Job? = null

        fun bind(photoItem: PhotoEntity) {
            binding.photo = photoItem

            // Hủy job tải ảnh cũ nếu view được tái sử dụng
            imageLoadJob?.cancel()
            binding.ivPhoto.setImageDrawable(null)

            // Tải ảnh trực tiếp từ URL bằng Kotlin Coroutines (không dùng thư viện ngoài)
            imageLoadJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL(photoItem.url)
                    val bitmap: Bitmap? = url.openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            binding.ivPhoto.setImageBitmap(bitmap)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(photoItem)
            }
            binding.executePendingBindings()
        }
    }

    class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoEntity>() {
        override fun areItemsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity): Boolean {
            return oldItem == newItem
        }
    }
}
