package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "machines")
data class MachineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val machineNumber: String, // e.g. "444", "1025"
    val assetNumber: String,   // e.g. "AST-0444", "ASSET-1025"
    val serialNumber: String,  // e.g. "SN-8849201"
    val brand: String,         // e.g. "Zitro", "IGT", "Aristocrat", "Novomatic"
    val model: String,         // e.g. "Altius Glare", "Fusion", "Helix"
    val area: String,          // e.g. "Sala Principal", "Zona VIP", "Área B"
    val game: String,          // e.g. "Link King", "Wheel of Fortune", "Dragon Link"
    val island: String         // e.g. "Isla 05", "Isla B-12"
)
