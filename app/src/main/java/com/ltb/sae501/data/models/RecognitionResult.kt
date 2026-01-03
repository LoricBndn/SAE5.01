package com.ltb.sae501.data.models

data class RecognitionResult @JvmOverloads constructor(
    var id: String = "",
    var userId: String? = null,
    var imageStorageUrl: String = "",
    var detectedAt: Long = System.currentTimeMillis(),
    var recognizedEmotions: List<RecognizedEmotion> = emptyList()
)