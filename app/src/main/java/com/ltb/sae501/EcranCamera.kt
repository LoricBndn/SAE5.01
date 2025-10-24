package com.ltb.sae501

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

@Composable
fun EcranCamera(
    detecteurEmotion: EmotionDetector,
    executeurEmotion: java.util.concurrent.ExecutorService
) {
    // Variables d'état (automatiquement mises à jour dans l'UI)
    var visages by remember { mutableStateOf(listOf<Face>()) }
    var emotions by remember { mutableStateOf(mapOf<Int, EmotionResult>()) }
    var largeurImage by remember { mutableStateOf(0) }
    var hauteurImage by remember { mutableStateOf(0) }

    // Flag pour éviter de lancer plusieurs analyses en même temps
    var enCoursAnalyse by remember { mutableStateOf(false) }

    AndroidView(
        factory = { context ->
            val vuePreview = PreviewView(context)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // prévisualisation
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(vuePreview.surfaceProvider)
                }

                // ML Kit pour détecter les visages
                val optionsMlKit = FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build()
                val detecteurVisages = FaceDetection.getClient(optionsMlKit)

                // analyse d'image
                val analyseur = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analyseur.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        // image pour ML Kit
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        // Convertir en Bitmap AVANT de fermer l'image
                        val bitmap = imageProxy.versBitmap()

                        // Détecter les visages
                        detecteurVisages.process(inputImage)
                            .addOnSuccessListener { visagesDetectes ->
                                // MAJ liste des visages
                                visages = visagesDetectes
                                largeurImage = inputImage.width
                                hauteurImage = inputImage.height

                                // Si visages et pas d'analayse
                                if (visagesDetectes.isNotEmpty() && bitmap != null && !enCoursAnalyse) {
                                    enCoursAnalyse = true

                                    // Copier le bitmap pour l'utiliser dans un autre thread
                                    val config = bitmap.config ?: Bitmap.Config.ARGB_8888
                                    val copieBitmap = bitmap.copy(config, false)

                                    // Analyser les émotions
                                    executeurEmotion.execute {
                                        try {
                                            val nouvellesEmotions = mutableMapOf<Int, EmotionResult>()

                                            // Pour chaque visage détecté
                                            visagesDetectes.forEachIndexed { index, visage ->
                                                try {
                                                    // Extraire visage de l'image
                                                    val bitmapVisage = detecteurEmotion.extraireVisage(copieBitmap, visage)

                                                    if (bitmapVisage != null) {
                                                        // Détecte l'émotion
                                                        val emotion = detecteurEmotion.detecterEmotion(bitmapVisage)

                                                        if (emotion != null) {
                                                            nouvellesEmotions[index] = emotion
                                                        }

                                                        // Libére la mémoire
                                                        bitmapVisage.recycle()
                                                    }
                                                } catch (e: Exception) {
                                                    println("Erreur analyse visage $index: ${e.message}")
                                                }
                                            }

                                            // MAJ les émotions affichées
                                            emotions = nouvellesEmotions
                                            copieBitmap.recycle()

                                        } catch (e: Exception) {
                                            println("Erreur analyse émotions: ${e.message}")
                                        } finally {
                                            enCoursAnalyse = false
                                        }
                                    }
                                } else if (visagesDetectes.isEmpty()) {
                                    // Pas de visages = pas d'émotions
                                    emotions = emptyMap()
                                }

                                // Nettoyer le bitmap principal
                                bitmap?.recycle()
                            }
                            .addOnFailureListener { e ->
                                println("Erreur détection visage: ${e.message}")
                            }
                            .addOnCompleteListener {
                                // Libérer l'image pour la prochaine frame
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                // Utiliser la caméra frontale
                val selecteurCamera = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    // Débinder tout avant de rebinder
                    cameraProvider.unbindAll()

                    // Lier la caméra au cycle de vie
                    cameraProvider.bindToLifecycle(
                        context as androidx.lifecycle.LifecycleOwner,
                        selecteurCamera,
                        preview,
                        analyseur
                    )
                } catch (e: Exception) {
                    println("Erreur configuration caméra: ${e.message}")
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))

            vuePreview
        },
        modifier = Modifier.fillMaxSize()
    )

    // Afficher rectangles et émotions par-dessus la caméra
    FaceOverlay(
        faces = visages,
        emotions = emotions,
        imageWidth = largeurImage,
        imageHeight = hauteurImage
    )
}