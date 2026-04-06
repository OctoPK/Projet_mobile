package fr.iut.projetmobile.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * File d'attente des actions effectuées hors-ligne.
 * Ces actions seront rejouées lors du retour de la connectivité.
 */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String,      // Ex: CREATE_CLUB, UPDATE_CLUB
    val clubId: Int,
    val payload: String? = null, // Body JSON du club sauvegardé
    val createdAt: Long = System.currentTimeMillis()
)