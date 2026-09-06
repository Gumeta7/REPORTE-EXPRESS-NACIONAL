package com.example.data.remote

data class IncidenciaItem(
    val idTicket: String,
    val sala: String,
    val marca: String,
    val modelo: String,
    val serie: String,
    val asset: String,
    val area: String,
    val propietario: String,
    val operativa: String = "NO",
    val estadoTicket: String = "ABIERTO",
    val fechaOrigen: String = "",
    val fechaReparacion: String = "",
    val falla: String = "",
    val prioridad: String = "MEDIA",
    val idTecnico: String = "",
    val tecnico: String = "",
    val resolucion: String = ""
)
