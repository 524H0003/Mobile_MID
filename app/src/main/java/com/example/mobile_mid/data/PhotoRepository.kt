package com.example.mobile_mid.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PhotoRepository(private val photoDao: PhotoDao) {

    val allPhotos: Flow<List<PhotoEntity>> = photoDao.getAllPhotos()

    suspend fun insertPhoto(photo: PhotoEntity) = withContext(Dispatchers.IO) {
        photoDao.insertPhoto(photo)
    }

    suspend fun deletePhoto(photo: PhotoEntity) = withContext(Dispatchers.IO) {
        photoDao.deletePhoto(photo)
    }
}
