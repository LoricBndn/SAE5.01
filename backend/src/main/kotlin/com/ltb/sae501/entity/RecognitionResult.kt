package com.ltb.sae501.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "recognition_results")
data class RecognitionResult(
    @Id
    @Column(length = 255)
    var id: String = "",

    @Column(nullable = false)
    var detectedAt: Long = System.currentTimeMillis(),

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    var imageData: ByteArray = byteArrayOf(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @Column(insertable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "recognitionResult", cascade = [CascadeType.ALL], orphanRemoval = true)
    var recognizedEmotions: MutableList<RecognizedEmotion> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecognitionResult

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
