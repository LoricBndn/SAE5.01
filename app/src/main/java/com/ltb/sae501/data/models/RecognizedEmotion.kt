package com.ltb.sae501.data.models

data class RecognizedEmotion @JvmOverloads constructor(
    var emotion: String = "",
    var confidence: Float = 0.0f
)