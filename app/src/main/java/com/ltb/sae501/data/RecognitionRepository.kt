package com.ltb.sae501.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ltb.sae501.auth.TokenManager
import com.ltb.sae501.data.local.AppDatabase
import com.ltb.sae501.data.local.LocalRecognition
import com.ltb.sae501.data.local.RecognitionDao
import com.ltb.sae501.data.models.RecognitionResult
import com.ltb.sae501.data.models.RecognizedEmotion
import com.ltb.sae501.network.RemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream

private const val TAG = "RecoRepo"

// repo pour les recos - gere online/offline
class RecognitionRepository(
    private val context: Context,
    private val dao: RecognitionDao,
    private val remoteDataSource: RemoteDataSource
) {

    companion object {
        @Volatile
        private var INSTANCE: RecognitionRepository? = null

        fun getInstance(context: Context, remoteDataSource: RemoteDataSource): RecognitionRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getDatabase(context)
                val instance = RecognitionRepository(
                    context.applicationContext,
                    database.recognitionDao(),
                    remoteDataSource
                )
                INSTANCE = instance
                instance
            }
        }
    }

    // save reco - backend si connecté, local sinon
    suspend fun saveRecognition(
        imageUri: Uri,
        emotions: List<RecognizedEmotion>,
        autoSaveEnabled: Boolean = true,
        isPublic: Boolean = false,
        showUsername: Boolean = true
    ): Boolean {
        return try {
            if (TokenManager.isLoggedIn()) {
                val imageData = readImageData(imageUri) ?: return false
                val displayName = if (isPublic && showUsername) TokenManager.getUsername() else null
                remoteDataSource.saveRecognition(imageData, emotions, isPublic, displayName)
            } else {
                // offline - save local
                val localImagePath = copyImageToInternalStorage(imageUri)
                val localRecognition = LocalRecognition.create(
                    imageLocalPath = localImagePath,
                    emotions = emotions,
                    userId = null
                )
                dao.insert(localRecognition)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "save failed", e)
            false
        }
    }

    private fun readImageData(imageUri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "read img failed", e)
            null
        }
    }

    // copie img vers internal storage
    private fun copyImageToInternalStorage(sourceUri: Uri): String {
        val detectedAt = System.currentTimeMillis()
        val filename = "recognition_$detectedAt.jpg"
        val destFile = File(context.filesDir, filename)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        return destFile.absolutePath
    }

    // sync une reco locale vers le backend
    private suspend fun syncRecognition(local: LocalRecognition): Boolean {
        return try {
            val imageFile = File(local.imageLocalPath)
            if (!imageFile.exists()) return false

            val imageData = imageFile.readBytes()
            val emotions = parseEmotionsFromJson(local.emotionsJson)
            remoteDataSource.saveRecognition(imageData, emotions, isPublic = false, displayName = null)
        } catch (e: Exception) {
            Log.e(TAG, "sync failed", e)
            false
        }
    }

    // sync toutes les recos en attente
    suspend fun syncAllPending(): Int {
        if (!TokenManager.isLoggedIn()) return 0

        val unsynced = dao.getUnsyncedRecognitions()
        if (unsynced.isEmpty()) return 0

        var successCount = 0

        unsynced.forEach { local ->
            if (syncRecognition(local)) {
                successCount++
                dao.deleteById(local.id)
                try {
                    File(local.imageLocalPath).delete()
                } catch (e: Exception) { }
            }
        }

        return successCount
    }

    // get history - backend si connecté, local sinon
    fun getHistory(): Flow<List<RecognitionResult>> {
        return if (TokenManager.isLoggedIn()) {
            remoteDataSource.getHistory()
        } else {
            dao.getAllRecognitions().map { localList ->
                localList.map { it.toRecognitionResult() }
            }
        }
    }

    fun getUnsyncedCount(): Flow<Int> = dao.getUnsyncedCount()

    fun getTotalCount(): Flow<Int> = dao.getTotalCount()

    private fun parseEmotionsFromJson(json: String): List<RecognizedEmotion> {
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<RecognizedEmotion>>() {}.type
        return gson.fromJson(json, type)
    }

    suspend fun deleteAllLocal() {
        dao.deleteAll()
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("recognition_")) {
                file.delete()
            }
        }
    }

    suspend fun deleteRecognition(id: String, imagePath: String?) {
        dao.deleteById(id)

        if (imagePath != null) {
            try {
                val file = File(imagePath)
                if (file.exists()) file.delete()
            } catch (e: Exception) { }
        }
    }
}
