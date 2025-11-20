package com.ltb.sae501.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id
    var id: String = "",

    @Column(unique = true, nullable = false, length = 100)
    var username: String = "",

    @Column(nullable = false)
    var password: String = "",

    @Column(length = 255)
    var email: String? = null,

    @Column(nullable = false)
    var createdAt: Long = System.currentTimeMillis(),

    @Column
    var lastLogin: Long? = null,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = false)
    var recognitions: MutableList<RecognitionResult> = mutableListOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var trainingImages: MutableList<TrainingImage> = mutableListOf()
)
