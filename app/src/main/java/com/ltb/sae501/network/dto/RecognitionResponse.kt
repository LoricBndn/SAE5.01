package com.ltb.sae501.network.dto

import com.google.gson.annotations.SerializedName

data class RecognitionResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("imageUrl")
    val imageUrl: String,

    @SerializedName("recognizedEmotions")
    val recognizedEmotions: List<RecognizedEmotionDto>,

    @SerializedName("userId")
    val userId: String?
)

data class RecognizedEmotionDto(
    @SerializedName("emotion")
    val emotion: String,

    @SerializedName("confidence")
    val confidence: Float
)
