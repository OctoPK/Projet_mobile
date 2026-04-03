package fr.iut.projetmobile.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ressource choisie : Club
 * Adaptez les champs selon votre table dans la base de données du serveur.
 */
@Entity(tableName = "clubs")
data class Club(
    @PrimaryKey
    val id: Int,
    val nom: String,
    val ville: String,
    val isDirty: Boolean = false  // true = modifié localement, à synchroniser
)
