package com.ltb.sae501

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Résultat de détection d'émotion
 * @param emotion Le nom de l'émotion (ex: "happy", "sad")
 * @param confidence Score de confiance entre 0 et 1
 */
data class EmotionResult(
    val emotion: String,
    val confidence: Float
)

/**
 * Classe qui détecte les émotions sur un visage
 */
class EmotionDetector(private val context: Context) {

    // L'interpréteur TensorFlow Lite
    private var interpreter: Interpreter? = null

    // Liste des émotions possibles
    private var labels: List<String> = emptyList()

    // Taille des images acceptées par le modèle
    private val inputSize = 48

    init {
        chargerModele()
        chargerLabels()
    }

    /**
     * Charge le modèle d'IA depuis les assets
     */
    private fun chargerModele() {
        try {
            // Charger le fichier .tflite
            val model = FileUtil.loadMappedFile(context, "face_interpretation_model.tflite")

            // Créer l'interpréteur
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

    /**
     * Charge la liste des émotions
     */
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

    /**
     * Extrait la région du visage depuis une image complète
     * @param bitmap Image complète de la caméra
     * @param face Visage détecté par ML Kit
     * @return Bitmap du visage uniquement (ou null si erreur)
     */
    fun extraireVisage(bitmap: Bitmap, face: Face): Bitmap? {
        try {
            val bbox = face.boundingBox

            // marge de 20% autour du visage
            val margeX = (bbox.width() * 0.2f).toInt()
            val margeY = (bbox.height() * 0.2f).toInt()

            // Coordonnées avec marge
            val gauche = maxOf(0, bbox.left - margeX)
            val haut = maxOf(0, bbox.top - margeY)
            val droite = minOf(bitmap.width, bbox.right + margeX)
            val bas = minOf(bitmap.height, bbox.bottom + margeY)

            val largeur = droite - gauche
            val hauteur = bas - haut

            // si visage valide
            if (largeur <= 0 || hauteur <= 0) return null

            // image
            return Bitmap.createBitmap(bitmap, gauche, haut, largeur, hauteur)

        } catch (e: Exception) {
            println("Erreur visage: ${e.message}")
            return null
        }
    }

    /**
     * Détecte l'émotion sur un visage
     * @param visageBitmap Image du visage
     * @return EmotionResult avec l'émotion et la confiance
     */
    fun detecterEmotion(visageBitmap: Bitmap): EmotionResult? {
        // si modèle chargé
        if (interpreter == null) {
            println("Modèle non chargé")
            return null
        }

        try {
            // Redimensionner visage à 48x48
            val visageRedimensionne = Bitmap.createScaledBitmap(
                visageBitmap,
                inputSize,
                inputSize,
                true
            )

            // Convertir image en donnée
            val donnees = convertirEnDonnees(visageRedimensionne)

            // tableau pour recevoir les résultats
            val resultats = Array(1) { FloatArray(labels.size) }

            // prédiction
            interpreter?.run(donnees, resultats)

            // émotion avec la plus haute probabilité
            val probabilites = resultats[0]
            var meilleureEmotion = 0
            var meilleureProba = probabilites[0]

            for (i in 1 until probabilites.size) {
                if (probabilites[i] > meilleureProba) {
                    meilleureProba = probabilites[i]
                    meilleureEmotion = i
                }
            }

            // Nettoyer la mémoire
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

    /**
     * Convertit un Bitmap pour le modèle
     * en niveaux de gris
     * entre 0 et 1
     */
    private fun convertirEnDonnees(bitmap: Bitmap): ByteBuffer {
        // buffer de la bonne taille
        // 4 bytes par pixel (float) * largeur * hauteur * 1 canal (grayscale)
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        // pixels de l'image
        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            // Extraire Rouge, Vert, Bleu
            val rouge = (pixel shr 16) and 0xFF
            val vert = (pixel shr 8) and 0xFF
            val bleu = pixel and 0xFF

            // en niveau de gris
            val gris = (0.299f * rouge + 0.587f * vert + 0.114f * bleu)

            // Normaliser entre 0 et 1 (division par 255)
            val valeurNormalisee = gris / 255.0f

            // Ajouter au buffer
            byteBuffer.putFloat(valeurNormalisee)
        }

        // curseur au début
        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * Libère les ressources
     */
    fun fermer() {
        interpreter?.close()
        interpreter = null
    }
}