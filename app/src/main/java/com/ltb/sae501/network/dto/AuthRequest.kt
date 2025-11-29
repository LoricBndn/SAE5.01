package com.ltb.sae501.network.dto

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)
