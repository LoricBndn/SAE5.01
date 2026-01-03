package com.ltb.sae501.network.dto

import com.google.gson.annotations.SerializedName

data class CategoryResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("nameEn")
    val nameEn: String,

    @SerializedName("emoji")
    val emoji: String,

    @SerializedName("color")
    val color: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("imageCount")
    val imageCount: Int,

    @SerializedName("createdAt")
    val createdAt: Long,

    @SerializedName("updatedAt")
    val updatedAt: Long,

    @SerializedName("trainingImages")
    val trainingImages: List<String>,
)
