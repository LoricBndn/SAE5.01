package com.ltb.sae501.ui.screens

import androidx.compose.runtime.Composable
import com.ltb.sae501.EcranCamera
import com.ltb.sae501.EmotionDetector
import com.ltb.sae501.firebase.FirebaseDataSource

@Composable
fun CameraScreen(
    detecteurEmotion: EmotionDetector,
    executeurEmotion: java.util.concurrent.ExecutorService,
    dataSource: FirebaseDataSource
) {
    EcranCamera(
        detecteurEmotion = detecteurEmotion,
        executeurEmotion = executeurEmotion,
        dataSource = dataSource
    )
}