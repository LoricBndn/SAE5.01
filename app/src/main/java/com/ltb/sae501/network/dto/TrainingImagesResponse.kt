package com.ltb.sae501.network.dto

import com.google.gson.annotations.SerializedName

data class TrainingImagesResponse(
    @SerializedName("categories")
    val categories: List<CategoryImagesDto>
)

data class CategoryImagesDto(
    @SerializedName("categoryId")
    val categoryId: String,

    @SerializedName("categoryName")
    val categoryName: String,

    @SerializedName("images")
    val images: List<ImageDataDto>
)

data class ImageDataDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("data")
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageDataDto

        if (id != other.id) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
