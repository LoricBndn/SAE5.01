package com.ltb.sae501.dto

data class TrainingImagesResponse(
    val categories: List<CategoryImagesDto>
)

data class CategoryImagesDto(
    val categoryId: String,
    val categoryName: String,
    val images: List<ImageDataDto>
)

data class ImageDataDto(
    val id: String,
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
