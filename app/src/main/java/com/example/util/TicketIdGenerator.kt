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
            // Salas oficiales con códigos de tres letras definidos
            normalized.contains("METROCENTRO") || normalized.contains("MAJ") -> "MAJ"
            normalized.contains("PUERTA DE HIERRO") || normalized.contains("PDH") -> "PDH"
            normalized.contains("CIRCUNVALACION") || normalized.contains("CIR") -> "CIR"
            normalized.contains("SATELITE") || (normalized.contains("CAPRI") && normalized.contains("SAT")) -> "SAT"
            normalized.contains("DIAMONDS") || normalized.contains("DIAMOND") || normalized.contains("CORDILLERAS") || normalized.contains("DMS") -> "DMS"
            normalized.contains("VENETO") || normalized.contains("INTERLOMAS") || normalized.contains("VNT") -> "VNT"
            normalized.contains("TUXTLA") || normalized.contains("TUX") -> "TUX"
            normalized.contains("BOCA DEL RIO") || normalized.contains("BOCA") -> "BOC"
            normalized.contains("CARRANZA") -> "CAR"
            normalized.contains("GUAYMAS") -> "GYM"
            normalized.contains("MANDARIN") -> "MAN"
            normalized.contains("MERIDA") -> "MER"
            normalized.contains("METEPEC") -> "MET"
            normalized.contains("PACHUCA") -> "PAC"
            normalized.contains("PLAYA") -> "PLA"
            normalized.contains("POZA RICA") -> "PZR"
            normalized.contains("PUEBLA") -> "PUE"
            normalized.contains("PUNTO SUR") -> "PTS"
            normalized.contains("TONALA") -> "TON"
            normalized.contains("CORPORATIVO") -> "COR"
            else -> {
                // Generador dinámico para cualquier sala no catalogada previamente:
                // Toma las 3 primeras letras alfabéticas o "WIN" por defecto
                val lettersOnly = normalized.filter { it.isLetter() }
                if (lettersOnly.length >= 3) lettersOnly.take(3) else "WIN"
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
