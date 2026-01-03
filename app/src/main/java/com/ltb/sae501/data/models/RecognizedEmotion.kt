package com.ltb.sae501.data.models

data class RecognizedEmotion @JvmOverloads constructor(
    var id: String = "",
    var recognitionId: String = "",
    var emotionId: String = "",
    var confidence: Float = 0.0f,
    var detectedAt: Long = System.currentTimeMillis()
)