package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "technicians")
data class TechnicianEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val technicianId: String = "", // e.g. "MAJ-12", "ADMIN01"
    val nombre: String = "",       // e.g. "Carlos Avalos", "Antonio Parra"
    val idSala: String = "",       // e.g. "CGDL", "DOPE", "CIRC", "METR"
    val sala: String = "",         // e.g. "Winpot Metrocentro", "Corporativo GDL"
    val usuario: String = "",      // e.g. "cavalos", "aparra"
    val password: String = "",     // e.g. "cavalos$2026"
    val estatus: String = "ACTIVO",// e.g. "ACTIVO", "INACTIVO"
    val rol: String = "TECNICO"    // e.g. "TECNICO", "ADMIN", "ADMINISTRADOR", "DIRECTOR"
) {
    val isSuperUser: Boolean
        get() {
            val r = rol.trim().uppercase()
            return r == "SUPERUSER" || r == "SUPERUSUARIO"
        }

    val isDirector: Boolean
        get() {
            val r = rol.trim().uppercase()
            val s = sala.trim().uppercase()
            val id = idSala.trim().uppercase()
            return r == "DIRECTOR" || r == "DIRECTORA" || r.contains("DIRECTOR") || s.contains("DIRECTOR") || id == "DOPE"
        }

    val isCorporativo: Boolean
        get() {
            val s = sala.trim().uppercase()
            val id = idSala.trim().uppercase()
            val r = rol.trim().uppercase()
            return s.contains("CORPORATIVO") || id == "CGDL" || r.contains("CORPORATIVO")
        }

    val isAdmin: Boolean
        get() {
            val r = rol.trim().uppercase()
            return r == "ADMIN" || r == "ADMINISTRADOR" || r == "ADMINISTRADORA" || isDirector || isSuperUser || isCorporativo
        }

    val canViewAllSalas: Boolean
        get() = isAdmin || isDirector || isSuperUser || isCorporativo

    val rolDisplay: String
        get() = when {
            isSuperUser -> "Superusuario"
            isDirector -> "Director de Operaciones"
            isCorporativo -> "Corporativo GDL"
            isAdmin -> "Administrador Corporativo"
            else -> "Técnico"
        }
}
