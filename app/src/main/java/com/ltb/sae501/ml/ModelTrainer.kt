package com.ltb.sae501.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.ltb.sae501.network.RemoteDataSource
import com.ltb.sae501.network.dto.TrainingImagesResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.random.Random

class ModelTrainer(
    private val context: Context,
    private val dataSource: RemoteDataSource
) {

    private val metadataManager = ModelMetadataManager(context)
    private val minImagesPerCategory = 10

    companion object {
        private const val TAG = "ModelTrainer"
        private const val ENABLE_AUGMENTATION = true
    }

    // check si assez d'images
    suspend fun validateTrainingReadiness(): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val imagesResponse = dataSource.downloadAllTrainingImages()

            if (imagesResponse == null) {
                return@withContext ValidationResult.Insufficient(
                    listOf("Impossible de télécharger les images")
                )
            }

            val insufficientCategories = imagesResponse.categories
                .filter { it.images.size < minImagesPerCategory }
                .map { "${it.categoryName}: ${it.images.size}/$minImagesPerCategory" }

            if (insufficientCategories.isNotEmpty()) {
                ValidationResult.Insufficient(insufficientCategories)
            } else {
                val totalImages = imagesResponse.categories.sumOf { it.images.size }
                ValidationResult.Ready(totalImages)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur validation: ${e.message}", e)
            ValidationResult.Insufficient(listOf("Erreur: ${e.message}"))
        }
    }

    private suspend fun downloadTrainingImages(): TrainingImagesResponse? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Téléchargement des images d'entraînement...")
            dataSource.downloadAllTrainingImages()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur téléchargement images: ${e.message}", e)
            null
        }
    }

    // détection visage + resize + augmentation
    private suspend fun preprocessImages(
        imagesResponse: TrainingImagesResponse,
        progressCallback: (Int, String) -> Unit
    ): TrainingDataset = withContext(Dispatchers.Default) {
        progressCallback(20, "Prétraitement des images...")

        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
        val faceDetector = FaceDetection.getClient(faceDetectorOptions)

        val processedImages = mutableListOf<LabeledImage>()
        var processedCount = 0
        val totalCount = imagesResponse.categories.sumOf { it.images.size }

        val labelMap = mapOf(
            "Angry" to 0, "Disgust" to 1, "Fear" to 2, "Happy" to 3,
            "Sad" to 4, "Surprise" to 5, "Neutral" to 6
        )

        for (category in imagesResponse.categories) {
            val labelIndex = labelMap[category.categoryName] ?: 0
            val label = category.categoryName.lowercase()

            for (imageDto in category.images) {
                try {
                    val bitmap = BitmapFactory.decodeByteArray(
                        imageDto.data,
                        0,
                        imageDto.data.size
                    )

                    if (bitmap != null) {
                        // Détection de visage
                        val inputImage = InputImage.fromBitmap(bitmap, 0)
                        val faces = faceDetector.process(inputImage).await()

                        if (faces.isNotEmpty()) {
                            // Prendre le premier visage détecté
                            val face = faces[0]
                            val bbox = face.boundingBox

                            // Extraire le visage avec marge
                            val margeX = (bbox.width() * 0.2f).toInt()
                            val margeY = (bbox.height() * 0.2f).toInt()

                            val left = maxOf(0, bbox.left - margeX)
                            val top = maxOf(0, bbox.top - margeY)
                            val right = minOf(bitmap.width, bbox.right + margeX)
                            val bottom = minOf(bitmap.height, bbox.bottom + margeY)

                            val width = right - left
                            val height = bottom - top

                            if (width > 0 && height > 0) {
                                val faceBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)

                                // Redimensionner à 48x48
                                val resized = Bitmap.createScaledBitmap(faceBitmap, 48, 48, true)

                                // Convertir en niveaux de gris
                                val grayscale = convertToGrayscale(resized)

                                processedImages.add(LabeledImage(grayscale, label, labelIndex))

                                // Augmentation de données
                                if (ENABLE_AUGMENTATION) {
                                    processedImages.add(LabeledImage(
                                        rotateBitmap(grayscale, -15f),
                                        label,
                                        labelIndex
                                    ))
                                    processedImages.add(LabeledImage(
                                        rotateBitmap(grayscale, 15f),
                                        label,
                                        labelIndex
                                    ))
                                    processedImages.add(LabeledImage(
                                        adjustBrightness(grayscale, -0.2f),
                                        label,
                                        labelIndex
                                    ))
                                    processedImages.add(LabeledImage(
                                        adjustBrightness(grayscale, 0.2f),
                                        label,
                                        labelIndex
                                    ))
                                    processedImages.add(LabeledImage(
                                        flipHorizontal(grayscale),
                                        label,
                                        labelIndex
                                    ))
                                }

                                faceBitmap.recycle()
                                resized.recycle()
                            }
                        }

                        bitmap.recycle()
                    }

                    processedCount++
                    val progress = 20 + (processedCount * 20 / totalCount)
                    progressCallback(progress, "Traité $processedCount/$totalCount images")

                } catch (e: Exception) {
                    Log.e(TAG, "Erreur traitement image: ${e.message}", e)
                }
            }
        }

        faceDetector.close()

        // Mélanger et diviser en train/validation (80/20)
        processedImages.shuffle(Random(System.currentTimeMillis()))
        val splitIndex = (processedImages.size * 0.8).toInt()

        TrainingDataset(
            trainImages = processedImages.subList(0, splitIndex),
            validationImages = processedImages.subList(splitIndex, processedImages.size)
        )
    }

    private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val grayscale = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayscale
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun adjustBrightness(bitmap: Bitmap, factor: Float): Bitmap {
        val adjusted = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(adjusted)
        val paint = Paint()
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, factor * 255,
                0f, 1f, 0f, 0f, factor * 255,
                0f, 0f, 1f, 0f, factor * 255,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return adjusted
    }

    private fun flipHorizontal(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply { preScale(-1f, 1f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // lance l'entraînement
    suspend fun trainModel(
        progressCallback: (Int, String) -> Unit
    ): TrainingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            // Étape 1: Validation
            progressCallback(5, "Validation des données...")
            val validation = validateTrainingReadiness()
            if (validation is ValidationResult.Insufficient) {
                return@withContext TrainingResult(
                    success = false,
                    finalAccuracy = 0f,
                    trainingTime = 0,
                    errorMessage = "Données insuffisantes: ${validation.missingCategories.joinToString()}"
                )
            }

            // Étape 2: Téléchargement des images
            progressCallback(10, "Téléchargement des images...")
            val imagesResponse = downloadTrainingImages()
            if (imagesResponse == null) {
                return@withContext TrainingResult(
                    success = false,
                    finalAccuracy = 0f,
                    trainingTime = 0,
                    errorMessage = "Échec du téléchargement des images"
                )
            }

            // Étape 3: Prétraitement
            val dataset = preprocessImages(imagesResponse, progressCallback)
            progressCallback(40, "Prétraitement terminé: ${dataset.trainImages.size} images")

            // Étape 4: Lancement de l'entraînement sur le backend
            progressCallback(45, "Démarrage de l'entraînement...")
            val startResult = dataSource.startTraining()
            if (startResult == null) {
                return@withContext TrainingResult(
                    success = false,
                    finalAccuracy = 0f,
                    trainingTime = 0,
                    errorMessage = "Échec du démarrage de l'entraînement"
                )
            }

            // Le reste du processus est géré par TrainingScreen via polling
            // Cette méthode retourne immédiatement après le lancement

            val trainingTime = System.currentTimeMillis() - startTime

            TrainingResult(
                success = true,
                finalAccuracy = 0f, // Sera mis à jour après l'entraînement
                trainingTime = trainingTime,
                errorMessage = null
            )

        } catch (e: Exception) {
            Log.e(TAG, "Erreur entraînement: ${e.message}", e)
            TrainingResult(
                success = false,
                finalAccuracy = 0f,
                trainingTime = System.currentTimeMillis() - startTime,
                errorMessage = e.message
            )
        }
    }

    fun cleanup() {
        Log.d(TAG, "Nettoyage des ressources")
    }
}
