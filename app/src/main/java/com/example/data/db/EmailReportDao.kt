package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailReportDao {
    @Query("SELECT * FROM email_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<EmailReportEntity>>

    @Query("""
        SELECT * FROM email_reports 
        WHERE machineNumber LIKE '%' || :query || '%' 
           OR issueDescription LIKE '%' || :query || '%'
           OR subject LIKE '%' || :query || '%'
           OR recipient LIKE '%' || :query || '%'
           OR brand LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchReports(query: String): Flow<List<EmailReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: EmailReportEntity): Long

    @Query("DELETE FROM email_reports WHERE id = :id")
    suspend fun deleteReportById(id: Int)

    @Query("DELETE FROM email_reports")
    suspend fun clearAllReports()
}
