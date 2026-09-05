package com.example.data.remote

data class IncidenciaTicketPayload(
    val idTicket: String,
    val sala: String,
    val marca: String,
    val modelo: String,
    val serie: String,
    val asset: String,
    val area: String,
    val propietario: String,
    val idTecnico: String,
    val tecnico: String,
    val falla: String,
    val operativa: String = "NO",
    val estadoTicket: String = "ABIERTO",
    val prioridad: String = "MEDIA"
) {
    fun toJsonString(): String {
        fun escape(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
        return """
        {
            "id_ticket": "${escape(idTicket)}",
            "sala": "${escape(sala)}",
            "marca": "${escape(marca)}",
            "modelo": "${escape(modelo)}",
            "serie": "${escape(serie)}",
            "asset": "${escape(asset)}",
            "area": "${escape(area)}",
            "propietario": "${escape(propietario)}",
            "operativa": "${escape(operativa)}",
            "estado_ticket": "${escape(estadoTicket)}",
            "falla": "${escape(falla)}",
            "prioridad": "${escape(prioridad)}",
            "id_tecnico": "${escape(idTecnico)}",
            "tecnico": "${escape(tecnico)}"
        }
        """.trimIndent()
    }
}
