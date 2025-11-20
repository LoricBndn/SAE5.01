package com.ltb.sae501.dto

data class TrainingImageResponse(
    val id: String,
    val categoryId: String,
    val imageUrl: String,
    val fileName: String?,
    val uploadedAt: Long
)
