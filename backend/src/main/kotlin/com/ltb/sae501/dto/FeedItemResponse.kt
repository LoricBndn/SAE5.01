package com.ltb.sae501.dto

data class FeedItemResponse(
    val id: String,
    val detectedAt: Long,
    val imageUrl: String,
    val recognizedEmotions: List<RecognizedEmotionDto>,
    val displayName: String?,
    val isOwnPost: Boolean
)
