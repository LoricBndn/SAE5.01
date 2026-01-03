package com.ltb.sae501.data

import android.content.Context
import android.net.Uri
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

/**
 * Repository centralisé pour gérer les reconnaissances
 * Implémente le pattern offline-first : sauvegarde locale puis sync si connecté
 */
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
    
    /**
     * Sauvegarde une reconnaissance (offline-first)
     * 1. Copie l'image dans le stockage interne
     * 2. Sauvegarde localement dans Room
     * 3. Si connecté et autoSaveEnabled: synchronise immédiatement
     */
    suspend fun saveRecognition(
        imageUri: Uri,
        emotions: List<RecognizedEmotion>,
        autoSaveEnabled: Boolean = true
    ): Boolean {
        return try {
            val localImagePath = copyImageToInternalStorage(imageUri)
            val userId = TokenManager.getUserId()
            val localRecognition = LocalRecognition.create(
                imageLocalPath = localImagePath,
                emotions = emotions,
                userId = userId
            )
            
            dao.insert(localRecognition)
            
            if (autoSaveEnabled && TokenManager.isLoggedIn()) {
                val syncResult = syncRecognition(localRecognition)
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Copie une image du cache vers le stockage interne
     */
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
    
    /**
     * Synchronise une reconnaissance avec le backend
     */
    private suspend fun syncRecognition(local: LocalRecognition): Boolean {
        return try {
            val imageFile = File(local.imageLocalPath)
            
            if (!imageFile.exists()) {
                return false
            }
            
            val imageData = imageFile.readBytes()
            val emotions = parseEmotionsFromJson(local.emotionsJson)
            val success = remoteDataSource.saveRecognition(imageData, emotions)
            
            if (success) {
                dao.update(local.copy(isSynced = true, remoteId = local.id))
            }
            
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Synchronise toutes les reconnaissances non synchronisées
     * Appelé lors de la connexion
     */
    suspend fun syncAllPending(): Int {
        if (!TokenManager.isLoggedIn()) {
            return 0
        }
        val unsynced = dao.getUnsyncedRecognitions()
        
        if (unsynced.isEmpty()) {
            return 0
        }
        
        var successCount = 0
        
        val userId = TokenManager.getUserId()
        if (userId != null) {
            dao.assignUserToLocalRecognitions(userId)
        }
        
        unsynced.forEachIndexed { index, local ->
            val result = syncRecognition(local)
            if (result) {
                successCount++
            }
        }
        
        return successCount
    }
    
    /**
     * Récupère l'historique des reconnaissances
     * Combine données locales et distantes (pour l'instant uniquement locales)
     */
    fun getHistory(): Flow<List<RecognitionResult>> {
        return dao.getAllRecognitions().map { localList ->
            localList.map { it.toRecognitionResult() }
        }
    }
    
    /**
     * Obtient le nombre de reconnaissances non synchronisées
     */
    fun getUnsyncedCount(): Flow<Int> = dao.getUnsyncedCount()
    
    /**
     * Obtient le nombre total de reconnaissances
     */
    fun getTotalCount(): Flow<Int> = dao.getTotalCount()
    
    /**
     * Parse le JSON des émotions
     */
    private fun parseEmotionsFromJson(json: String): List<RecognizedEmotion> {
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<RecognizedEmotion>>() {}.type
        return gson.fromJson(json, type)
    }
    
    /**
     * Supprime toutes les données locales
     */
    suspend fun deleteAllLocal() {
        dao.deleteAll()
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("recognition_")) {
                file.delete()
            }
        }
    }

    /**
     * Supprime une reconnaissance spécifique (BDD + Fichier local)
     */
    suspend fun deleteRecognition(id: String, imagePath: String?) {
        dao.deleteById(id)
        
        if (imagePath != null) {
            try {
                val file = File(imagePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
