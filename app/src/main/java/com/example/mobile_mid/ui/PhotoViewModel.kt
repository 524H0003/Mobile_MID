package com.example.mobile_mid.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_mid.data.AppDatabase
import com.example.mobile_mid.data.PhotoEntity
import com.example.mobile_mid.data.PhotoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhotoRepository
    val photosState: StateFlow<List<PhotoEntity>>

    init {
        val photoDao = AppDatabase.getDatabase(application).photoDao()
        repository = PhotoRepository(photoDao)
        photosState = repository.allPhotos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addPhoto(url: String, title: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            val photo = PhotoEntity(
                url = url.trim(),
                title = if (title.isBlank()) "Ảnh không tên" else title.trim()
            )
            repository.insert(photo)
        }
    }

    fun deletePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            repository.delete(photo)
        }
    }

    fun addSamplePhotos() {
        viewModelScope.launch {
            val samples = listOf(
                PhotoEntity(url = "https://images.unsplash.com/photo-1506744038136-46273834b3fb", title = "Phong cảnh núi sông"),
                PhotoEntity(url = "https://images.unsplash.com/photo-1511884642898-4c92249e20b6", title = "Rừng thông buổi sáng"),
                PhotoEntity(url = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05", title = "Sương mù trên núi"),
                PhotoEntity(url = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d", title = "Thiên nhiên xanh tươi"),
                PhotoEntity(url = "https://images.unsplash.com/photo-1426604966848-d7adac402bff", title = "Thung lũng mơ mộng")
            )
            samples.forEach { repository.insert(it) }
        }
    }
}
