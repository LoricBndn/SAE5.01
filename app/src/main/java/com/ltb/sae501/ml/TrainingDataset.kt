package com.ltb.sae501.ml

import android.graphics.Bitmap

data class TrainingDataset(
    val trainImages: List<LabeledImage>,
    val validationImages: List<LabeledImage>
)

data class LabeledImage(
    val bitmap: Bitmap,
    val label: String,
    val labelIndex: Int
)

data class TrainingResult(
    val success: Boolean,
    val finalAccuracy: Float,
    val trainingTime: Long,
    val errorMessage: String?
)

data class EpochMetrics(
    val epoch: Int,
    val trainLoss: Float,
    val trainAccuracy: Float,
    val valLoss: Float,
    val valAccuracy: Float
)

sealed class TrainingError {
    data class InsufficientData(val missingCategories: List<String>) : TrainingError()
    data class NetworkError(val message: String) : TrainingError()
    data class ServerError(val code: Int, val message: String) : TrainingError()
    data class PreprocessingError(val message: String) : TrainingError()
    object Timeout : TrainingError()
}

sealed class ValidationResult {
    data class Ready(val totalImages: Int) : ValidationResult()
    data class Insufficient(val missingCategories: List<String>) : ValidationResult()
}

data class CombinedEmotionResult(
    val emotion: String,
    val confidence: Float,
    val source: PredictionSource,
    val baseProbabilities: FloatArray,
    val customProbabilities: FloatArray?,
    val combinedProbabilities: FloatArray,
    val weights: Pair<Float, Float>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CombinedEmotionResult

        if (emotion != other.emotion) return false
        if (confidence != other.confidence) return false
        if (source != other.source) return false
        if (!baseProbabilities.contentEquals(other.baseProbabilities)) return false
        if (customProbabilities != null) {
            if (other.customProbabilities == null) return false
            if (!customProbabilities.contentEquals(other.customProbabilities)) return false
        } else if (other.customProbabilities != null) return false
        if (!combinedProbabilities.contentEquals(other.combinedProbabilities)) return false
        if (weights != other.weights) return false

        return true
    }

    override fun hashCode(): Int {
        var result = emotion.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + baseProbabilities.contentHashCode()
        result = 31 * result + (customProbabilities?.contentHashCode() ?: 0)
        result = 31 * result + combinedProbabilities.contentHashCode()
        result = 31 * result + (weights?.hashCode() ?: 0)
        return result
    }
}

enum class PredictionSource {
    BASE,
    CUSTOM,
    HYBRID
}

data class ModelMetadata(
    val version: Int,
    val userId: String,
    val trainingDate: Long,
    val accuracy: Float,
    val trainingImageCount: Int,
    val epochsTrained: Int,
    val categories: Map<String, Int>
)
