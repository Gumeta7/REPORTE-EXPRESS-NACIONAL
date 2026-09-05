package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TicketIdGenerator {

    /**
     * Mapeo de salas oficiales a sus prefijos de 3 letras:
     * - Winpot Metrocentro      -> MAJ
     * - Winpot Puerta de hierro -> PDH
     * - Capri Satelite          -> SAT
     * - Diamonds Guadalajara    -> DMS
     * - Veneto Interlomas       -> VNT
     * - Winpot Tuxtla           -> TUX
     */
    fun getSalaPrefix(salaName: String): String? {
        val normalized = salaName.trim().uppercase(Locale.getDefault())
            .replace("Á", "A")
            .replace("É", "E")
            .replace("Í", "I")
            .replace("Ó", "O")
            .replace("Ú", "U")

        return when {
            normalized.contains("METROCENTRO") || normalized.contains("MAJ") -> "MAJ"
            normalized.contains("PUERTA DE HIERRO") || normalized.contains("PDH") -> "PDH"
            normalized.contains("SATELITE") || normalized.contains("CAPRI") && normalized.contains("SAT") -> "SAT"
            normalized.contains("DIAMONDS") || normalized.contains("DIAMOND") -> "DMS"
            normalized.contains("VENETO") || normalized.contains("INTERLOMAS") || normalized.contains("VNT") -> "VNT"
            normalized.contains("TUXTLA") || normalized.contains("TUX") -> "TUX"
            else -> null
        }
    }

    /**
     * Genera el ID_Ticket con estructura: [PREFIJO]-[YYYYMMDD]-[SERIE]
     * Ejemplo: MAJ-20260904-MX01245874
     * Retorna null si la sala no corresponde a una de las 6 salas oficiales.
     */
    fun generateTicketId(salaName: String, serialNumber: String, date: Date = Date()): String? {
        val prefix = getSalaPrefix(salaName) ?: return null
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateString = dateFormat.format(date)
        val cleanSerial = serialNumber.trim().ifBlank { "SN-PENDIENTE" }
        return "$prefix-$dateString-$cleanSerial"
    }

    /**
     * Valida si una sala corresponde a las sedes soportadas para incidencias.
     */
    fun isSupportedSala(salaName: String): Boolean {
        return getSalaPrefix(salaName) != null
    }
}
