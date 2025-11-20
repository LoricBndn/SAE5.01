package com.ltb.sae501.dto

data class RecognitionRequest(
    val emotions: List<RecognizedEmotionDto>,
    val userId: String? = null
)
