package com.ltb.sae501.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.face.Face
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DualModelDetector(private val context: Context) {

    private var baseInterpreter: Interpreter? = null
    private var customInterpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private val inputSize = 48
    private val metadataManager = ModelMetadataManager(context)

    init {
        chargerModeleBase()
        chargerLabels()
        chargerModelePersonnalise()
    }

    private fun chargerModeleBase() {
        try {
            val model = FileUtil.loadMappedFile(context, "face_interpretation_model.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            baseInterpreter = Interpreter(model, options)
            Log.d(TAG, "Modèle de base chargé avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur chargement modèle de base: ${e.message}", e)
        }
    }

    private fun chargerLabels() {
        try {
            labels = context.assets.open("labels.txt")
                .bufferedReader()
                .readLines()
            Log.d(TAG, "${labels.size} émotions chargées: $labels")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur chargement labels: ${e.message}", e)
        }
    }

    private fun chargerModelePersonnalise() {
        try {
            if (metadataManager.customModelExists()) {
                val modelPath = metadataManager.getCustomModelPath()
                val modelFile = File(modelPath)

                if (modelFile.exists() && modelFile.length() > 0) {
                    val options = Interpreter.Options().apply {
                        setNumThreads(4)
                    }
                    customInterpreter = Interpreter(modelFile, options)
                    val metadata = metadataManager.loadMetadata()
                    Log.d(TAG, "Modèle personnalisé chargé avec succès (${modelFile.length()} bytes)")
                    if (metadata != null) {
                        Log.d(TAG, "Métadonnées modèle: précision=${(metadata.accuracy * 100).toInt()}%, images=${metadata.trainingImageCount}")
                    }
                    Log.d(TAG, "Double IA activée: modèle de base + modèle personnalisé")
                } else {
                    Log.w(TAG, "Fichier modèle personnalisé existe mais est vide")
                }
            } else {
                Log.d(TAG, "Aucun modèle personnalisé trouvé")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur chargement modèle personnalisé: ${e.message}", e)
            customInterpreter = null
        }
    }

    fun reloadCustomModel() {
        try {
            Log.d(TAG, "Rechargement du modèle personnalisé...")
            customInterpreter?.close()
            customInterpreter = null
            chargerModelePersonnalise()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur rechargement modèle personnalisé: ${e.message}", e)
        }
    }

    fun extraireVisage(bitmap: Bitmap, face: Face): Bitmap? {
        try {
            val bbox = face.boundingBox

            val margeX = (bbox.width() * 0.2f).toInt()
            val margeY = (bbox.height() * 0.2f).toInt()

            val gauche = maxOf(0, bbox.left - margeX)
            val haut = maxOf(0, bbox.top - margeY)
            val droite = minOf(bitmap.width, bbox.right + margeX)
            val bas = minOf(bitmap.height, bbox.bottom + margeY)

            val largeur = droite - gauche
            val hauteur = bas - haut

            if (largeur <= 0 || hauteur <= 0) {
                return null
            }

            return Bitmap.createBitmap(bitmap, gauche, haut, largeur, hauteur)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur extraction visage: ${e.message}", e)
            return null
        }
    }

    // détection avec les 2 modèles
    fun detecterEmotion(visageBitmap: Bitmap): CombinedEmotionResult? {
        try {
            if (baseInterpreter == null) {
                Log.e(TAG, "Modèle de base non chargé")
                return null
            }

            val visageRedimensionne = Bitmap.createScaledBitmap(
                visageBitmap,
                inputSize,
                inputSize,
                true
            )

            val donnees = convertirEnDonnees(visageRedimensionne)

            // Inférence avec le modèle de base
            val baseResults = Array(1) { FloatArray(labels.size) }
            baseInterpreter?.run(donnees.duplicate(), baseResults)
            val baseProbabilities = baseResults[0]

            // Inférence avec le modèle personnalisé (si disponible)
            val customProbabilities = if (customInterpreter != null) {
                try {
                    Log.d(TAG, "Exécution inférence sur modèle personnalisé")
                    val customResults = Array(1) { FloatArray(labels.size) }
                    customInterpreter?.run(donnees.duplicate(), customResults)
                    Log.d(TAG, "Inférence modèle personnalisé réussie")
                    customResults[0]
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur inférence modèle personnalisé: ${e.message}", e)
                    null
                }
            } else {
                null
            }

            // Combiner les prédictions
            val result = PredictionCombiner.combine(baseProbabilities, customProbabilities, labels)
            Log.d(TAG, "Résultat détection: émotion=${result.emotion}, confiance=${result.confidence}, source=${result.source}")
            return result

        } catch (e: Exception) {
            Log.e(TAG, "Erreur détection émotion: ${e.message}", e)
            return null
        }
    }

    // bitmap vers données TFLite
    private fun convertirEnDonnees(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            val rouge = (pixel shr 16) and 0xFF
            val vert = (pixel shr 8) and 0xFF
            val bleu = pixel and 0xFF

            // grayscale
            val gris = (0.299f * rouge + 0.587f * vert + 0.114f * bleu)
            val valeurNormalisee = gris / 255.0f

            byteBuffer.putFloat(valeurNormalisee)
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    // pour réutiliser le buffer
    private fun ByteBuffer.duplicate(): ByteBuffer {
        val duplicate = ByteBuffer.allocateDirect(this.capacity())
        duplicate.order(this.order())
        this.rewind()
        duplicate.put(this)
        duplicate.rewind()
        this.rewind()
        return duplicate
    }

    fun hasCustomModel(): Boolean {
        return customInterpreter != null
    }

    fun getCustomModelMetadata(): ModelMetadata? {
        return metadataManager.loadMetadata()
    }

    fun fermer() {
        try {
            baseInterpreter?.close()
            baseInterpreter = null

            customInterpreter?.close()
            customInterpreter = null

            Log.d(TAG, "Interpréteurs fermés")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur fermeture interpréteurs: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "DualModelDetector"
    }
}
