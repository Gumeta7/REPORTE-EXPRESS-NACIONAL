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
     * Genera el ID_Ticket con la nueva nomenclatura oficial:
     * [ID_SALA]-[NUMERO DE SERIE DEL EQUIPO]-[NUMERO CONSECUTIVO]
     * Ejemplo: PHIE-22/55243-1
     * El número consecutivo comienza en 1.
     */
    fun generateTicketId(salaName: String, serialNumber: String, consecutive: Int = 1): String {
        val idSala = getSalaPrefix(salaName)
        val cleanSerial = serialNumber.trim().ifBlank { "SN-PENDIENTE" }
        val validConsecutive = if (consecutive > 0) consecutive else 1
        return "$idSala-$cleanSerial-$validConsecutive"
    }

    /**
     * Extrae el número consecutivo de un folio con el formato [ID_SALA]-[SERIE]-[CONSECUTIVO].
     * Ignora folios con el formato antiguo [ID_SALA]-[YYYYMMDD]-[SERIE].
     */
    fun extractConsecutiveFromTicketId(ticketId: String): Int? {
        val trimmed = ticketId.trim()
        if (trimmed.isBlank()) return null

        val parts = trimmed.split("-")
        // Formato nuevo requiere al menos 3 partes: ID_SALA, SERIE y CONSECUTIVO
        if (parts.size < 3) return null

        // En el formato anterior [SALA]-[YYYYMMDD]-[SERIE], la segunda parte es una fecha de 8 dígitos que empieza con "20"
        if (parts.size == 3 && parts[1].length == 8 && parts[1].startsWith("20") && parts[1].all { it.isDigit() }) {
            return null
        }

        val lastPart = parts.lastOrNull() ?: return null
        return lastPart.toIntOrNull()
    }

    /**
     * Reemplaza o actualiza el número consecutivo de un ticket respetando sala y serie.
     * Ejemplo: "PHIE-22/55243-1", 7 -> "PHIE-22/55243-7"
     */
    fun updateConsecutiveInTicketId(originalTicketId: String, newConsecutive: Int): String {
        val trimmed = originalTicketId.trim()
        val parts = trimmed.split("-")
        return if (parts.size >= 3) {
            val prefix = parts.dropLast(1).joinToString("-")
            "$prefix-$newConsecutive"
        } else {
            trimmed
        }
    }

    /**
     * Valida si una sala tiene un nombre válido.
     */
    fun isSupportedSala(salaName: String): Boolean {
        return salaName.trim().isNotBlank()
    }
}
