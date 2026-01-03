package com.ltb.sae501.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id
    @Column(length = 255)
    var id: String = "",

    @Column(unique = true, nullable = false, length = 100)
    var username: String = "",

    @Column(nullable = false, length = 255)
    var password: String = "",

    @Column(length = 255)
    var email: String? = null,

    @Column(nullable = false)
    var createdAt: Long = System.currentTimeMillis(),

    @Column(nullable = false)
    var lastLogin: Long? = System.currentTimeMillis(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = false)
    var recognitions: MutableList<RecognitionResult> = mutableListOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = false)
    var customModels: MutableList<CustomModel> = mutableListOf()
)
