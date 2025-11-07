// app/src/main/java/com/ltb/sae501/data/models/RecognitionResult.kt
package com.ltb.sae501.data.models

/**
 * Représente un enregistrement complet de reconnaissance d'émotion, prêt pour Firebase.
 * Nécessite @JvmOverloads pour la désérialisation par Firebase.
 */
data class RecognitionResult @JvmOverloads constructor(
    var id: String = "",                               // Clé unique générée par RTDB
    var timestamp: Long = System.currentTimeMillis(),    // Date et heure de la reconnaissance (epoch time)
    var imageStorageUrl: String = "",                    // URL de téléchargement de l'image capturée (Firebase Storage)
    var recognizedEmotions: List<RecognizedEmotion> = emptyList(), // La liste des émotions classées
    var userId: String? = null                           // ID de l'utilisateur (pour le partage, fonctionnalité supplémentaire du SAE)
)