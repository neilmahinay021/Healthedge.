package com.example.healthedge.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val DEBUG_BASE_URL = "https://healthedge-api.onrender.com/api/"
    private const val PROD_BASE_URL = "https://healthedge-api.onrender.com/api/"
    
    // Set to false for release build
    private const val IS_DEBUG = false
    
    private val BASE_URL = if (IS_DEBUG) DEBUG_BASE_URL else PROD_BASE_URL
    
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
    val faceApi: FaceApi = retrofit.create(FaceApi::class.java)
} 