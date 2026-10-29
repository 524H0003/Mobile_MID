package com.example.mobile_mid.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_mid.data.AppDatabase
import com.example.mobile_mid.data.NetworkModule
import com.example.mobile_mid.data.PhotoEntity
import com.example.mobile_mid.data.PhotoRepository
import com.example.mobile_mid.util.PerformanceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhotoRepository
    private val apiService = NetworkModule.picsumApiService
    val photosState: StateFlow<List<PhotoEntity>>

    private val _isLoading = MutableStateFlow(false)
    val isLoadingState: StateFlow<Boolean> = _isLoading.asStateFlow()

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
                url = url,
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

    fun addRandomPhotos(count: Int, size: Int) {
        val safeCount = count.coerceIn(1, 1000)
        val safeSize = size.coerceIn(50, 4000)
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.getPhotos(page = 2, limit = safeCount)
                if (response.isSuccessful && response.body() != null) {
                    val photos = response.body()!!.map { picumPhoto ->
                        val url = "https://picsum.photos/id/${picumPhoto.id}/$safeSize"
                        val title = orDefault(picumPhoto.author, "Ảnh từ Picsum")
                        PhotoEntity(url = url, title = title)
                    }
                    photos.forEach { repository.insertPhoto(it) }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun orDefault(value: String?, defaultValue: String): String {
        return if (value.isNullOrBlank()) defaultValue else value
    }
}
