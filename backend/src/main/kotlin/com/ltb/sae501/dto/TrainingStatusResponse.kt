package com.ltb.sae501.dto

data class TrainingStatusResponse(
    val status: String,
    val progress: Float,
    val currentEpoch: Int,
    val totalEpochs: Int,
    val accuracy: Float,
    val errorMessage: String?
)
