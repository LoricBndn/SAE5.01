package com.ltb.sae501.data.models

data class RecognitionResult @JvmOverloads constructor(
    var id: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var imageStorageUrl: String = "",
    var recognizedEmotions: List<RecognizedEmotion> = emptyList(),
    var userId: String? = null
)