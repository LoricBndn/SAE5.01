package com.ltb.sae501

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.ltb.sae501.data.models.RecognizedEmotion
import com.ltb.sae501.ml.CombinedEmotionResult
import com.ltb.sae501.ml.DualModelDetector
import com.ltb.sae501.network.RemoteDataSource
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun EcranCamera(
    detecteurEmotion: DualModelDetector,
    executeurEmotion: java.util.concurrent.ExecutorService,
    dataSource: RemoteDataSource,
    repository: com.ltb.sae501.data.RecognitionRepository,
    isFrontCamera: Boolean,
    isLoggedIn: Boolean,
    onCameraFlipped: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var visages by remember { mutableStateOf(listOf<Face>()) }
    var emotions by remember { mutableStateOf(mapOf<Int, CombinedEmotionResult>()) }
    var categories by remember { mutableStateOf<List<com.ltb.sae501.data.models.EmotionCategory>>(emptyList()) }
    var largeurImage by remember { mutableIntStateOf(0) }
    var hauteurImage by remember { mutableIntStateOf(0) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var enCoursAnalyse by remember { mutableStateOf(false) }

    val showPercentage by com.ltb.sae501.preferences.AppPreferences.isPercentageShownFlow.collectAsState()
    val autoSaveEnabled by com.ltb.sae501.preferences.AppPreferences.isAutoSaveEnabledFlow.collectAsState()

    var frameCounter by remember { mutableIntStateOf(0) }
    val analyseFrequency = 3
    val emotionHistorySize = 5
    val emotionHistory = remember { mutableStateMapOf<Int, MutableList<CombinedEmotionResult>>() }
    
    var showResultDialog by remember { mutableStateOf(false) }
    var analyzedImageUri by remember { mutableStateOf<Uri?>(null) }
    var analyzedEmotions by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isAnalyzing = true
            analyzeImageFromGallery(
                context = context,
                uri = it,
                detecteurEmotion = detecteurEmotion,
                executeurEmotion = executeurEmotion,
                onAnalysisComplete = { emotions ->
                    isAnalyzing = false
                    if (emotions.isNotEmpty()) {
                        analyzedImageUri = it
                        analyzedEmotions = emotions
                        showResultDialog = true
                    }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        dataSource.getCategories().collect { cats ->
            categories = cats
            Log.d("EcranCamera", "Catégories chargées: ${cats.size} catégories")
        }
    }

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
                                                Log.e("EcranCamera", "Erreur analyse visage $index: ${e.message}", e)
                                            }
                                        }

                                        val emotionsLissees = calculerEmotionsLissees(emotionHistory)
                                        emotions = emotionsLissees
                                        copieBitmap.recycle()
                                    } catch (e: Exception) {
                                        Log.e("EcranCamera", "Erreur analyse émotions: ${e.message}", e)
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
                            Log.e("EcranCamera", "Erreur détection visage: ${e.message}", e)
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
                Log.e("EcranCamera", "Erreur configuration caméra: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        FaceOverlay(
            faces = visages,
            emotions = emotions,
            imageWidth = largeurImage,
            imageHeight = hauteurImage,
            isFrontCamera = isFrontCamera,
            showPercentage = showPercentage
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
                // Bouton Galerie
                IconButton(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_gallery),
                        contentDescription = "Galerie",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Bouton Capture
                IconButton(
                    onClick = {
                        imageCapture?.let { capture ->
                            val photoFile = File(
                                context.filesDir,
                                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                                    .format(System.currentTimeMillis()) + ".jpg"
                            )
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            capture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onError(exc: ImageCaptureException) {
                                        Log.e("EcranCamera", "Erreur capture photo: ${exc.message}", exc)
                                    }

                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        val uri = output.savedUri ?: Uri.fromFile(photoFile)

                                        // Préparer les émotions détectées en temps réel
                                        val emotionsToShow = emotions.values.map { emotionResult ->
                                            emotionResult.emotion to emotionResult.confidence
                                        }.sortedByDescending { it.second }

                                        if (emotionsToShow.isNotEmpty()) {
                                            analyzedImageUri = uri
                                            analyzedEmotions = emotionsToShow
                                            showResultDialog = true
                                        }
                                    }
                                }
                            )
                        } ?: run {
                            Log.e("EcranCamera", "imageCapture est null")
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

                // Bouton Retournement caméra
                IconButton(
                    onClick = {
                        onCameraFlipped(!isFrontCamera)
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
            }
        }

        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Analyse en cours...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showResultDialog && analyzedImageUri != null) {
        EmotionResultDialog(
            imageUri = analyzedImageUri!!,
            emotions = analyzedEmotions,
            categories = categories,
            autoSaveEnabled = autoSaveEnabled,
            isLoggedIn = isLoggedIn,
            onDismiss = {
                showResultDialog = false
                analyzedImageUri = null
                analyzedEmotions = emptyList()
            },
            onSave = { isPublic, showUsername ->
                Log.d("EcranCamera", "=== onSave CALLED ===")
                Log.d("EcranCamera", "isPublic=$isPublic, showUsername=$showUsername")
                Log.d("EcranCamera", "analyzedEmotions count: ${analyzedEmotions.size}")
                analyzedEmotions.forEach { (id, conf) -> Log.d("EcranCamera", "  - emotion: '$id', confidence: $conf") }
                coroutineScope.launch {
                    // Utiliser directement l'emotionId du modèle ML (correspond aux IDs en BDD)
                    val emotionsToSave = analyzedEmotions.map { (emotionId, confidence) ->
                        RecognizedEmotion(
                            id = java.util.UUID.randomUUID().toString(),
                            recognitionId = "",
                            emotionId = emotionId,
                            confidence = confidence,
                            detectedAt = System.currentTimeMillis()
                        )
                    }

                    Log.d("EcranCamera", "emotionsToSave: ${emotionsToSave.size}")
                    if (emotionsToSave.isNotEmpty()) {
                        Log.d("EcranCamera", "Calling repository.saveRecognition...")
                        val result = repository.saveRecognition(
                            imageUri = analyzedImageUri!!,
                            emotions = emotionsToSave,
                            autoSaveEnabled = true,
                            isPublic = isPublic,
                            showUsername = showUsername
                        )
                        Log.d("EcranCamera", "saveRecognition result: $result")
                    } else {
                        Log.w("EcranCamera", "No emotions to save!")
                    }

                    showResultDialog = false
                    analyzedImageUri = null
                    analyzedEmotions = emptyList()
                }
            }
        )
    }
}

@Composable
fun EmotionResultDialog(
    imageUri: Uri,
    emotions: List<Pair<String, Float>>,
    categories: List<com.ltb.sae501.data.models.EmotionCategory>,
    autoSaveEnabled: Boolean,
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    onSave: (isPublic: Boolean, showUsername: Boolean) -> Unit
) {
    var isPublic by remember { mutableStateOf(false) }
    var showUsername by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bouton fermer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                AsyncImage(
                    model = imageUri,
                    contentDescription = "Image analysée",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Résultats de l'analyse",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Liste des émotions détectées
                emotions.take(3).forEachIndexed { index, (emotionId, confidence) ->
                    val category = categories.find { it.id.equals(emotionId, ignoreCase = true) }
                    val emotionName = category?.name ?: emotionId
                    val emoji = category?.emoji ?: "😐"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                emoji,
                                fontSize = 32.sp
                            )
                            Text(
                                emotionName,
                                fontSize = 18.sp,
                                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            "${(confidence * 100).toInt()}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (index) {
                                0 -> MaterialTheme.colorScheme.primary
                                1 -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.tertiary
                            }
                        )
                    }

                    LinearProgressIndicator(
                        progress = { confidence },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when (index) {
                            0 -> MaterialTheme.colorScheme.primary
                            1 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                }

                // Options de partage (seulement si connecté)
                if (isLoggedIn) {
                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        "Options de partage",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Partager dans le fil public",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isPublic,
                            onCheckedChange = { isPublic = it }
                        )
                    }

                    if (isPublic) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Afficher mon pseudo",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = showUsername,
                                onCheckedChange = { showUsername = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Boutons d'action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!autoSaveEnabled) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ignorer", fontSize = 16.sp)
                        }
                    }

                    Button(
                        onClick = { onSave(isPublic, showUsername) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (autoSaveEnabled) "Continuer" else "Sauvegarder",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

fun analyzeImageFromGallery(
    context: android.content.Context,
    uri: Uri,
    detecteurEmotion: DualModelDetector,
    executeurEmotion: java.util.concurrent.ExecutorService,
    onAnalysisComplete: (List<Pair<String, Float>>) -> Unit
) {
    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (bitmap == null) {
            mainHandler.post { onAnalysisComplete(emptyList()) }
            return
        }

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val optionsMlKit = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        val detecteurVisages = FaceDetection.getClient(optionsMlKit)

        detecteurVisages.process(inputImage)
            .addOnSuccessListener { visages ->
                Log.d("GalleryAnalysis", "Visages détectés: ${visages.size}")
                if (visages.isNotEmpty()) {
                    val emotions = mutableListOf<Pair<String, Float>>()

                    executeurEmotion.execute {
                        try {
                            visages.forEach { visage ->
                                val bitmapVisage = detecteurEmotion.extraireVisage(bitmap, visage)
                                if (bitmapVisage != null) {
                                    val emotion = detecteurEmotion.detecterEmotion(bitmapVisage)
                                    if (emotion != null) {
                                        emotions.add(emotion.emotion to emotion.confidence)
                                        Log.d("GalleryAnalysis", "Émotion détectée: ${emotion.emotion} - ${emotion.confidence}")
                                    }
                                    bitmapVisage.recycle()
                                }
                            }

                            bitmap.recycle()

                            // Trier par confiance décroissante
                            val sortedEmotions = emotions.sortedByDescending { it.second }
                            Log.d("GalleryAnalysis", "Total émotions: ${sortedEmotions.size}")

                            mainHandler.post { onAnalysisComplete(sortedEmotions) }
                        } catch (e: Exception) {
                            Log.e("GalleryAnalysis", "Erreur dans l'exécuteur: ${e.message}", e)
                            bitmap.recycle()
                            mainHandler.post { onAnalysisComplete(emptyList()) }
                        }
                    }
                } else {
                    Log.d("GalleryAnalysis", "Aucun visage détecté")
                    bitmap.recycle()
                    mainHandler.post { onAnalysisComplete(emptyList()) }
                }
            }
            .addOnFailureListener { e ->
                Log.e("GalleryAnalysis", "Erreur détection visage: ${e.message}", e)
                bitmap.recycle()
                mainHandler.post { onAnalysisComplete(emptyList()) }
            }
    } catch (e: Exception) {
        Log.e("GalleryAnalysis", "Erreur analyse image: ${e.message}", e)
        mainHandler.post { onAnalysisComplete(emptyList()) }
    }
}

private fun calculerEmotionsLissees(
    history: Map<Int, List<CombinedEmotionResult>>
): Map<Int, CombinedEmotionResult> {
    val emotionsLissees = mutableMapOf<Int, CombinedEmotionResult>()

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

        if (meilleureEmotion != null && emotionsList.isNotEmpty()) {
            val dernierResultat = emotionsList.last()
            emotionsLissees[faceIndex] = CombinedEmotionResult(
                emotion = meilleureEmotion.first,
                confidence = meilleureEmotion.second,
                source = dernierResultat.source,
                baseProbabilities = dernierResultat.baseProbabilities,
                customProbabilities = dernierResultat.customProbabilities,
                combinedProbabilities = dernierResultat.combinedProbabilities,
                weights = dernierResultat.weights
            )
        }
    }

    return emotionsLissees
}