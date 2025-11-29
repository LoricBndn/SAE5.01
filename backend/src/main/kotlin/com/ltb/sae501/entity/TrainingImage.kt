package com.ltb.sae501.entity

import jakarta.persistence.*

@Entity
@Table(name = "training_images")
data class TrainingImage(
    @Id
    @Column(length = 255)
    var id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: EmotionCategory? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    var imageData: ByteArray = byteArrayOf(),

    @Column(length = 255)
    var fileName: String? = null,

    @Column(nullable = false)
    var uploadedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrainingImage

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
