package com.ltb.sae501.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object pour les reconnaissances locales
 * Gère toutes les opérations CRUD sur la table recognitions
 */
@Dao
interface RecognitionDao {
    /**
     * Récupère toutes les reconnaissances triées par date (plus récent en premier)
     * Retourne un Flow pour observer les changements en temps réel
     */
    @Query("SELECT * FROM recognitions ORDER BY detectedAt DESC")
    fun getAllRecognitions(): Flow<List<LocalRecognition>>
    
    /**
     * Récupère uniquement les reconnaissances non synchronisées
     * Utilisé lors de la connexion pour synchroniser les données pending
     */
    @Query("SELECT * FROM recognitions WHERE isSynced = 0")
    suspend fun getUnsyncedRecognitions(): List<LocalRecognition>
    
    /**
     * Récupère les reconnaissances d'un utilisateur spécifique
     */
    @Query("SELECT * FROM recognitions WHERE userId = :userId ORDER BY detectedAt DESC")
    fun getRecognitionsByUser(userId: String): Flow<List<LocalRecognition>>
    
    /**
     * Compte le nombre de reconnaissances non synchronisées
     * Flow pour afficher un badge en temps réel dans le profil
     */
    @Query("SELECT COUNT(*) FROM recognitions WHERE isSynced = 0")
    fun getUnsyncedCount(): Flow<Int>
    
    /**
     * Compte le nombre total de reconnaissances
     */
    @Query("SELECT COUNT(*) FROM recognitions")
    fun getTotalCount(): Flow<Int>
    
    /**
     * Insère une nouvelle reconnaissance
     * OnConflictStrategy.REPLACE remplace si l'ID existe déjà
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recognition: LocalRecognition): Long
    
    /**
     * Insère plusieurs reconnaissances à la fois
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recognitions: List<LocalRecognition>)
    
    /**
     * Met à jour une reconnaissance existante
     * Utilisé pour marquer comme synchronisée
     */
    @Update
    suspend fun update(recognition: LocalRecognition)
    
    /**
     * Supprime une reconnaissance
     */
    @Delete
    suspend fun delete(recognition: LocalRecognition)
    
    /**
     * Supprime une reconnaissance par son ID
     */
    @Query("DELETE FROM recognitions WHERE id = :id")
    suspend fun deleteById(id: String)
    
    /**
     * Supprime toutes les reconnaissances
     * Utilisé pour le "reset" de l'application
     */
    @Query("DELETE FROM recognitions")
    suspend fun deleteAll()
    
    /**
     * Marque une reconnaissance comme synchronisée
     */
    @Query("UPDATE recognitions SET isSynced = 1, remoteId = :remoteId WHERE id = :localId")
    suspend fun markAsSynced(localId: String, remoteId: String)
    
    /**
     * Met à jour l'userId pour toutes les reconnaissances locales après connexion
     */
    @Query("UPDATE recognitions SET userId = :userId WHERE userId IS NULL")
    suspend fun assignUserToLocalRecognitions(userId: String)
}
