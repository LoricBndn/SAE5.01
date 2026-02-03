package com.ltb.sae501.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ltb.sae501.auth.TokenManager
import com.ltb.sae501.data.models.EmotionCategory
import com.ltb.sae501.data.models.RecognitionResult
import com.ltb.sae501.data.models.RecognizedEmotion
import com.ltb.sae501.network.dto.AuthRequest
import com.ltb.sae501.network.dto.FeedItemResponse
import com.ltb.sae501.network.dto.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

private const val TAG = "RemoteDS"

class RemoteDataSource(private val context: Context) {
    private val apiService = RetrofitClient.apiService

    suspend fun register(username: String, email: String, password: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(username, password, email)
            val response = apiService.register(request)
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    val token = authResponse.token
                    val userId = authResponse.userId
                    if (token != null && userId != null) {
                        TokenManager.saveToken(token, userId, username)
                        return@withContext Result.success(true)
                    }
                }
            }
            val errorBody = response.errorBody()?.string()
            Result.failure(Exception(errorBody ?: "Échec de l'inscription"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = AuthRequest(username, password)
            val response = apiService.login(request)
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    val token = authResponse.token
                    val userId = authResponse.userId
                    if (token != null && userId != null) {
                        TokenManager.saveToken(token, userId, username)
                        return@withContext Result.success(true)
                    }
                }
            }
            val errorBody = response.errorBody()?.string()
            Result.failure(Exception(errorBody ?: "Identifiants invalides"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        TokenManager.clearToken()
    }

    suspend fun saveRecognition(
        imageData: ByteArray,
        recognizedEmotions: List<RecognizedEmotion>,
        isPublic: Boolean = false,
        displayName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestFile = imageData.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", "recognition.jpg", requestFile)

            val emotionsJson = buildEmotionsJson(recognizedEmotions)
            val emotionsBody = emotionsJson.toRequestBody("text/plain".toMediaTypeOrNull())
            val isPublicBody = isPublic.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val displayNameBody = displayName?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.saveRecognition(imagePart, emotionsBody, isPublicBody, displayNameBody)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "save reco failed", e)
            false
        }
    }

    fun getFeed(): Flow<List<FeedItemResponse>> = flow {
        while (true) {
            try {
                val response = apiService.getFeed()
                if (response.isSuccessful) {
                    emit(response.body() ?: emptyList())
                }
            } catch (e: Exception) { }
            delay(5000)
        }
    }

    fun getHistory(): Flow<List<RecognitionResult>> = flow {
        while (true) {
            try {
                val response = apiService.getAllRecognitions()
                if (response.isSuccessful) {
                    val recognitions = response.body()?.map { dto ->
                        RecognitionResult(
                            id = dto.id,
                            userId = dto.userId,
                            imageData = null,
                            imageLocalPath = null,
                            imageUrl = buildImageUrl("/api/files/recognition/${dto.id}"),
                            detectedAt = dto.detectedAt,
                            recognizedEmotions = dto.recognizedEmotions.map { emotionDto ->
                                RecognizedEmotion(
                                    id = emotionDto.id ?: "",
                                    recognitionId = dto.id,
                                    emotionId = emotionDto.emotionId,
                                    confidence = emotionDto.confidence,
                                    detectedAt = dto.detectedAt
                                )
                            }
                        )
                    } ?: emptyList()
                    emit(recognitions)
                }
            } catch (e: Exception) { }
            delay(3000)
        }
    }

    fun getCategories(): Flow<List<EmotionCategory>> = flow {
        var failures = 0
        val maxFailures = 3

        while (true) {
            try {
                val response = apiService.getAllCategories()
                if (response.isSuccessful) {
                    failures = 0
                    val categories = response.body()?.map { dto ->
                        EmotionCategory(
                            id = dto.id,
                            name = dto.name,
                            nameEn = dto.nameEn,
                            emoji = dto.emoji,
                            color = dto.color,
                            description = dto.description,
                            imageCount = dto.imageCount,
                            createdAt = dto.createdAt,
                            updatedAt = dto.updatedAt,
                            trainingImages = dto.trainingImages
                        )
                    } ?: emptyList()
                    emit(categories.sortedBy { it.name })
                } else {
                    failures++
                    if (failures >= maxFailures) {
                        emit(emptyList())
                        break
                    }
                }
            } catch (e: Exception) {
                failures++
                if (failures >= maxFailures) {
                    emit(emptyList())
                    break
                }
            }
            delay(5000)
        }
    }

    suspend fun uploadTrainingImage(categoryId: String, imageUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val imageFile = uriToFile(imageUri)
            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

            val response = apiService.uploadTrainingImage(categoryId, imagePart)
            imageFile.delete()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteTrainingImage(categoryId: String, imageUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val imageId = extractImageIdFromUrl(imageUrl) ?: return@withContext false
            val response = apiService.deleteTrainingImage(categoryId, imageId)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun initializeDefaultCategories(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.initializeCategories()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)
        inputStream?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    private fun buildEmotionsJson(emotions: List<RecognizedEmotion>): String {
        val emotionStrings = emotions.joinToString(",") { emotion ->
            """{"emotionId":"${emotion.emotionId}","confidence":${emotion.confidence}}"""
        }
        return "[$emotionStrings]"
    }

    private fun buildImageUrl(relativeUrl: String): String {
        if (relativeUrl.startsWith("http")) return relativeUrl
        val baseUrl = com.ltb.sae501.BuildConfig.API_BASE_URL
            .removeSuffix("/api/")
            .removeSuffix("/")
        return "$baseUrl$relativeUrl"
    }

    private fun extractImageIdFromUrl(url: String): String? {
        return url.substringAfterLast("/").takeIf { it.isNotBlank() }
    }

    suspend fun startTraining(): Map<String, String>? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.startTraining()
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTrainingStatus(): com.ltb.sae501.network.dto.TrainingStatusResponse? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTrainingStatus()
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadCustomModel(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.downloadCustomModel()
            if (response.isSuccessful) response.body()?.bytes() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadAllTrainingImages(): com.ltb.sae501.network.dto.TrainingImagesResponse? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.downloadAllTrainingImages()
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }
}
