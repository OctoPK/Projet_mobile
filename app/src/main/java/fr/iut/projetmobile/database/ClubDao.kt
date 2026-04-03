package fr.iut.projetmobile.database

import androidx.room.*
import fr.iut.projetmobile.model.Club

@Dao
interface ClubDao {

    @Query("SELECT * FROM clubs ORDER BY nom ASC")
    fun getAll(): List<Club>

    @Query("SELECT * FROM clubs WHERE id = :id")
    fun getById(id: Int): Club?

    @Query("SELECT * FROM clubs WHERE isDirty = 1")
    fun getDirty(): List<Club>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(club: Club)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(clubs: List<Club>)

    @Update
    fun update(club: Club)

    @Delete
    fun delete(club: Club)

    @Query("DELETE FROM clubs")
    fun deleteAll()
}
