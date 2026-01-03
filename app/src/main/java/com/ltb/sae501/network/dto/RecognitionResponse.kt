package com.ltb.sae501.network.dto

import com.google.gson.annotations.SerializedName

data class RecognitionResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("userId")
    val userId: String?,

    @SerializedName("imageStorageUrl")
    val imageStorageUrl: String,

    @SerializedName("detectedAt")
    val detectedAt: Long,

    @SerializedName("recognizedEmotions")
    val recognizedEmotions: List<RecognizedEmotionDto>,

)

data class RecognizedEmotionDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("recognitionId")
    val recognitionId: String,
    
    @SerializedName("emotionId")
    val emotionId: String,

    @SerializedName("confidence")
    val confidence: Float,

    @SerializedName("detectedAt")
    val detectedAt: Long,
)
