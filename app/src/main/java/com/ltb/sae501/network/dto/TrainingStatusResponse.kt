package com.ltb.sae501.network.dto

import com.google.gson.annotations.SerializedName

data class TrainingStatusResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("progress")
    val progress: Float,

    @SerializedName("currentEpoch")
    val currentEpoch: Int,

    @SerializedName("totalEpochs")
    val totalEpochs: Int,

    @SerializedName("accuracy")
    val accuracy: Float,

    @SerializedName("errorMessage")
    val errorMessage: String?
)
