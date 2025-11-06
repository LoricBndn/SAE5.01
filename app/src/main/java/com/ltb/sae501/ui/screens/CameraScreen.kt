package com.ltb.sae501.ui.screens

import androidx.compose.runtime.Composable
import com.ltb.sae501.EcranCamera
import com.ltb.sae501.EmotionDetector

@Composable
fun CameraScreen(
    detecteurEmotion: EmotionDetector,
    executeurEmotion: java.util.concurrent.ExecutorService
) {
    EcranCamera(
        detecteurEmotion = detecteurEmotion,
        executeurEmotion = executeurEmotion
    )
}