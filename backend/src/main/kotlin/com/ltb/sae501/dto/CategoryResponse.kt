package com.ltb.sae501.dto

data class CategoryResponse(
    val id: String,
    val name: String,
    val nameEn: String,
    val emoji: String,
    val color: String,
    val description: String,
    val imageCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val trainingImages: List<String>
)
