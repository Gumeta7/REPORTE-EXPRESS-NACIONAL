package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "email_reports")
data class EmailReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val recipient: String,
    val subject: String,
    val body: String,
    val machineNumber: String,
    val issueDescription: String,
    val brand: String = "",
    val model: String = "",
    val serialNumber: String = "",
    val assetNumber: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Enviado" // e.g., "Enviado", "Borrador"
)
