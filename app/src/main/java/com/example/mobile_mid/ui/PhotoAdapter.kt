package com.example.mobile_mid.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_mid.data.PhotoEntity
import com.example.mobile_mid.databinding.ItemPhotoBinding

class PhotoAdapter(
    private val onItemClick: (PhotoEntity) -> Unit,
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

        fun bind(photoItem: PhotoEntity) {
            binding.photo = photoItem
            // Load image using ImageHolderView
            binding.imageWithProgress.loadUrl(photoItem.url)

            itemView.setOnClickListener {
                onItemClick(photoItem)
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
