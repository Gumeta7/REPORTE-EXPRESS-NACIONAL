package com.example.ui.screens

import androidx.compose.foundation.background
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

    // KPIs computados sobre la lista filtrada de la sala
    val totalCount = incidencias.size
    val abiertasCount = remember(incidencias) {
        incidencias.count { it.estadoTicket.contains("ABIERTO", ignoreCase = true) || it.estadoTicket.contains("PENDIENTE", ignoreCase = true) }
    }
    val procesoCount = remember(incidencias) {
        incidencias.count { it.estadoTicket.contains("PROCESO", ignoreCase = true) || it.estadoTicket.contains("ATENCION", ignoreCase = true) }
    }
    val cerradasCount = remember(incidencias) {
        incidencias.count { it.estadoTicket.contains("CERRAD", ignoreCase = true) || it.estadoTicket.contains("RESUELT", ignoreCase = true) }
    }
    val inoperativasCount = remember(incidencias) {
        incidencias.count { it.operativa.equals("NO", ignoreCase = true) }
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
            // 1. Header Informativo - Modo Solo Lectura
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Dashboard de Sala",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Incidencias de Sala",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Solo lectura",
                                            modifier = Modifier.size(11.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Solo Lectura",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ubicación: $activeSala",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.syncFromDrive(showProgressMessage = true) },
                        modifier = Modifier.testTag("refresh_dashboard_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refrescar Incidencias",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item {
            // 2. KPIs Dinámicos de la Sala
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiStatCard(
                    label = "Total",
                    count = totalCount,
                    icon = Icons.Default.Assessment,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                KpiStatCard(
                    label = "Abiertas",
                    count = abiertasCount,
                    icon = Icons.Default.Warning,
                    color = Color(0xFFDC2626), // Rojo
                    modifier = Modifier.weight(1f)
                )
                KpiStatCard(
                    label = "En Proceso",
                    count = procesoCount,
                    icon = Icons.Default.HourglassTop,
                    color = Color(0xFFEA580C), // Naranja
                    modifier = Modifier.weight(1f)
                )
                KpiStatCard(
                    label = "Fuera Serv.",
                    count = inoperativasCount,
                    icon = Icons.Default.Error,
                    color = Color(0xFF991B1B), // Guinda
                    modifier = Modifier.weight(1f)
                )
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
            // 4. Filtros Rápidos por Estado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val estados = listOf("TODOS", "ABIERTO", "EN PROCESO", "CERRADO")
                estados.forEach { est ->
                    val isSelected = selectedEstadoFilter.equals(est, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateSelectedIncidenciaEstadoFilter(est) },
                        label = { Text(est, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
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
    val isOperativaNo = incidencia.operativa.equals("NO", ignoreCase = true)
    val estadoBadgeColor = when {
        incidencia.estadoTicket.contains("ABIERTO", ignoreCase = true) || incidencia.estadoTicket.contains("PENDIENTE", ignoreCase = true) -> Color(0xFFDC2626)
        incidencia.estadoTicket.contains("PROCESO", ignoreCase = true) -> Color(0xFFEA580C)
        incidencia.estadoTicket.contains("CERRAD", ignoreCase = true) || incidencia.estadoTicket.contains("RESUELT", ignoreCase = true) -> Color(0xFF16A34A)
        else -> MaterialTheme.colorScheme.primary
    }

    val displayDate = remember(incidencia.fechaOrigen) {
        formatExcelDate(incidencia.fechaOrigen)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("incidencia_card_${incidencia.idTicket}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
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

                // Badge de Estado
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = estadoBadgeColor
                ) {
                    Text(
                        text = incidencia.estadoTicket,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Datos de la máquina
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${incidencia.marca} - ${incidencia.modelo}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isOperativaNo) "🔴 Fuera de Servicio" else "🟢 Operativa",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOperativaNo) Color(0xFFDC2626) else Color(0xFF16A34A)
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

            // Pie de tarjeta: Técnico y Fecha Origen
            if (incidencia.tecnico.isNotBlank() || displayDate.isNotBlank()) {
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
                    if (displayDate.isNotBlank()) {
                        Text(
                            text = displayDate,
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
