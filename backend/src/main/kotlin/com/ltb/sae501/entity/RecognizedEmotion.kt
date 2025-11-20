package com.ltb.sae501.entity

import jakarta.persistence.*

@Entity
@Table(name = "recognized_emotions")
data class RecognizedEmotion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recognition_id", nullable = false)
    var recognitionResult: RecognitionResult? = null,

    @Column(nullable = false, length = 50)
    var emotion: String = "",

    @Column(nullable = false)
    var confidence: Float = 0.0f
)
