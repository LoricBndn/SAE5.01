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
import com.ltb.sae501.ml.CombinedEmotionResult
import kotlin.math.max

// Dessine les rectangles et émotions sur la caméra
@Composable
fun FaceOverlay(
    faces: List<Face>,
    emotions: Map<Int, CombinedEmotionResult>,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean = true,
    showPercentage: Boolean = true
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (faces.isEmpty()) return@Canvas

        val scale = minOf(
            size.width / imageWidth.toFloat(),
            size.height / imageHeight.toFloat()
        )

        val offsetX = (size.width - imageWidth * scale) / 2f
        val offsetY = (size.height - imageHeight * scale) / 2f

        for ((index, visage) in faces.withIndex()) {
            val boite = visage.boundingBox

            val largeurVisage = boite.width() * scale
            val hauteurVisage = boite.height() * scale
            var tailleCarree = max(largeurVisage, hauteurVisage)
            tailleCarree *= 2f

            var centreX = (boite.left + boite.width() / 2f) * scale + offsetX
            val centreY = (boite.top + boite.height() / 2f) * scale + offsetY

            if (isFrontCamera) {
                centreX = size.width - centreX
            }

            val gauche = centreX - tailleCarree / 2f
            val haut = centreY - tailleCarree / 2f

            drawRect(
                color = Color.Red,
                topLeft = Offset(gauche, haut),
                size = androidx.compose.ui.geometry.Size(tailleCarree, tailleCarree),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )

            val texte = creerTexte(emotions[index], showPercentage)

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

private fun creerTexte(emotion: CombinedEmotionResult?, showPercentage: Boolean): String {
    if (emotion == null) {
        return "Analyse..."
    }

    val emotionFrancais = traduireEmotion(emotion.emotion)
    
    return if (showPercentage) {
        val pourcentage = (emotion.confidence * 100).toInt()
        "$emotionFrancais $pourcentage%"
    } else {
        emotionFrancais
    }
}

private fun traduireEmotion(emotion: String): String {
    return when (emotion.lowercase()) {
        "happy" -> "Joyeux"
        "sad" -> "Triste"
        "angry" -> "En colère"
        "surprise" -> "Surpris"
        "fear" -> "Effrayé"
        "disgust" -> "Dégoûté"
        "neutral" -> "Neutre"
        else -> emotion
    }
}