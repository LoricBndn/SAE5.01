package com.ltb.sae501

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.ltb.sae501.data.models.RecognizedEmotion
import com.ltb.sae501.network.RemoteDataSource
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun EcranCamera(
    detecteurEmotion: EmotionDetector,
    executeurEmotion: java.util.concurrent.ExecutorService,
    dataSource: RemoteDataSource
) {

    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var visages by remember { mutableStateOf(listOf<Face>()) }
    var emotions by remember { mutableStateOf(mapOf<Int, EmotionResult>()) }
    var largeurImage by remember { mutableIntStateOf(0) }
    var hauteurImage by remember { mutableIntStateOf(0) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isFrontCamera by remember { mutableStateOf(true) }

    var enCoursAnalyse by remember { mutableStateOf(false) }

    var frameCounter by remember { mutableIntStateOf(0) }
    val analyseFrequency = 3
    val emotionHistorySize = 5
    val emotionHistory = remember { mutableStateMapOf<Int, MutableList<EmotionResult>>() }

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(isFrontCamera) {
        frameCounter = 0
        emotionHistory.clear()
        emotions = emptyMap()
    }

    LaunchedEffect(isFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imgCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = imgCapture

            val optionsMlKit = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
            val detecteurVisages = FaceDetection.getClient(optionsMlKit)

            val analyseur = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analyseur.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    val bitmap = imageProxy.versBitmap()

                    detecteurVisages.process(inputImage)
                        .addOnSuccessListener { visagesDetectes ->
                            visages = visagesDetectes
                            largeurImage = inputImage.width
                            hauteurImage = inputImage.height

                            frameCounter++
                            val shouldAnalyze = frameCounter % analyseFrequency == 0

                            if (visagesDetectes.isNotEmpty() && bitmap != null && !enCoursAnalyse && shouldAnalyze) {
                                enCoursAnalyse = true
                                val config = bitmap.config ?: Bitmap.Config.ARGB_8888
                                val copieBitmap = bitmap.copy(config, false)

                                executeurEmotion.execute {
                                    try {
                                        visagesDetectes.forEachIndexed { index, visage ->
                                            try {
                                                val bitmapVisage = detecteurEmotion.extraireVisage(copieBitmap, visage)
                                                if (bitmapVisage != null) {
                                                    val emotion = detecteurEmotion.detecterEmotion(bitmapVisage)
                                                    if (emotion != null) {
                                                        println("[Visage $index] Émotion détectée: ${emotion.emotion} - Confiance: ${String.format("%.1f", emotion.confidence * 100)}%")

                                                        if (!emotionHistory.containsKey(index)) {
                                                            emotionHistory[index] = mutableListOf()
                                                        }
                                                        emotionHistory[index]?.add(emotion)

                                                        if (emotionHistory[index]!!.size > emotionHistorySize) {
                                                            emotionHistory[index]!!.removeAt(0)
                                                        }
                                                    }
                                                    bitmapVisage.recycle()
                                                }
                                            } catch (e: Exception) {
                                                println("Erreur analyse visage $index: ${e.message}")
                                            }
                                        }

                                        val emotionsLissees = calculerEmotionsLissees(emotionHistory)

                                        emotionsLissees.forEach { (index, emotion) ->
                                            println("[Visage $index] Lissée: ${emotion.emotion} - ${String.format("%.1f", emotion.confidence * 100)}%")
                                        }

                                        emotions = emotionsLissees

                                        copieBitmap.recycle()
                                    } catch (e: Exception) {
                                        println("Erreur analyse émotions: ${e.message}")
                                    } finally {
                                        enCoursAnalyse = false
                                    }
                                }
                            } else if (visagesDetectes.isEmpty()) {
                                emotions = emptyMap()
                                emotionHistory.clear()
                            }
                            bitmap?.recycle()
                        }
                        .addOnFailureListener { e ->
                            println("Erreur détection visage: ${e.message}")
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            val selecteurCamera = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selecteurCamera,
                    preview,
                    imgCapture,
                    analyseur
                )
            } catch (e: Exception) {
                println("Erreur configuration caméra: ${e.message}")
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay pour afficher les boîtes englobantes et les résultats d'émotion
        FaceOverlay(
            faces = visages,
            emotions = emotions,
            imageWidth = largeurImage,
            imageHeight = hauteurImage,
            isFrontCamera = isFrontCamera
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isFrontCamera = !isFrontCamera
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_rotate),
                        contentDescription = "Changer de caméra",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = {
                        imageCapture?.let { capture ->
                            val photoFile = File(
                                context.externalMediaDirs.firstOrNull(),
                                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                                    .format(System.currentTimeMillis()) + ".jpg"
                            )
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            capture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onError(exc: ImageCaptureException) {
                                        println("Erreur capture photo: ${exc.message}")
                                        exc.printStackTrace()
                                        // TODO: Afficher un Toast d'erreur
                                    }

                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        println("Photo sauvegardée: ${photoFile.absolutePath}")

                                        val uri = output.savedUri ?: Uri.fromFile(photoFile)

                                        val emotionsToSave = emotions.values.map { emotionResult ->
                                            RecognizedEmotion(
                                                emotion = emotionResult.emotion,
                                                confidence = emotionResult.confidence
                                            )
                                        }.toList()

                                        if (emotionsToSave.isNotEmpty()) {
                                            coroutineScope.launch {
                                                val success = dataSource.saveRecognition(
                                                    imageUri = uri,
                                                    recognizedEmotions = emotionsToSave
                                                )
                                                if (success) {
                                                    println("Sauvegardé!")
                                                    // TODO: Toast succès
                                                } else {
                                                    println("Échec sauvegarde")
                                                    // TODO: Toast erreur
                                                }
                                            }
                                        } else {
                                            println("Aucune émotion à sauvegarder")
                                        }
                                    }
                                }
                            )
                        } ?: run {
                            println("imageCapture est null")
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Spacer(modifier = Modifier.size(56.dp))
            }
        }
    }
}

// Lisse les émotions en moyennant les dernières détections
private fun calculerEmotionsLissees(
    history: Map<Int, List<EmotionResult>>
): Map<Int, EmotionResult> {
    val emotionsLissees = mutableMapOf<Int, EmotionResult>()

    history.forEach { (faceIndex, emotionsList) ->
        if (emotionsList.isEmpty()) return@forEach

        val emotionScores = mutableMapOf<String, MutableList<Float>>()

        emotionsList.forEach { emotionResult ->
            if (!emotionScores.containsKey(emotionResult.emotion)) {
                emotionScores[emotionResult.emotion] = mutableListOf()
            }
            emotionScores[emotionResult.emotion]?.add(emotionResult.confidence)
        }

        val moyennesEmotions = emotionScores.map { (emotion, scores) ->
            emotion to scores.average().toFloat()
        }

        val meilleureEmotion = moyennesEmotions.maxByOrNull { it.second }

        if (meilleureEmotion != null) {
            emotionsLissees[faceIndex] = EmotionResult(
                emotion = meilleureEmotion.first,
                confidence = meilleureEmotion.second
            )
        }
    }

    return emotionsLissees
}