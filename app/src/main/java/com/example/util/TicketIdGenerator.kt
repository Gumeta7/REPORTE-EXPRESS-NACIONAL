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
    fun getSalaPrefix(salaName: String): String {
        val normalized = salaName.trim().uppercase(Locale.getDefault())
            .replace("Á", "A")
            .replace("É", "E")
            .replace("Í", "I")
            .replace("Ó", "O")
            .replace("Ú", "U")

        return when {
            // Códigos oficiales ID_Sala definidos en el archivo:
            normalized == "CGDL" || normalized.contains("CORPORATIVO") -> "CGDL"
            normalized == "DOPE" || normalized.contains("DIRECTOR DE OPERACIONES") -> "DOPE"
            normalized == "PACH" || normalized.contains("PACHUCA") -> "PACH"
            normalized == "MERI" || normalized.contains("MERIDA") -> "MERI"
            normalized == "PLAY" || normalized.contains("PLAYA") -> "PLAY"
            normalized == "PUEB" || normalized.contains("PUEBLA") -> "PUEB"
            normalized == "CARR" || normalized.contains("CARRANZA") -> "CARR"
            normalized == "POZA" || normalized.contains("POZA RICA") -> "POZA"
            normalized == "TONA" || normalized.contains("TONALA") -> "TONA"
            normalized == "CORD" || normalized.contains("CORDILLERAS") || normalized.contains("DIAMOND") -> "CORD"
            normalized == "CIRC" || normalized.contains("CIRCUNVALACION") -> "CIRC"
            normalized == "METR" || normalized.contains("METROCENTRO") || normalized.contains("MAJ") -> "METR"
            normalized == "METE" || normalized.contains("METEPEC") -> "METE"
            normalized == "INTE" || normalized.contains("INTERLOMAS") || normalized.contains("VENETO") -> "INTE"
            normalized == "MAND" || normalized.contains("MANDARIN") -> "MAND"
            normalized == "SATE" || normalized.contains("SATELITE") -> "SATE"
            normalized == "GUAY" || normalized.contains("GUAYMAS") -> "GUAY"
            normalized == "BOCA" || normalized.contains("BOCA DEL RIO") -> "BOCA"
            normalized == "TUXT" || normalized.contains("TUXTLA") -> "TUXT"
            normalized == "PSUR" || normalized.contains("PUNTO SUR") -> "PSUR"
            normalized == "PHIE" || normalized.contains("PUERTA DE HIERRO") || normalized.contains("PDH") -> "PHIE"
            else -> {
                // Generador dinámico para cualquier sala no catalogada previamente:
                val lettersOnly = normalized.filter { it.isLetter() }
                if (lettersOnly.length >= 4) lettersOnly.take(4) else if (lettersOnly.length >= 3) lettersOnly.take(3) else "SALA"
            }
        }
    }

    /**
     * Genera el ID_Ticket con estructura: [PREFIJO]-[YYYYMMDD]-[SERIE]
     * Ejemplo: CIR-20260906-CAPRI111
     * Siempre retorna un ID válido para garantizar que la incidencia se registre en Google Sheets.
     */
    fun generateTicketId(salaName: String, serialNumber: String, date: Date = Date()): String {
        val prefix = getSalaPrefix(salaName)
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateString = dateFormat.format(date)
        val cleanSerial = serialNumber.trim().ifBlank { "SN-PENDIENTE" }
        return "$prefix-$dateString-$cleanSerial"
    }

    /**
     * Valida si una sala tiene un nombre válido.
     */
    fun isSupportedSala(salaName: String): Boolean {
        return salaName.trim().isNotBlank()
    }
}
