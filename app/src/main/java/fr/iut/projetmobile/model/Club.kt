package fr.iut.projetmobile.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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

    @ColumnInfo(name = "member_count")
    val memberCount: Int = 0,

    @ColumnInfo(name = "is_dirty")
    val isDirty: Boolean = false
)