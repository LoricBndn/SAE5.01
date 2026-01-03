package com.ltb.sae501.dto

data class RecognitionResponse(
    val id: String,
    val detectedAt: Long,
    val imageUrl: String,
    val recognizedEmotions: List<RecognizedEmotionDto>,
    val userId: String?
)
