package com.ltb.sae501.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ltb.sae501.data.models.RecognitionResult
import com.ltb.sae501.data.models.RecognizedEmotion
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Entité Room pour stocker les reconnaissances localement
 * Permet le mode offline-first avec synchronisation automatique
 */
@Entity(tableName = "recognitions")
data class LocalRecognition(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val imageLocalPath: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val emotionsJson: String,
    val isSynced: Boolean = false,
    val remoteId: String? = null,
) {
    /**
     * Convertit LocalRecognition en RecognitionResult pour l'affichage
     */
    fun toRecognitionResult(): RecognitionResult {
        val gson = Gson()
        val emotionListType = object : TypeToken<List<RecognizedEmotion>>() {}.type
        val emotions: List<RecognizedEmotion> = gson.fromJson(emotionsJson, emotionListType)
        
        return RecognitionResult(
            id = remoteId ?: id,
            userId = userId,
            imageStorageUrl = "file://$imageLocalPath",
            detectedAt = detectedAt,
            recognizedEmotions = emotions,
        )
    }
    
    companion object {
        /**
         * Crée un LocalRecognition à partir des données de capture
         */
        fun create(
            imageLocalPath: String,
            emotions: List<RecognizedEmotion>,
            userId: String? = null
        ): LocalRecognition {
            val gson = Gson()
            val emotionsJson = gson.toJson(emotions)
            
            return LocalRecognition(
                imageLocalPath = imageLocalPath,
                emotionsJson = emotionsJson,
                userId = userId
            )
        }
    }
}
