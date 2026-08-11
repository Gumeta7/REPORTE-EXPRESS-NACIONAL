package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MachineDao {
    @Query("SELECT * FROM machines ORDER BY machineNumber ASC")
    fun getAllMachines(): Flow<List<MachineEntity>>

    @Query("""
        SELECT * FROM machines 
        WHERE machineNumber LIKE '%' || :query || '%' 
           OR assetNumber LIKE '%' || :query || '%' 
           OR serialNumber LIKE '%' || :query || '%'
           OR brand LIKE '%' || :query || '%'
           OR area LIKE '%' || :query || '%'
           OR game LIKE '%' || :query || '%'
           OR island LIKE '%' || :query || '%'
        ORDER BY machineNumber ASC
    """)
    fun searchMachines(query: String): Flow<List<MachineEntity>>

    @Query("SELECT * FROM machines WHERE machineNumber = :machineNum OR assetNumber = :machineNum LIMIT 1")
    suspend fun getMachineByNumber(machineNum: String): MachineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachine(machine: MachineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMachines(machines: List<MachineEntity>)

    @Query("DELETE FROM machines WHERE id = :id")
    suspend fun deleteMachineById(id: Int)

    @Query("DELETE FROM machines")
    suspend fun clearAllMachines()

    @Query("SELECT COUNT(*) FROM machines")
    suspend fun getMachineCount(): Int
}
