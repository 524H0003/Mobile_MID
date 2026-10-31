package com.example.mobile_mid.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_mid.data.AppDatabase
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

    fun addRandomPicsumPhotos(count: Int, size: Int) {
        val safeCount = count.coerceIn(1, 1000)
        val safeSize = size.coerceIn(50, 4000)
        viewModelScope.launch {
            _isLoading.value = true
            try {
                PerformanceMonitor.measureTime("AddRandomPicsumPhotos (count=$safeCount, size=$safeSize)") {
                    withContext(Dispatchers.IO) {
                        for (i in 0 until safeCount) {
                            val randomId = (0..1084).random()
                            val url = "http://localhost:8686/id/$randomId/$safeSize/$safeSize"
                            val photo = PhotoEntity(
                                url = url,
                                title = "Ảnh ngẫu nhiên #$randomId (${safeSize}x$safeSize)"
                            )
                            repository.insertPhoto(photo)
                        }
                    }
                }
                PerformanceMonitor.logMemoryUsage()
            } finally {
                _isLoading.value = false
            }
        }
    }
}