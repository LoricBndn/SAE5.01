package com.ltb.sae501.entity

import jakarta.persistence.*

@Entity
@Table(name = "custom_models")
data class CustomModel(
    @Id
    @Column(length = 255)
    var id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    var modelData: ByteArray = byteArrayOf(),

    @Column(nullable = false)
    var version: Int = 1,

    @Column(nullable = false)
    var accuracy: Float = 0.0f,

    @Column(nullable = false)
    var trainingImageCount: Int = 0,

    @Column(nullable = false)
    var createdAt: Long = System.currentTimeMillis(),

    @Column(length = 1000)
    var metadataJson: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomModel

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
