package com.ltb.sae501

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Résultat de la détection
data class EmotionResult(
    val emotion: String,
    val confidence: Float
)

class EmotionDetector(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private val inputSize = 48

    init {
        chargerModele()
        chargerLabels()
    }

    private fun chargerModele() {
        try {
            val model = FileUtil.loadMappedFile(context, "face_interpretation_model.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(model, options)

            println("Modèle émotions chargé avec succès")
        } catch (e: Exception) {
            println("Erreur chargement modèle: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun chargerLabels() {
        try {
            labels = context.assets.open("labels.txt")
                .bufferedReader()
                .readLines()
            println("${labels.size} émotions chargées: $labels")
        } catch (e: Exception) {
            println("Erreur chargement labels: ${e.message}")
        }
    }

    // Extraire juste le visage de l'image
    fun extraireVisage(bitmap: Bitmap, face: Face): Bitmap? {
        try {
            val bbox = face.boundingBox

            // ajouter marge autour
            val margeX = (bbox.width() * 0.2f).toInt()
            val margeY = (bbox.height() * 0.2f).toInt()

            val gauche = maxOf(0, bbox.left - margeX)
            val haut = maxOf(0, bbox.top - margeY)
            val droite = minOf(bitmap.width, bbox.right + margeX)
            val bas = minOf(bitmap.height, bbox.bottom + margeY)

            val largeur = droite - gauche
            val hauteur = bas - haut

            if (largeur <= 0 || hauteur <= 0) return null

            return Bitmap.createBitmap(bitmap, gauche, haut, largeur, hauteur)

        } catch (e: Exception) {
            println("Erreur visage: ${e.message}")
            return null
        }
    }

    fun detecterEmotion(visageBitmap: Bitmap): EmotionResult? {
        if (interpreter == null) {
            println("Modèle non chargé")
            return null
        }

        try {
            // resize en 48x48
            val visageRedimensionne = Bitmap.createScaledBitmap(
                visageBitmap,
                inputSize,
                inputSize,
                true
            )

            val donnees = convertirEnDonnees(visageRedimensionne)
            val resultats = Array(1) { FloatArray(labels.size) }

            interpreter?.run(donnees, resultats)

            // trouver meilleure proba
            val probabilites = resultats[0]
            var meilleureEmotion = 0
            var meilleureProba = probabilites[0]

            for (i in 1 until probabilites.size) {
                if (probabilites[i] > meilleureProba) {
                    meilleureProba = probabilites[i]
                    meilleureEmotion = i
                }
            }

            visageRedimensionne.recycle()

            return EmotionResult(
                emotion = labels[meilleureEmotion],
                confidence = meilleureProba
            )

        } catch (e: Exception) {
            println("Erreur détection émotion: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    // Convertir en grayscale pour le modèle
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

    fun fermer() {
        interpreter?.close()
        interpreter = null
    }
}