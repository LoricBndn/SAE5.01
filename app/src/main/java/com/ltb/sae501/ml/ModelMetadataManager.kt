package com.ltb.sae501.ml

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class ModelMetadataManager(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val modelsDir = File(context.filesDir, "models")
    private val metadataFile = File(modelsDir, "model_metadata.json")

    init {
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
    }

    fun saveMetadata(metadata: ModelMetadata) {
        try {
            val json = gson.toJson(metadata)
            metadataFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadMetadata(): ModelMetadata? {
        return try {
            if (metadataFile.exists()) {
                val json = metadataFile.readText()
                gson.fromJson(json, ModelMetadata::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun customModelExists(): Boolean {
        val modelFile = File(modelsDir, "custom_emotion_model.tflite")
        return modelFile.exists() && modelFile.length() > 0
    }

    fun getCustomModelPath(): String {
        return File(modelsDir, "custom_emotion_model.tflite").absolutePath
    }

    fun deleteCustomModel(): Boolean {
        return try {
            val modelFile = File(modelsDir, "custom_emotion_model.tflite")
            val deleted = modelFile.delete()
            metadataFile.delete()
            deleted
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveModelFile(modelData: ByteArray): Boolean {
        return try {
            val modelFile = File(modelsDir, "custom_emotion_model.tflite")
            modelFile.writeBytes(modelData)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getModelSize(): Long {
        val modelFile = File(modelsDir, "custom_emotion_model.tflite")
        return if (modelFile.exists()) modelFile.length() else 0L
    }
}
