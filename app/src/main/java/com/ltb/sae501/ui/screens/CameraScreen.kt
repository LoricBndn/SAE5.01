package com.ltb.sae501.ui.screens

import androidx.compose.runtime.Composable
import com.ltb.sae501.EcranCamera
import com.ltb.sae501.ml.DualModelDetector
import com.ltb.sae501.network.RemoteDataSource

@Composable
fun CameraScreen(
    detecteurEmotion: DualModelDetector,
    executeurEmotion: java.util.concurrent.ExecutorService,
    dataSource: RemoteDataSource,
    repository: com.ltb.sae501.data.RecognitionRepository,
    isFrontCamera: Boolean,
    isLoggedIn: Boolean,
    onCameraFlipped: (Boolean) -> Unit
) {
    EcranCamera(
        detecteurEmotion = detecteurEmotion,
        executeurEmotion = executeurEmotion,
        dataSource = dataSource,
        repository = repository,
        isFrontCamera = isFrontCamera,
        isLoggedIn = isLoggedIn,
        onCameraFlipped = onCameraFlipped
    )
}