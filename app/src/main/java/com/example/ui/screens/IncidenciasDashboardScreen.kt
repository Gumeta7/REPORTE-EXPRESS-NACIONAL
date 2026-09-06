package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.IncidenciaItem
import com.example.ui.viewmodel.ReportViewModel

@Composable
fun IncidenciasDashboardScreen(
    viewModel: ReportViewModel
) {
    val incidencias by viewModel.incidenciasList.collectAsState()
    val salaIncidencias by viewModel.salaIncidenciasList.collectAsState()
    val rawIncidencias by viewModel.rawIncidencias.collectAsState()
    val searchQuery by viewModel.incidenciasSearchQuery.collectAsState()
    val selectedEstadoFilter by viewModel.selectedIncidenciaEstadoFilter.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isSyncing by viewModel.isSyncingDrive.collectAsState()
    val adminSelectedSala by viewModel.adminSelectedSala.collectAsState()

    val isAdmin = currentUser?.isAdmin == true
    val activeSala = if (isAdmin && adminSelectedSala.isNotBlank() && !adminSelectedSala.equals("TODAS", ignoreCase = true)) {
        adminSelectedSala
    } else {
        currentUser?.sala?.ifBlank { "CORPORATIVO" } ?: "CORPORATIVO"
    }

    // Helper functions idénticas a la web (KPIHeader.jsx & index.jsx)
    fun isTicketPendiente(item: IncidenciaItem): Boolean {
        val st = item.estadoTicket.uppercase()
        return !st.contains("RESUELT") && !st.contains("CERRAD")
    }

    fun isTicketResuelto(item: IncidenciaItem): Boolean {
        val st = item.estadoTicket.uppercase()
        return st.contains("RESUELT") || st.contains("CERRAD")
    }

    // 6 Métricas KPI computadas sobre la sala activa (idénticas a la web)
    val totalCount = salaIncidencias.size
    val noOperativas = remember(salaIncidencias) {
        salaIncidencias.count { it.operativa.equals("NO", ignoreCase = true) && isTicketPendiente(it) }
    }
    val operativas = remember(salaIncidencias) {
        salaIncidencias.count { it.operativa.equals("SI", ignoreCase = true) && isTicketPendiente(it) }
    }
    val pendientes = remember(salaIncidencias) {
        salaIncidencias.count { isTicketPendiente(it) }
    }
    val resueltos = remember(salaIncidencias) {
        salaIncidencias.count { isTicketResuelto(it) }
    }
    val criticas = remember(salaIncidencias) {
        salaIncidencias.count { (it.prioridad.equals("CRITICA", true) || it.prioridad.equals("ALTA", true)) && isTicketPendiente(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("incidencias_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // 1. Header Informativo de la Sala
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Dashboard de Sala",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Incidencias de Sala",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeSala,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.syncFromDrive(showProgressMessage = true) },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("refresh_dashboard_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refrescar Incidencias",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // 2. Grid de 6 KPIs Dinámicos de la Sala (Estructura idéntica al KPIHeader web)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Fila 1: Total, Fuera de Servicio, En Servicio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiStatCard(
                        label = "Total",
                        count = totalCount,
                        icon = Icons.Default.Assessment,
                        color = Color(0xFF4F46E5), // Indigo
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateSelectedIncidenciaEstadoFilter("TODOS") }
                    )
                    KpiStatCard(
                        label = "Fuera Serv.",
                        count = noOperativas,
                        icon = Icons.Default.Error,
                        color = Color(0xFFDC2626), // Rojo
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateSelectedIncidenciaEstadoFilter("PENDIENTES") }
                    )
                    KpiStatCard(
                        label = "En Servicio",
                        count = operativas,
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF16A34A), // Verde
                        modifier = Modifier.weight(1f)
                    )
                }

                // Fila 2: Pendientes, Resueltos, Críticas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiStatCard(
                        label = "Pendientes",
                        count = pendientes,
                        icon = Icons.Default.HourglassTop,
                        color = Color(0xFFEA580C), // Naranja/Ámbar
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateSelectedIncidenciaEstadoFilter("PENDIENTES") }
                    )
                    KpiStatCard(
                        label = "Resueltos",
                        count = resueltos,
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF0284C7), // Azul Cielo
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateSelectedIncidenciaEstadoFilter("RESUELTOS") }
                    )
                    KpiStatCard(
                        label = "Críticas",
                        count = criticas,
                        icon = Icons.Default.Warning,
                        color = Color(0xFFB91C1C), // Guinda
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateSelectedIncidenciaEstadoFilter("PENDIENTES") }
                    )
                }
            }
        }

        item {
            // 3. Barra de Búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateIncidenciasSearchQuery(it) },
                label = { Text("Buscar por Ticket, Asset, Serie, Marca o Falla") },
                placeholder = { Text("Ej: WIN-001, 456, Touch...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateIncidenciasSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("incidencias_search_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        item {
            // 4. Filtros Rápidos por Estado (Organización idéntica a la web: TODOS, PENDIENTES, RESUELTOS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TODOS
                val isTodos = selectedEstadoFilter.equals("TODOS", ignoreCase = true)
                FilterChip(
                    selected = isTodos,
                    onClick = { viewModel.updateSelectedIncidenciaEstadoFilter("TODOS") },
                    label = { Text("TODOS ($totalCount)", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                // PENDIENTES (con indicador ámbar/naranja)
                val isPendientes = selectedEstadoFilter.equals("PENDIENTES", ignoreCase = true) || selectedEstadoFilter.equals("PENDIENTE", ignoreCase = true)
                FilterChip(
                    selected = isPendientes,
                    onClick = { viewModel.updateSelectedIncidenciaEstadoFilter("PENDIENTES") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFF59E0B), CircleShape)
                        )
                    },
                    label = { Text("PENDIENTES ($pendientes)", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEA580C),
                        selectedLabelColor = Color.White
                    )
                )

                // RESUELTOS (con indicador verde)
                val isResueltos = selectedEstadoFilter.equals("RESUELTOS", ignoreCase = true) || selectedEstadoFilter.equals("RESUELTO", ignoreCase = true)
                FilterChip(
                    selected = isResueltos,
                    onClick = { viewModel.updateSelectedIncidenciaEstadoFilter("RESUELTOS") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                    },
                    label = { Text("RESUELTOS ($resueltos)", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF16A34A),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // 5. Contenido / Lista de Incidencias
        if (incidencias.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Sin incidencias",
                            modifier = Modifier.size(54.dp),
                            tint = Color(0xFF16A34A).copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (rawIncidencias.isEmpty()) {
                                "No hay incidencias descargadas. Presiona actualizar."
                            } else {
                                "No hay incidencias que coincidan con los filtros en $activeSala."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(
                items = incidencias,
                key = { it.idTicket + "_" + it.asset + "_" + it.fechaOrigen }
            ) { incidencia ->
                IncidenciaTicketCard(incidencia = incidencia)
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Espacio final para que el último elemento no quede oculto bajo la barra inferior
        }
    }
}

@Composable
fun KpiStatCard(
    label: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Surface(
        modifier = clickableModifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun IncidenciaTicketCard(
    incidencia: IncidenciaItem
) {
    val isResuelto = incidencia.estadoTicket.contains("RESUELT", ignoreCase = true) || incidencia.estadoTicket.contains("CERRAD", ignoreCase = true)
    val isOperativaNo = incidencia.operativa.equals("NO", ignoreCase = true)

    val estadoBadgeColor = if (isResuelto) Color(0xFF16A34A) else Color(0xFFEA580C)
    val estadoBadgeText = if (isResuelto) {
        "RESUELTO"
    } else if (incidencia.estadoTicket.contains("PROCESO", ignoreCase = true)) {
        "EN PROCESO"
    } else {
        "PENDIENTE"
    }

    val displayDate = remember(incidencia.fechaOrigen) {
        formatExcelDate(incidencia.fechaOrigen)
    }

    val displayFechaReparacion = remember(incidencia.fechaReparacion) {
        formatExcelDate(incidencia.fechaReparacion)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("incidencia_card_${incidencia.idTicket}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isResuelto) Color(0xFF16A34A).copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isResuelto) 1.5.dp else 1.dp,
            color = if (isResuelto) Color(0xFF16A34A).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Fila superior: ID Ticket, Prioridad y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = incidencia.idTicket,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (incidencia.prioridad.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val prioColor = when (incidencia.prioridad.uppercase()) {
                            "CRITICA", "ALTA" -> Color(0xFFDC2626)
                            "MEDIA" -> Color(0xFFEA580C)
                            else -> Color(0xFFEAB308)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = prioColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = incidencia.prioridad,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = prioColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Badge de Estado (RESUELTO en verde, PENDIENTE en naranja/ámbar)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = estadoBadgeColor
                ) {
                    Text(
                        text = estadoBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Datos de la máquina y estatus operativo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${incidencia.marca} - ${incidencia.modelo}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isResuelto) {
                        "🟢 Atendido / Resuelto"
                    } else if (isOperativaNo) {
                        "🔴 Fuera de Servicio"
                    } else {
                        "🟢 Operativa"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (!isResuelto && isOperativaNo) Color(0xFFDC2626) else Color(0xFF16A34A)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Asset: ${incidencia.asset}  |  Serie: ${incidencia.serie}  |  Área: ${incidencia.area.ifBlank { "Sala" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Detalle de la Falla
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Falla Reportada:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = incidencia.falla.ifBlank { "Sin descripción detallada." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Solución o Resolución Aplicada (si existe)
            if (incidencia.resolucion.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF16A34A).copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Solución / Resolución Aplicada:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = incidencia.resolucion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Pie de tarjeta: Técnico, Fecha Origen y Fecha Reparación
            if (incidencia.tecnico.isNotBlank() || displayDate.isNotBlank() || displayFechaReparacion.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (incidencia.tecnico.isNotBlank()) {
                        Text(
                            text = "Por: ${incidencia.tecnico}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    val dateText = when {
                        isResuelto && displayFechaReparacion.isNotBlank() -> "Resuelto: $displayFechaReparacion"
                        displayDate.isNotBlank() -> displayDate
                        else -> ""
                    }
                    if (dateText.isNotBlank()) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

// Convierte números seriales de fecha de Excel (ej: 46270.91099) o textos a formato dd/MM/yyyy HH:mm
fun formatExcelDate(dateStr: String): String {
    val trimmed = dateStr.trim()
    if (trimmed.isBlank()) return ""
    try {
        val num = trimmed.toDoubleOrNull()
        if (num != null && num > 30000 && num < 60000) {
            // Número serial de fecha de Excel
            val millis = ((num - 25569) * 86400 * 1000).toLong()
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(millis))
        }
    } catch (_: Exception) {}
    return trimmed
}
