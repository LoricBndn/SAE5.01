package com.ltb.sae501.entity

import jakarta.persistence.*

@Entity
@Table(name = "emotion_categories")
data class EmotionCategory(
    @Id
    @Column(length = 50)
    var id: String = "",

    @Column(unique = true, nullable = false, length = 100)
    var name: String = "",

    @Column(unique = true, nullable = false, length = 100)
    var nameEn: String = "",

    @Column(length = 10)
    var emoji: String = "",

    @Column(length = 7)
    var color: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    @Column
    var imageCount: Int = 0,

    @Column(nullable = false)
    var createdAt: Long = System.currentTimeMillis(),

    @Column(nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),

    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], orphanRemoval = true)
    var trainingImages: MutableList<TrainingImage> = mutableListOf()
)
