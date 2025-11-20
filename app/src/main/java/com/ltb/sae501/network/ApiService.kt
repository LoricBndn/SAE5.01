package com.ltb.sae501.network

import com.ltb.sae501.network.dto.AuthRequest
import com.ltb.sae501.network.dto.AuthResponse
import com.ltb.sae501.network.dto.CategoryResponse
import com.ltb.sae501.network.dto.RecognitionResponse
import com.ltb.sae501.network.dto.RegisterRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Authentication
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    // Recognitions
    @Multipart
    @POST("recognitions")
    suspend fun saveRecognition(
        @Part image: MultipartBody.Part,
        @Part("emotions") emotions: RequestBody,
        @Part("userId") userId: RequestBody?
    ): Response<RecognitionResponse>

    @GET("recognitions")
    suspend fun getAllRecognitions(): Response<List<RecognitionResponse>>

    @GET("recognitions/{id}")
    suspend fun getRecognitionById(@Path("id") id: String): Response<RecognitionResponse>

    @DELETE("recognitions/{id}")
    suspend fun deleteRecognition(@Path("id") id: String): Response<Void>

    // Categories
    @GET("categories")
    suspend fun getAllCategories(): Response<List<CategoryResponse>>

    @GET("categories/{id}")
    suspend fun getCategoryById(@Path("id") id: String): Response<CategoryResponse>

    @POST("categories/initialize")
    suspend fun initializeCategories(): Response<Map<String, Any>>

    // Training Images
    @Multipart
    @POST("categories/{categoryId}/training-images")
    suspend fun uploadTrainingImage(
        @Path("categoryId") categoryId: String,
        @Part image: MultipartBody.Part
    ): Response<Any>

    @DELETE("categories/{categoryId}/training-images/{imageId}")
    suspend fun deleteTrainingImage(
        @Path("categoryId") categoryId: String,
        @Path("imageId") imageId: String
    ): Response<Void>
}
