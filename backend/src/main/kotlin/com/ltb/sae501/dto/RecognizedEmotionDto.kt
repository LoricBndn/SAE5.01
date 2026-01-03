package com.ltb.sae501.dto

data class RecognizedEmotionDto(
    val id: String? = null,
    val recognitionId: String? = null,
    val emotionId: String,
    val confidence: Float,
    val detectedAt: Long? = null
)
