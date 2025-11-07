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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseDataSource {
    private val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("history")
    private val storageRef = FirebaseStorage.getInstance().reference

    /**
     * Sauvegarde l'image dans Storage et les métadonnées de l'émotion dans RTDB.
     * @param imageUri L'URI locale de l'image capturée.
     * @param recognizedEmotions La liste des émotions reconnues.
     * @return True si la sauvegarde est réussie.
     */
    suspend fun saveRecognition(
        imageUri: Uri,
        recognizedEmotions: List<RecognizedEmotion>
    ): Boolean {
        // 1. Préparation de l'ID unique et du chemin
        val newKey = databaseRef.push().key ?: return false
        val imagePath = "recognitions/$newKey.jpg"
        val fileRef = storageRef.child(imagePath)

        try {
            // 2. Upload de l'image dans Firebase Storage
            fileRef.putFile(imageUri).await()
            val downloadUrl = fileRef.downloadUrl.await().toString()

            // 3. Création de l'objet de résultat
            val result = RecognitionResult(
                id = newKey,
                timestamp = System.currentTimeMillis(),
                imageStorageUrl = downloadUrl,
                recognizedEmotions = recognizedEmotions,
                userId = "user_test_sae501"
            )

            // 4. Sauvegarde des métadonnées dans Realtime Database
            databaseRef.child(newKey).setValue(result).await()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            // Log l'échec et retourne faux
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

        // Attache le listener au nœud 'history'
        databaseRef.addValueEventListener(listener)
        awaitClose { databaseRef.removeEventListener(listener) }
    }
}