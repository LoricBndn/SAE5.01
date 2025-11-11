package com.ltb.sae501.data.models

/**
 * Représente une catégorie d'émotion
 */
data class EmotionCategory @JvmOverloads constructor(
    var id: String = "",
    var name: String = "",
    var nameEn: String = "",
    var emoji: String = "",
    var color: String = "",
    var description: String = "",
    var imageCount: Int = 0,
    var trainingImages: List<String> = emptyList(),
    var createdAt: Long = System.currentTimeMillis(),
    var lastUpdated: Long = System.currentTimeMillis()
)