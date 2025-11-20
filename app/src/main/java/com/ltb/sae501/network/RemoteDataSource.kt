package com.ltb.sae501.network

import android.content.Context
import android.net.Uri
import com.ltb.sae501.auth.TokenManager
import com.ltb.sae501.data.models.EmotionCategory
import com.ltb.sae501.data.models.RecognitionResult
import com.ltb.sae501.data.models.RecognizedEmotion
import com.ltb.sae501.network.dto.AuthRequest
import com.ltb.sae501.network.dto.RecognizedEmotionDto
import com.ltb.sae501.network.dto.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class RemoteDataSource(private val context: Context) {
    private val apiService = RetrofitClient.apiService

    // Authentication methods
    suspend fun register(username: String, email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(username, password, email)
            val response = apiService.register(request)
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    val token = authResponse.token
                    val userId = authResponse.userId
                    if (token != null && userId != null) {
                        TokenManager.saveToken(token, userId)
                        return@withContext true
                    }
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun login(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = AuthRequest(username, password)
            val response = apiService.login(request)
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    val token = authResponse.token
                    val userId = authResponse.userId
                    if (token != null && userId != null) {
                        TokenManager.saveToken(token, userId)
                        return@withContext true
                    }
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun logout() {
        TokenManager.clearToken()
    }

    suspend fun saveRecognition(
        imageUri: Uri,
        recognizedEmotions: List<RecognizedEmotion>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Convert URI to File and create multipart body
            val imageFile = uriToFile(imageUri)
            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

            // Convert emotions list to JSON string
            val emotionsJson = buildEmotionsJson(recognizedEmotions)
            val emotionsBody = emotionsJson.toRequestBody("text/plain".toMediaTypeOrNull())

            // User ID (hardcoded for now, will be replaced with actual authentication)
            val userIdBody = "user_test_sae501".toRequestBody("text/plain".toMediaTypeOrNull())

            // Make API call
            val response = apiService.saveRecognition(imagePart, emotionsBody, userIdBody)

            // Clean up temp file
            imageFile.delete()

            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Récupère l'historique (polling toutes les 3s)
    fun getHistory(): Flow<List<RecognitionResult>> = flow {
        while (true) {
            try {
                val response = apiService.getAllRecognitions()
                if (response.isSuccessful) {
                    val recognitions = response.body()?.map { dto ->
                        RecognitionResult(
                            id = dto.id,
                            timestamp = dto.timestamp,
                            imageStorageUrl = buildImageUrl(dto.imageUrl),
                            recognizedEmotions = dto.recognizedEmotions.map { emotionDto ->
                                RecognizedEmotion(
                                    emotion = emotionDto.emotion,
                                    confidence = emotionDto.confidence
                                )
                            },
                            userId = dto.userId
                        )
                    } ?: emptyList()

                    emit(recognitions)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Polling interval: 3 seconds
            delay(3000)
        }
    }

    fun getCategories(): Flow<List<EmotionCategory>> = flow {
        var consecutiveFailures = 0
        val maxFailures = 3

        while (true) {
            try {
                val response = apiService.getAllCategories()
                if (response.isSuccessful) {
                    consecutiveFailures = 0 // Reset counter on success
                    val categories = response.body()?.map { dto ->
                        EmotionCategory(
                            id = dto.id,
                            name = dto.name,
                            nameEn = dto.nameEn,
                            emoji = dto.emoji,
                            color = dto.color,
                            description = dto.description,
                            imageCount = dto.imageCount,
                            trainingImages = dto.trainingImages.map { buildImageUrl(it) },
                            createdAt = dto.createdAt,
                            lastUpdated = dto.lastUpdated
                        )
                    } ?: emptyList()

                    emit(categories.sortedBy { it.name })
                } else {
                    consecutiveFailures++
                    if (consecutiveFailures >= maxFailures) {
                        // Emit empty list to signal error state
                        emit(emptyList())
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                consecutiveFailures++
                if (consecutiveFailures >= maxFailures) {
                    // Emit empty list and stop polling after multiple failures
                    emit(emptyList())
                    break
                }
            }

            // Polling interval: 5 seconds
            delay(5000)
        }
    }

    suspend fun uploadTrainingImage(categoryId: String, imageUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val imageFile = uriToFile(imageUri)
            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

            val response = apiService.uploadTrainingImage(categoryId, imagePart)

            // Clean up temp file
            imageFile.delete()

            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteTrainingImage(categoryId: String, imageUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Extract image ID from URL
            val imageId = extractImageIdFromUrl(imageUrl)
            if (imageId == null) return@withContext false

            val response = apiService.deleteTrainingImage(categoryId, imageId)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun initializeDefaultCategories(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.initializeCategories()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
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
            """{"emotion":"${emotion.emotion}","confidence":${emotion.confidence}}"""
        }
        return "[$emotionStrings]"
    }

    private fun buildImageUrl(relativeUrl: String): String {
        // If URL is already absolute, return as is
        if (relativeUrl.startsWith("http")) {
            return relativeUrl
        }

        // Otherwise, build full URL
        val baseUrl = RetrofitClient.apiService.toString()
            .substringBefore("/api/")
            .replace("Proxy", "")
            .trim()

        return "http://10.0.2.2:8080$relativeUrl"
    }

    private fun extractImageIdFromUrl(url: String): String? {
        // Extract image ID from URL like "/api/files/training/{imageId}"
        return url.substringAfterLast("/").takeIf { it.isNotBlank() }
    }
}
