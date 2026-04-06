package fr.iut.projetmobile.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import fr.iut.projetmobile.model.Club

// Attention : on augmente la version de la base à 5 puisqu'on ajoute la colonne is_dirty à Club
@Database(entities = [Club::class, PendingActionEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clubDao(): ClubDao
    abstract fun pendingActionDao(): PendingActionDao // Ajout du DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder<AppDatabase>(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "projetmobile.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}