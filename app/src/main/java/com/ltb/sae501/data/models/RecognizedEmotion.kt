// app/src/main/java/com/ltb/sae501/data/models/RecognizedEmotion.kt
package com.ltb.sae501.data.models

/**
 * Représente une émotion unique reconnue par le modèle IA.
 * Nécessite @JvmOverloads pour la désérialisation par Firebase.
 */
data class RecognizedEmotion @JvmOverloads constructor(
    var emotion: String = "",        // Nom de l'émotion (Ex: "happy", "angry")
    var confidence: Float = 0.0f,  // Pourcentage de confiance (Ex: 0.98)
    // Vous pouvez ajouter l'emoji ou la description ici si désiré, mais le nœud 'categories' dans RTDB est déjà là pour ça.
)