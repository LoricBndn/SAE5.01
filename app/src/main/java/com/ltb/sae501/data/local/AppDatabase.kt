package com.ltb.sae501.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de données Room pour l'application
 * Stocke les reconnaissances d'émotions localement
 */
@Database(
    entities = [LocalRecognition::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun recognitionDao(): RecognitionDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Obtient l'instance singleton de la base de données
         * Thread-safe avec double-checked locking
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sae501_database"
                )
                    .fallbackToDestructiveMigration() // Pour le développement, supprimer en production
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
