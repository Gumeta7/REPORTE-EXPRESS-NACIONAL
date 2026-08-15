package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TechnicianDao {
    @Query("SELECT * FROM technicians ORDER BY nombre ASC")
    fun getAllTechnicians(): Flow<List<TechnicianEntity>>

    @Query("SELECT * FROM technicians WHERE LOWER(TRIM(usuario)) = LOWER(TRIM(:user)) LIMIT 1")
    suspend fun getTechnicianByUser(user: String): TechnicianEntity?

    @Query("SELECT * FROM technicians WHERE LOWER(TRIM(usuario)) = LOWER(TRIM(:user)) AND password = :pass LIMIT 1")
    suspend fun authenticate(user: String, pass: String): TechnicianEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTechnicians(technicians: List<TechnicianEntity>)

    @Query("DELETE FROM technicians")
    suspend fun clearAllTechnicians()

    @Query("SELECT COUNT(*) FROM technicians")
    suspend fun getTechnicianCount(): Int
}
