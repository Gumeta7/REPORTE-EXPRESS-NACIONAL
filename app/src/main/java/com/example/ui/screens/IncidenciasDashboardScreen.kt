package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
    val availableSalas by viewModel.availableSalas.collectAsState()

    val isAdmin = currentUser?.isAdmin == true
    val activeSalaDisplay = if (isAdmin) {
        if (adminSelectedSala.isBlank() || adminSelectedSala.equals("TODAS", ignoreCase = true)) {
            "Todas las Salas (Nacional)"
        } else {
            adminSelectedSala
        }
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
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Incidencias de Sala",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeSalaDisplay,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                                .size(40.dp)
                                .testTag("refresh_dashboard_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refrescar Incidencias",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1.5. Selector de Sala para Administradores
        if (isAdmin && availableSalas.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Filtrar por Sala:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        availableSalas.forEach { sala ->
                            val isSelected = if (sala.equals("TODAS", ignoreCase = true)) {
                                adminSelectedSala.isBlank() || adminSelectedSala.equals("TODAS", ignoreCase = true)
                            } else {
                                adminSelectedSala.equals(sala, ignoreCase = true)
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setAdminSelectedSala(sala) },
                                label = {
                                    Text(
                                        text = sala,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
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
            // 3. Barra de Búsqueda Compacta
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateIncidenciasSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Buscar ticket, asset, serie, falla...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.updateIncidenciasSearchQuery("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("incidencias_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
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
                    label = { Text("TODOS ($totalCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                // PENDIENTES
                val isPendientes = selectedEstadoFilter.equals("PENDIENTES", ignoreCase = true) || selectedEstadoFilter.equals("PENDIENTE", ignoreCase = true)
                FilterChip(
                    selected = isPendientes,
                    onClick = { viewModel.updateSelectedIncidenciaEstadoFilter("PENDIENTES") },
                    label = { Text("PENDIENTES ($pendientes)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEA580C),
                        selectedLabelColor = Color.White
                    )
                )

                // RESUELTOS
                val isResueltos = selectedEstadoFilter.equals("RESUELTOS", ignoreCase = true) || selectedEstadoFilter.equals("RESUELTO", ignoreCase = true)
                FilterChip(
                    selected = isResueltos,
                    onClick = { viewModel.updateSelectedIncidenciaEstadoFilter("RESUELTOS") },
                    label = { Text("RESUELTOS ($resueltos)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
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
                                "No hay incidencias que coincidan con los filtros en $activeSalaDisplay."
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
    val isCritica = incidencia.prioridad.equals("CRITICA", ignoreCase = true) || incidencia.prioridad.equals("ALTA", ignoreCase = true)

    val estadoColor = when {
        isResuelto -> Color(0xFF16A34A) // Verde esmeralda
        incidencia.estadoTicket.contains("PROCESO", ignoreCase = true) -> Color(0xFF0284C7) // Azul
        else -> Color(0xFFEA580C) // Naranja
    }
    val estadoBadgeText = when {
        isResuelto -> "RESUELTO"
        incidencia.estadoTicket.contains("PROCESO", ignoreCase = true) -> "EN PROCESO"
        else -> "PENDIENTE"
    }

    val accentBorderColor = when {
        isResuelto -> Color(0xFF16A34A)
        isCritica -> Color(0xFFDC2626)
        else -> Color(0xFFEA580C)
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isResuelto) Color(0xFF86EFAC).copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Tira de acento lateral izquierda indicadora de estado
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentBorderColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. FILA SUPERIOR: ID de Ticket + Badge de Estado (Separados para que nunca se aplasten)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${incidencia.idTicket}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = estadoColor
                    ) {
                        Text(
                            text = estadoBadgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            softWrap = false,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                // 2. FILA DE INFORMACIÓN DEL EQUIPO Y ESTADO OPERATIVO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${incidencia.marca} • ${incidencia.modelo}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (incidencia.prioridad.isNotBlank()) {
                            val prioColor = when (incidencia.prioridad.uppercase()) {
                                "CRITICA", "ALTA" -> Color(0xFFDC2626)
                                "MEDIA" -> Color(0xFFEA580C)
                                else -> Color(0xFFEAB308)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = prioColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = incidencia.prioridad.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = prioColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (!isResuelto) {
                            val opText = if (isOperativaNo) "Inoperativa" else "Operativa"
                            val opColor = if (isOperativaNo) Color(0xFFDC2626) else Color(0xFF16A34A)
                            Text(
                                text = opText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = opColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 3. TAGS METADATA (Asset, Serie, Área, Sala)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetadataTag(label = "Asset", value = incidencia.asset)
                    MetadataTag(label = "Serie", value = incidencia.serie)
                    if (incidencia.area.isNotBlank()) {
                        MetadataTag(label = "Área", value = incidencia.area)
                    }
                    if (incidencia.sala.isNotBlank()) {
                        MetadataTag(label = "Sala", value = incidencia.sala)
                    }
                }

                // 4. DETALLE DE LA FALLA
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Falla Reportada:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = incidencia.falla.ifBlank { "Sin descripción detallada." },
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 5. SOLUCIÓN / RESOLUCIÓN APLICADA (Si existe)
                if (incidencia.resolucion.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Solución / Resolución Aplicada:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = incidencia.resolucion,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = Color(0xFF14532D)
                            )
                        }
                    }
                }

                // 6. PIE DE TARJETA: Técnico y Fechas
                if (incidencia.tecnico.isNotBlank() || displayDate.isNotBlank() || displayFechaReparacion.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (incidencia.tecnico.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = incidencia.tecnico,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val (dateLabel, dateVal) = when {
                            isResuelto && displayFechaReparacion.isNotBlank() -> Pair("Resuelto:", displayFechaReparacion)
                            displayDate.isNotBlank() -> Pair("Reportado:", displayDate)
                            else -> Pair("", "")
                        }

                        if (dateVal.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$dateLabel $dateVal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataTag(label: String, value: String) {
    if (value.isBlank()) return
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
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
