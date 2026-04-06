package fr.iut.projetmobile.database

import androidx.room.*

@Dao
interface PendingActionDao {
    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    fun getAllPendingActions(): List<PendingActionEntity>

    @Insert
    fun insert(action: PendingActionEntity)

    @Delete
    fun delete(action: PendingActionEntity)

    @Query("DELETE FROM pending_actions")
    fun deleteAll()
}