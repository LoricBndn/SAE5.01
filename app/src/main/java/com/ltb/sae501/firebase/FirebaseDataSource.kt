package com.ltb.sae501.firebase

import android.net.Uri
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.storage.FirebaseStorage
import com.ltb.sae501.data.models.RecognitionResult
import com.ltb.sae501.data.models.RecognizedEmotion
import com.ltb.sae501.data.models.EmotionCategory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseDataSource {
    private val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val historyRef = databaseRef.child("history")
    private val categoriesRef = databaseRef.child("categories")
    private val storageRef = FirebaseStorage.getInstance().reference

    /**
     * Sauvegarde l'image dans Storage et les métadonnées de l'émotion dans RTDB.
     */
    suspend fun saveRecognition(
        imageUri: Uri,
        recognizedEmotions: List<RecognizedEmotion>
    ): Boolean {
        val newKey = historyRef.push().key ?: return false
        val imagePath = "recognitions/$newKey.jpg"
        val fileRef = storageRef.child(imagePath)

        try {
            fileRef.putFile(imageUri).await()
            val downloadUrl = fileRef.downloadUrl.await().toString()

            val result = RecognitionResult(
                id = newKey,
                timestamp = System.currentTimeMillis(),
                imageStorageUrl = downloadUrl,
                recognizedEmotions = recognizedEmotions,
                userId = "user_test_sae501"
            )

            historyRef.child(newKey).setValue(result).await()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Récupère l'historique de l'utilisateur actuel sous forme de Flow.
     */
    fun getHistory(): Flow<List<RecognitionResult>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val results = snapshot.children.mapNotNull { child ->
                    child.getValue(RecognitionResult::class.java)
                }.sortedByDescending { it.timestamp }

                trySend(results)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        historyRef.addValueEventListener(listener)
        awaitClose { historyRef.removeEventListener(listener) }
    }

    /**
     * Récupère toutes les catégories d'émotions avec leurs images d'entraînement
     */
    fun getCategories(): Flow<List<EmotionCategory>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categories = mutableListOf<EmotionCategory>()

                for (categorySnapshot in snapshot.children) {
                    val categoryId = categorySnapshot.key ?: continue

                    // Récupérer les données de base
                    val name = categorySnapshot.child("name").getValue(String::class.java) ?: ""
                    val nameEn = categorySnapshot.child("nameEn").getValue(String::class.java) ?: ""
                    val emoji = categorySnapshot.child("emoji").getValue(String::class.java) ?: ""
                    val color = categorySnapshot.child("color").getValue(String::class.java) ?: ""
                    val description = categorySnapshot.child("description").getValue(String::class.java) ?: ""
                    val imageCount = categorySnapshot.child("imageCount").getValue(Int::class.java) ?: 0
                    val createdAt = categorySnapshot.child("createdAt").getValue(Long::class.java) ?: 0L

                    // Récupérer les URLs des images d'entraînement
                    val trainingImages = mutableListOf<String>()
                    val imagesSnapshot = categorySnapshot.child("trainingImages")
                    for (imageSnapshot in imagesSnapshot.children) {
                        imageSnapshot.getValue(String::class.java)?.let { url ->
                            trainingImages.add(url)
                        }
                    }

                    categories.add(
                        EmotionCategory(
                            id = categoryId,
                            name = name,
                            nameEn = nameEn,
                            emoji = emoji,
                            color = color,
                            description = description,
                            imageCount = imageCount,
                            trainingImages = trainingImages,
                            createdAt = createdAt,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                }

                // Trier par ordre alphabétique
                trySend(categories.sortedBy { it.name })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        categoriesRef.addValueEventListener(listener)
        awaitClose { categoriesRef.removeEventListener(listener) }
    }

    /**
     * Upload une image d'entraînement pour une catégorie d'émotion
     */
    suspend fun uploadTrainingImage(categoryId: String, imageUri: Uri): Boolean {
        try {
            // 1. Générer un ID unique pour l'image
            val imageId = System.currentTimeMillis().toString()
            val imagePath = "training/$categoryId/$imageId.jpg"
            val fileRef = storageRef.child(imagePath)

            // 2. Upload l'image dans Storage
            fileRef.putFile(imageUri).await()
            val downloadUrl = fileRef.downloadUrl.await().toString()

            // 3. Ajouter l'URL à la liste des images d'entraînement
            val categoryRef = categoriesRef.child(categoryId)

            // Récupérer le nombre actuel d'images
            val currentCountSnapshot = categoryRef.child("imageCount").get().await()
            val currentCount = currentCountSnapshot.getValue(Int::class.java) ?: 0

            // Ajouter l'URL à la liste
            categoryRef.child("trainingImages").child(imageId).setValue(downloadUrl).await()

            // Mettre à jour le compteur
            categoryRef.child("imageCount").setValue(currentCount + 1).await()
            categoryRef.child("lastUpdated").setValue(System.currentTimeMillis()).await()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Supprime une image d'entraînement
     */
    suspend fun deleteTrainingImage(categoryId: String, imageUrl: String): Boolean {
        try {
            // 1. Trouver l'ID de l'image dans la base de données
            val categoryRef = categoriesRef.child(categoryId)
            val imagesSnapshot = categoryRef.child("trainingImages").get().await()

            var imageIdToDelete: String? = null
            for (imageSnapshot in imagesSnapshot.children) {
                if (imageSnapshot.getValue(String::class.java) == imageUrl) {
                    imageIdToDelete = imageSnapshot.key
                    break
                }
            }

            if (imageIdToDelete == null) return false

            // 2. Supprimer de Storage
            val storageUrl = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageUrl.delete().await()

            // 3. Supprimer de la base de données
            categoryRef.child("trainingImages").child(imageIdToDelete).removeValue().await()

            // 4. Mettre à jour le compteur
            val currentCountSnapshot = categoryRef.child("imageCount").get().await()
            val currentCount = currentCountSnapshot.getValue(Int::class.java) ?: 0
            categoryRef.child("imageCount").setValue(maxOf(0, currentCount - 1)).await()
            categoryRef.child("lastUpdated").setValue(System.currentTimeMillis()).await()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Initialise les catégories par défaut si elles n'existent pas
     */
    suspend fun initializeDefaultCategories(): Boolean {
        try {
            val snapshot = categoriesRef.get().await()

            // Si des catégories existent déjà, ne rien faire
            if (snapshot.exists() && snapshot.childrenCount > 0) {
                return true
            }

            // Créer les catégories par défaut du dataset FER-2013
            val defaultCategories = mapOf(
                "angry" to mapOf(
                    "name" to "Colère",
                    "nameEn" to "Angry",
                    "emoji" to "😠",
                    "color" to "#FF4444",
                    "description" to "Expressions de colère et d'irritation",
                    "imageCount" to 0,
                    "createdAt" to System.currentTimeMillis()
                ),
                "disgust" to mapOf(
                    "name" to "Dégoût",
                    "nameEn" to "Disgust",
                    "emoji" to "🤢",
                    "color" to "#8B4513",
                    "description" to "Expressions de dégoût et de répulsion",
                    "imageCount" to 0,
                    "createdAt" to System.currentTimeMillis()
                ),
                "fear" to mapOf(
                    "name" to "Peur",
                    "nameEn" to "Fear",
                    "emoji" to "😨",
                    "color" to "#9370DB",
                    "description" to "Expressions de peur et d'anxiété",
                    "imageCount" to 0,
                    "createdAt" to System.currentTimeMillis()
                ),
                "happy" to mapOf(
                    "name" to "Joie",
                    "nameEn" to "Happy",
                    "emoji" to "😄",
                    "color" to "#FFD700",
                    "description" to "Expressions de joie et de bonheur",
                    "imageCount" to 0,
                    "createdAt" to System.currentTimeMillis()
                ),
                "sad" to mapOf(
                    "name" to "Tristesse",
                    "nameEn" to "Sad",
                    "emoji" to "😢",
                    "color" to "#4169E1",
                    "description" to "Expressions de tristesse et de mélancolie",
                    "imageCount" to 0,
                    "createdAt" to System.currentTimeMillis()
                ),
                "surprise" to mapOf(
                    "name" to "Surprise",
                    "nameEn" to "Surprise",
                    "emoji" to "😲",
                    "color" to "#FF69B4",
                    "description" to "Expressions de surprise et d'étonnement",
                    "imageCount" to 0,
                    "createdAt" to System.currentTimeMillis()
                ),
                "neutral" to mapOf(
                    "name" to "Neutre",
                    "nameEn" to "Neutral",
                    "emoji" to "😐",
                    "color" to "#808080",
                    "description" to "Expressions neutres sans émotion marquée",
                    "imageCount" to 0,
                    "createdAt" to System.currentTimeMillis()
                )
            )

            // Écrire chaque catégorie dans Firebase
            for ((categoryId, categoryData) in defaultCategories) {
                categoriesRef.child(categoryId).setValue(categoryData).await()
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}