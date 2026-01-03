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
    var user: User = User(),

    @Column(nullable = false)
    var accuracy: Float = 0.0f,

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    var metadata: String = "",

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    var modelData: ByteArray = byteArrayOf(),

    @Column(nullable = false, length = 50)
    var version: String = "",

    @Column(nullable = false)
    var createdAt: Long = System.currentTimeMillis(),

    @Column(nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),

    @OneToMany(mappedBy = "customModel", cascade = [CascadeType.ALL], orphanRemoval = true)
    var trainingImages: MutableList<TrainingImage> = mutableListOf()
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
