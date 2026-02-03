package com.ltb.sae501.data.models

data class RecognitionResult @JvmOverloads constructor(
    var id: String = "",
    var userId: String? = null,
    var imageData: ByteArray? = null,
    var imageLocalPath: String? = null,
    var imageUrl: String? = null,
    var detectedAt: Long = System.currentTimeMillis(),
    var recognizedEmotions: List<RecognizedEmotion> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RecognitionResult
        if (id != other.id) return false
        if (userId != other.userId) return false
        if (imageData != null) {
            if (other.imageData == null) return false
            if (!imageData.contentEquals(other.imageData)) return false
        } else if (other.imageData != null) return false
        if (imageLocalPath != other.imageLocalPath) return false
        if (imageUrl != other.imageUrl) return false
        if (detectedAt != other.detectedAt) return false
        if (recognizedEmotions != other.recognizedEmotions) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (userId?.hashCode() ?: 0)
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        result = 31 * result + (imageLocalPath?.hashCode() ?: 0)
        result = 31 * result + (imageUrl?.hashCode() ?: 0)
        result = 31 * result + detectedAt.hashCode()
        result = 31 * result + recognizedEmotions.hashCode()
        return result
    }
}