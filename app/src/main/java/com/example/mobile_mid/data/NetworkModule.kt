package com.example.mobile_mid.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException

object NetworkModule {
    const val BASE_URL = "http://localhost:8080/"

    // Cờ kiểm soát trạng thái mạng cho mục đích kiểm thử (hỗ trợ cả adb reverse localhost)
    var isOffline = false

    private val offlineInterceptor = Interceptor { chain ->
        if (isOffline) {
            throw IOException("Network is offline (Simulated)")
        }
        chain.proceed(chain.request())
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(offlineInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val picsumApiService: PicsumApiService = retrofit.create(PicsumApiService::class.java)
}