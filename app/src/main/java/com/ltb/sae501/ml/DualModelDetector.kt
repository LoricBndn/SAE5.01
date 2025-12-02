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
                    Log.d(TAG, "Modèle personnalisé chargé avec succès (${modelFile.length()} bytes)")
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

    /**
     * Recharge le modèle personnalisé (appelé après un entraînement)
     */
    fun reloadCustomModel() {
        try {
            customInterpreter?.close()
            customInterpreter = null
            chargerModelePersonnalise()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur rechargement modèle personnalisé: ${e.message}", e)
        }
    }

    /**
     * Extrait le visage de l'image avec une marge de 20%
     */
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

    /**
     * Détecte l'émotion en utilisant le modèle dual (base + custom)
     */
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
                    val customResults = Array(1) { FloatArray(labels.size) }
                    customInterpreter?.run(donnees.duplicate(), customResults)
                    customResults[0]
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur inférence modèle personnalisé: ${e.message}", e)
                    null
                }
            } else {
                null
            }

            // Combiner les prédictions
            return PredictionCombiner.combine(baseProbabilities, customProbabilities, labels)

        } catch (e: Exception) {
            Log.e(TAG, "Erreur détection émotion: ${e.message}", e)
            return null
        }
    }

    /**
     * Convertit le bitmap en données d'entrée pour le modèle TFLite
     */
    private fun convertirEnDonnees(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            val rouge = (pixel shr 16) and 0xFF
            val vert = (pixel shr 8) and 0xFF
            val bleu = pixel and 0xFF

            // Conversion en niveaux de gris (luminosité)
            val gris = (0.299f * rouge + 0.587f * vert + 0.114f * bleu)
            val valeurNormalisee = gris / 255.0f

            byteBuffer.putFloat(valeurNormalisee)
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * Duplique le ByteBuffer pour pouvoir l'utiliser plusieurs fois
     */
    private fun ByteBuffer.duplicate(): ByteBuffer {
        val duplicate = ByteBuffer.allocateDirect(this.capacity())
        duplicate.order(this.order())
        this.rewind()
        duplicate.put(this)
        duplicate.rewind()
        this.rewind()
        return duplicate
    }

    /**
     * Vérifie si un modèle personnalisé est chargé
     */
    fun hasCustomModel(): Boolean {
        return customInterpreter != null
    }

    /**
     * Récupère les métadonnées du modèle personnalisé
     */
    fun getCustomModelMetadata(): ModelMetadata? {
        return metadataManager.loadMetadata()
    }

    /**
     * Ferme les interpréteurs et libère les ressources
     */
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
