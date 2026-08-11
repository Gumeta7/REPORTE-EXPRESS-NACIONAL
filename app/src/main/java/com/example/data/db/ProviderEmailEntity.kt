package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "provider_emails")
data class ProviderEmailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val providerName: String,
    val email: String
)
