package com.example.mobile_mid.data

import kotlinx.coroutines.flow.Flow

class PhotoRepository(private val photoDao: PhotoDao) {

    val allPhotos: Flow<List<PhotoEntity>> = photoDao.getAllPhotos()

    suspend fun insert(photo: PhotoEntity) {
        photoDao.insertPhoto(photo)
    }

    suspend fun delete(photo: PhotoEntity) {
        photoDao.deletePhoto(photo)
    }
}
