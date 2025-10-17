package com.ltb.sae501

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import com.google.mlkit.vision.face.Face
import kotlin.math.max
import android.graphics.Paint

@Composable
fun FaceOverlay(faces: List<Face>, imageWidth: Int, imageHeight: Int, isFrontCamera: Boolean = true) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (faces.isEmpty()) return@Canvas
        
        val scale = minOf(size.width / imageWidth.toFloat(), size.height / imageHeight.toFloat())
        val offsetX = (size.width - imageWidth * scale) / 2f
        val offsetY = (size.height - imageHeight * scale) / 2f

        for (face in faces) {
            val bbox = face.boundingBox
            val faceWidth = bbox.width() * scale
            val faceHeight = bbox.height() * scale

            var squareSize = max(faceWidth, faceHeight)

            squareSize *= 2f

            // Centre du visage
            var centerX = (bbox.left + bbox.width() / 2f) * scale + offsetX
            val centerY = (bbox.top + bbox.height() / 2f) * scale + offsetY

            if (isFrontCamera) {
                centerX = size.width - centerX
            }

            val left = centerX - squareSize / 2f
            val top = centerY - squareSize / 2f

            drawRect(
                color = Color.Green,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )

            val label = "Visage 85%"
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 40f
                    isAntiAlias = true
                    setShadowLayer(5f, 0f, 0f, android.graphics.Color.BLACK)
                }
                drawText(label, left, maxOf(0f, top - 10f), paint)
            }
        }
    }
}
