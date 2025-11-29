package com.ltb.sae501.network.dto

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("token")
    val token: String?,

    @SerializedName("userId")
    val userId: String?,

    @SerializedName("message")
    val message: String
)
