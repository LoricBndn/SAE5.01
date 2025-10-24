package com.ltb.sae501

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import com.google.mlkit.vision.face.Face
import kotlin.math.max

/**
 * Composant qui dessine les rectangles et émotions par-dessus la caméra
 *
 * @param faces Liste des visages
 * @param emotions Map contenant les émotions
 * @param imageWidth Largeur de la caméra
 * @param imageHeight Hauteur de la caméra
 * @param isFrontCamera True si caméra frontale (pour inverser horizontalement)
 */
@Composable
fun FaceOverlay(
    faces: List<Face>,
    emotions: Map<Int, EmotionResult>,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean = true
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Si pas de visages, ne rien dessiner
        if (faces.isEmpty()) return@Canvas

        // Calculer le ratio pour adapter l'image caméra à l'écran
        val scale = minOf(
            size.width / imageWidth.toFloat(),
            size.height / imageHeight.toFloat()
        )

        // centrer l'image
        val offsetX = (size.width - imageWidth * scale) / 2f
        val offsetY = (size.height - imageHeight * scale) / 2f

        // Pour chaque visage détecté
        for ((index, visage) in faces.withIndex()) {
            val boite = visage.boundingBox

            // Calculer la taille du carré
            val largeurVisage = boite.width() * scale
            val hauteurVisage = boite.height() * scale
            var tailleCarree = max(largeurVisage, hauteurVisage)
            tailleCarree *= 2f

            // centre du visage
            var centreX = (boite.left + boite.width() / 2f) * scale + offsetX
            val centreY = (boite.top + boite.height() / 2f) * scale + offsetY

            // Si caméra frontale, inverser horizontalement (effet miroir)
            if (isFrontCamera) {
                centreX = size.width - centreX
            }

            // Position du coin haut-gauche du carré
            val gauche = centreX - tailleCarree / 2f
            val haut = centreY - tailleCarree / 2f

            // Dessiner le rectangle
            drawRect(
                color = Color.Red,
                topLeft = Offset(gauche, haut),
                size = androidx.compose.ui.geometry.Size(tailleCarree, tailleCarree),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )

            // texte à afficher
            val texte = creerTexte(emotions[index])

            // Dessiner le texte au-dessus du rectangle
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 40f
                    isAntiAlias = true
                    setShadowLayer(5f, 0f, 0f, android.graphics.Color.BLACK)
                }
                drawText(texte, gauche, maxOf(0f, haut - 10f), paint)
            }
        }
    }
}

/**
 * Crée le texte à afficher (émotion en français + pourcentage)
 */
private fun creerTexte(emotion: EmotionResult?): String {
    if (emotion == null) {
        return "Analyse..."
    }

    val emotionFrancais = traduireEmotion(emotion.emotion)
    val pourcentage = (emotion.confidence * 100).toInt()

    return "$emotionFrancais $pourcentage%"
}

/**
 * Traduit les émotions de l'anglais vers le français
 */
private fun traduireEmotion(emotion: String): String {
    return when (emotion.lowercase()) {
        "happy" -> "Joyeux"
        "sad" -> "Triste"
        "angry" -> "En colère"
        "surprise" -> "Surpris"
        "fear" -> "Effrayé"
        "disgust" -> "Dégoûté"
        "neutral" -> "Neutre"
        else -> emotion // Si inconnu, garder tel quel
    }
}