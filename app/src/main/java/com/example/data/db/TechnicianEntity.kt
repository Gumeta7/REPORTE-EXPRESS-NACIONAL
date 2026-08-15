package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "technicians")
data class TechnicianEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val technicianId: String = "", // e.g. "MAJ-12", "ADMIN01"
    val nombre: String = "",       // e.g. "Carlos Avalos", "Antonio Parra"
    val sala: String = "",         // e.g. "Winpot Metrocentro", "Corporativo GDL"
    val usuario: String = "",      // e.g. "cavalos", "aparra"
    val password: String = "",     // e.g. "cavalos$2026"
    val estatus: String = "ACTIVO",// e.g. "ACTIVO", "INACTIVO"
    val rol: String = "TECNICO"    // e.g. "TECNICO", "ADMIN", "ADMINISTRADOR"
) {
    val isAdmin: Boolean
        get() = rol.trim().uppercase() == "ADMIN" || rol.trim().uppercase() == "ADMINISTRADOR"
}
