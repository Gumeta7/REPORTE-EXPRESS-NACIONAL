package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MachineDao {
    @Query("SELECT * FROM machines ORDER BY sala ASC, machineNumber ASC")
    fun getAllMachines(): Flow<List<MachineEntity>>

    @Query("""
        SELECT * FROM machines 
        WHERE machineNumber LIKE '%' || :query || '%' 
           OR assetNumber LIKE '%' || :query || '%' 
           OR serialNumber LIKE '%' || :query || '%'
           OR brand LIKE '%' || :query || '%'
           OR model LIKE '%' || :query || '%'
           OR area LIKE '%' || :query || '%'
           OR game LIKE '%' || :query || '%'
           OR island LIKE '%' || :query || '%'
           OR sala LIKE '%' || :query || '%'
           OR propietario LIKE '%' || :query || '%'
           OR qrId LIKE '%' || :query || '%'
        ORDER BY sala ASC, machineNumber ASC
    """)
    fun searchMachines(query: String): Flow<List<MachineEntity>>

    @Query("SELECT DISTINCT sala FROM machines WHERE sala != '' ORDER BY sala ASC")
    fun getDistinctSalas(): Flow<List<String>>

    @Query("SELECT * FROM machines WHERE machineNumber = :machineNum OR assetNumber = :machineNum OR serialNumber = :machineNum LIMIT 1")
    suspend fun getMachineByNumber(machineNum: String): MachineEntity?

    @Query("""
        SELECT * FROM machines 
        WHERE LOWER(TRIM(serialNumber)) = LOWER(TRIM(:key)) 
           OR LOWER(TRIM(assetNumber)) = LOWER(TRIM(:key)) 
           OR LOWER(TRIM(qrId)) = LOWER(TRIM(:key))
           OR LOWER(TRIM(machineNumber)) = LOWER(TRIM(:key))
        LIMIT 1
    """)
    suspend fun getMachineBySerialOrAssetOrQr(key: String): MachineEntity?

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
