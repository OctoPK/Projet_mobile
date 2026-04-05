package fr.iut.projetmobile.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ressource choisie : Club
 * Adaptez les champs selon la documentation de l'API.
 */
@Entity(tableName = "clubs")
data class Club(
    @PrimaryKey
    @ColumnInfo(name = "club_id")
    val id: Int,

    @ColumnInfo(name = "club_name")
    val nom: String,

    @ColumnInfo(name = "club_street")
    val rue: String? = null,

    @ColumnInfo(name = "club_city")
    val ville: String,

    @ColumnInfo(name = "club_postal_code")
    val codePostal: String? = null,

    @ColumnInfo(name = "is_approved")
    val isApproved: Boolean = false,

    val isDirty: Boolean = false  // true = modifié localement, à synchroniser
)
