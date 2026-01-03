package com.ltb.sae501.data.models

data class EmotionCategory @JvmOverloads constructor(
    var id: String = "",
    var name: String = "",
    var nameEn: String = "",
    var emoji: String = "",
    var color: String = "",
    var description: String = "",
    var imageCount: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var trainingImages: List<String> = emptyList()
)