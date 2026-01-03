package com.ltb.sae501.entity

import jakarta.persistence.*

@Entity
@Table(name = "recognized_emotions")
data class RecognizedEmotion(
    @Id
    @Column(length = 255)
    var id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recognition_id", nullable = false)
    var recognitionResult: RecognitionResult = RecognitionResult(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_id", nullable = false)
    var emotionCategory: EmotionCategory = EmotionCategory(),

    @Column(nullable = false)
    var confidence: Float = 0.0f,

    @Column(nullable = false)
    var detectedAt: Long = System.currentTimeMillis()
)
