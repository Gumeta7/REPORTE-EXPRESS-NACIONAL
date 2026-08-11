package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderEmailDao {
    @Query("SELECT * FROM provider_emails ORDER BY providerName ASC")
    fun getAllProviderEmails(): Flow<List<ProviderEmailEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviderEmail(providerEmail: ProviderEmailEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProviderEmails(providers: List<ProviderEmailEntity>)

    @Query("DELETE FROM provider_emails WHERE id = :id")
    suspend fun deleteProviderEmailById(id: Int)

    @Query("DELETE FROM provider_emails")
    suspend fun clearAllProviderEmails()

    @Query("SELECT COUNT(*) FROM provider_emails")
    suspend fun getProviderEmailCount(): Int
}
