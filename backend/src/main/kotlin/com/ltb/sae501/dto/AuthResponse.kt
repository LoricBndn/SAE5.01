package com.ltb.sae501.dto

data class AuthResponse(
    val token: String?,
    val userId: String?,
    val message: String
)
