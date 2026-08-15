package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "machines")
data class MachineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val machineNumber: String, // e.g. "444", "1025", "456"
    val assetNumber: String,   // e.g. "456", "AST-0444", "ASSET-1025"
    val serialNumber: String,  // e.g. "MX01245874", "SN-8849201"
    val brand: String,         // e.g. "Ainsworth", "IGT", "Zitro", "EGT"
    val model: String,         // e.g. "A560H", "PEAK 49", "Altius Glare"
    val area: String,          // e.g. "FUMADORES", "NO FUMAR", "Sala Principal"
    val game: String = "",     // e.g. "Link King", "Wheel of Fortune"
    val island: String = "",   // e.g. "Isla 01"
    val sala: String = "",     // e.g. "Winpot Metrocentro", "Winpot Puerta de hierro"
    val qrId: String = "",     // e.g. "QR-0001"
    val propietario: String = "" // e.g. "WINPOT"
)
