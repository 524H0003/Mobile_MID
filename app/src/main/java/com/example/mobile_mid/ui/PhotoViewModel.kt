package com.example.mobile_mid.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_mid.data.AppDatabase
import com.example.mobile_mid.data.NetworkModule
import com.example.mobile_mid.data.PhotoEntity
import com.example.mobile_mid.data.PhotoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhotoRepository
    private val apiService = NetworkModule.picsumApiService
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
        var formattedUrl = url.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }
        viewModelScope.launch {
            val photo = PhotoEntity(
                url = formattedUrl,
                title = if (title.isBlank()) "Ảnh không tên" else title.trim()
            )
            repository.insertPhoto(photo)
        }
    }

    fun deletePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
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
            samples.forEach { repository.insertPhoto(it) }
        }
    }

    fun addPicsumPhotos(count: Int, size: Int) {
        val safeCount = count.coerceIn(1, 100)
        val safeSize = size.coerceIn(100, 2000)
        viewModelScope.launch {
            try {
                val response = apiService.getPhotos(page = 2, limit = safeCount)
                if (response.isSuccessful && response.body() != null) {
                    val photos = response.body()!!.map { picumPhoto ->
                        val url = picumPhoto.downloadUrl
                        val title = orDefault(picumPhoto.author, "Ảnh từ Picsum")
                        PhotoEntity(url = url, title = title)
                    }
                    photos.forEach { repository.insertPhoto(it) }
                }
            } catch (e: Exception) {
                // Fallback to random URLs if API fails
                val fallbackPhotos = (1..safeCount).map { index ->
                    PhotoEntity(
                        url = "https://picsum.photos/$safeSize/$safeSize?random=$index",
                        title = "Picsum Photo $index"
                    )
                }
                fallbackPhotos.forEach { repository.insertPhoto(it) }
            }
        }
    }

    private fun orDefault(value: String?, defaultValue: String): String {
        return if (value.isNullOrBlank()) defaultValue else value
    }
}