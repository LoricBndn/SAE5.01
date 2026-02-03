package com.ltb.sae501.network.dto

import com.google.gson.annotations.SerializedName

data class FeedItemResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("detectedAt")
    val detectedAt: Long,

    @SerializedName("imageUrl")
    val imageUrl: String,

    @SerializedName("recognizedEmotions")
    val recognizedEmotions: List<RecognizedEmotionDto>,

    @SerializedName("displayName")
    val displayName: String?,

    @SerializedName("isOwnPost")
    val isOwnPost: Boolean
)
