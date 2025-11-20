package com.ltb.sae501.dto

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null
)
