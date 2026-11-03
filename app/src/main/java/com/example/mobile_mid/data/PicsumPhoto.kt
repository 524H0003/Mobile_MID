package com.example.mobile_mid.data

import com.squareup.moshi.Json

data class PicsumPhoto(
    @Json(name = "id") val id: String,
    @Json(name = "author") val author: String,
    @Json(name = "width") val width: Int,
    @Json(name = "height") val height: Int,
    @Json(name = "url") val url: String,
    @Json(name = "download_url") val downloadUrl: String
)