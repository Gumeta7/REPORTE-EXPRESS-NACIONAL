package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.IncidenciaItem
import com.example.ui.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    val isSuperUser = currentUser?.isSuperUser == true
    var selectedTicketForDetail by remember { mutableStateOf<IncidenciaItem?>(null) }
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
                            onClick = { viewModel.syncFromDrive(showProgressMessage = true, forceSyncMachines = true) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("refresh_dashboard_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Actualizar máquinas y reportes",
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
            // 2. Grid de 6 KPIs Dinámicos de la Sala (Compacto y sin iconos)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Fila 1: Fuera de servicio, Pendientes, En servicio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isFueraSelected = selectedEstadoFilter.equals("FUERA_SERVICIO", ignoreCase = true) || selectedEstadoFilter.equals("FUERA SERVICIO", ignoreCase = true)
                    KpiStatCard(
                        label = "Fuera Servicio",
                        count = noOperativas,
                        color = Color(0xFFDC2626), // Rojo
                        isSelected = isFueraSelected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isFueraSelected) {
                                viewModel.updateSelectedIncidenciaEstadoFilter("TODOS")
                            } else {
                                viewModel.updateSelectedIncidenciaEstadoFilter("FUERA_SERVICIO")
                            }
                        }
                    )
                    val isPendientesSelected = selectedEstadoFilter.equals("PENDIENTES", ignoreCase = true) || selectedEstadoFilter.equals("PENDIENTE", ignoreCase = true)
                    KpiStatCard(
                        label = "Pendientes",
                        count = pendientes,
                        color = Color(0xFFEA580C), // Naranja/Ámbar
                        isSelected = isPendientesSelected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isPendientesSelected) {
                                viewModel.updateSelectedIncidenciaEstadoFilter("TODOS")
                            } else {
                                viewModel.updateSelectedIncidenciaEstadoFilter("PENDIENTES")
                            }
                        }
                    )
                    val isEnServicioSelected = selectedEstadoFilter.equals("EN_SERVICIO", ignoreCase = true) || selectedEstadoFilter.equals("EN SERVICIO", ignoreCase = true)
                    KpiStatCard(
                        label = "En Servicio",
                        count = operativas,
                        color = Color(0xFF16A34A), // Verde
                        isSelected = isEnServicioSelected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isEnServicioSelected) {
                                viewModel.updateSelectedIncidenciaEstadoFilter("TODOS")
                            } else {
                                viewModel.updateSelectedIncidenciaEstadoFilter("EN_SERVICIO")
                            }
                        }
                    )
                }

                // Fila 2: Críticas, Resueltos, Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isCriticasSelected = selectedEstadoFilter.equals("CRITICAS", ignoreCase = true) || selectedEstadoFilter.equals("CRÍTICAS", ignoreCase = true)
                    KpiStatCard(
                        label = "Críticas",
                        count = criticas,
                        color = Color(0xFFB91C1C), // Guinda / Rojo oscuro
                        isSelected = isCriticasSelected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isCriticasSelected) {
                                viewModel.updateSelectedIncidenciaEstadoFilter("TODOS")
                            } else {
                                viewModel.updateSelectedIncidenciaEstadoFilter("CRITICAS")
                            }
                        }
                    )
                    val isResueltosSelected = selectedEstadoFilter.equals("RESUELTOS", ignoreCase = true) || selectedEstadoFilter.equals("RESUELTO", ignoreCase = true)
                    KpiStatCard(
                        label = "Resueltos",
                        count = resueltos,
                        color = Color(0xFF0284C7), // Azul Cielo
                        isSelected = isResueltosSelected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isResueltosSelected) {
                                viewModel.updateSelectedIncidenciaEstadoFilter("TODOS")
                            } else {
                                viewModel.updateSelectedIncidenciaEstadoFilter("RESUELTOS")
                            }
                        }
                    )
                    val isTotalSelected = selectedEstadoFilter.equals("TODOS", ignoreCase = true) || selectedEstadoFilter.equals("TOTAL", ignoreCase = true)
                    KpiStatCard(
                        label = "Total",
                        count = totalCount,
                        color = Color(0xFF4F46E5), // Indigo
                        isSelected = isTotalSelected,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateSelectedIncidenciaEstadoFilter("TODOS") }
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
                val isTodos = selectedEstadoFilter.equals("TODOS", ignoreCase = true) || selectedEstadoFilter.equals("TOTAL", ignoreCase = true)
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

                // FUERA DE SERVICIO
                val isFuera = selectedEstadoFilter.equals("FUERA_SERVICIO", ignoreCase = true) || selectedEstadoFilter.equals("FUERA SERVICIO", ignoreCase = true)
                FilterChip(
                    selected = isFuera,
                    onClick = { viewModel.updateSelectedIncidenciaEstadoFilter(if (isFuera) "TODOS" else "FUERA_SERVICIO") },
                    label = { Text("FUERA SERVICIO ($noOperativas)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFDC2626),
                        selectedLabelColor = Color.White
                    )
                )

                // PENDIENTES
                val isPendientes = selectedEstadoFilter.equals("PENDIENTES", ignoreCase = true) || selectedEstadoFilter.equals("PENDIENTE", ignoreCase = true)
                FilterChip(
                    selected = isPendientes,
                    onClick = { viewModel.updateSelectedIncidenciaEstadoFilter(if (isPendientes) "TODOS" else "PENDIENTES") },
                    label = { Text("PENDIENTES ($pendientes)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEA580C),
                        selectedLabelColor = Color.White
                    )
                )

                // EN SERVICIO
                val isEnServicio = selectedEstadoFilter.equals("EN_SERVICIO", ignoreCase = true) || selectedEstadoFilter.equals("EN SERVICIO", ignoreCase = true)
                FilterChip(
                    selected = isEnServicio,
                    onClick = { viewModel.updateSelectedIncidenciaEstadoFilter(if (isEnServicio) "TODOS" else "EN_SERVICIO") },
                    label = { Text("EN SERVICIO ($operativas)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF16A34A),
                        selectedLabelColor = Color.White
                    )
                )

                // CRÍTICAS
                val isCriticas = selectedEstadoFilter.equals("CRITICAS", ignoreCase = true) || selectedEstadoFilter.equals("CRÍTICAS", ignoreCase = true)
                FilterChip(
                    selected = isCriticas,
                    onClick = { viewModel.updateSelectedIncidenciaEstadoFilter(if (isCriticas) "TODOS" else "CRITICAS") },
                    label = { Text("CRÍTICAS ($criticas)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFB91C1C),
                        selectedLabelColor = Color.White
                    )
                )

                // RESUELTOS
                val isResueltos = selectedEstadoFilter.equals("RESUELTOS", ignoreCase = true) || selectedEstadoFilter.equals("RESUELTO", ignoreCase = true)
                FilterChip(
                    selected = isResueltos,
                    onClick = { viewModel.updateSelectedIncidenciaEstadoFilter(if (isResueltos) "TODOS" else "RESUELTOS") },
                    label = { Text("RESUELTOS ($resueltos)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7),
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
                IncidenciaTicketCard(
                    incidencia = incidencia,
                    onClick = { selectedTicketForDetail = incidencia }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Espacio final para que el último elemento no quede oculto bajo la barra inferior
        }
    }

    // Modal de Detalle y Gestión de Estatus
    selectedTicketForDetail?.let { ticket ->
        IncidenciaDetailDialog(
            incidencia = ticket,
            isSuperUser = isSuperUser,
            onDismiss = { selectedTicketForDetail = null },
            onUpdateStatus = { idTicket, nuevoEstado, operativa, resolucion, fechaReparacion, onComplete ->
                viewModel.updateIncidenciaStatus(
                    idTicket = idTicket,
                    nuevoEstado = nuevoEstado,
                    operativa = operativa,
                    resolucion = resolucion,
                    fechaReparacion = fechaReparacion,
                    onComplete = onComplete
                )
            }
        )
    }
}

@Composable
fun KpiStatCard(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Surface(
        modifier = clickableModifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.22f) else color.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else color.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    lineHeight = 20.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                ),
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun IncidenciaTicketCard(
    incidencia: IncidenciaItem,
    onClick: () -> Unit = {}
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
            .clickable { onClick() }
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
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Text(
                        text = incidencia.idTicket,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable {
                                com.example.util.EmailIntentUtil.copyToClipboard(
                                    context,
                                    "Número de Reporte",
                                    incidencia.idTicket
                                )
                            }
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

@Composable
fun IncidenciaDetailDialog(
    incidencia: IncidenciaItem,
    isSuperUser: Boolean,
    onDismiss: () -> Unit,
    onUpdateStatus: (String, String, String, String, String, (Boolean, String) -> Unit) -> Unit
) {
    var currentTicket by remember(incidencia) { mutableStateOf(incidencia) }

    val initialStatus = if (currentTicket.estadoTicket.contains("RESUELT", true) || currentTicket.estadoTicket.contains("CERRAD", true)) "RESUELTO" else "PENDIENTE"
    var editStatus by remember(currentTicket) { mutableStateOf(initialStatus) }
    var editOperativa by remember(currentTicket) {
        mutableStateOf(currentTicket.operativa.ifBlank { if (initialStatus == "RESUELTO") "SI" else "NO" })
    }
    var editResolucion by remember(currentTicket) { mutableStateOf(currentTicket.resolucion) }

    val defaultToday = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    var editFechaReparacion by remember(currentTicket) {
        val f = currentTicket.fechaReparacion.trim()
        val parsed = if (f.isNotBlank()) formatExcelDate(f).split(" ")[0] else if (initialStatus == "RESUELTO") defaultToday else ""
        mutableStateOf(parsed)
    }

    var isUpdating by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf<String?>(null) }
    var isUpdateSuccess by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Dialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Modal Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Detalle del Reporte",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentTicket.idTicket,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        com.example.util.EmailIntentUtil.copyToClipboard(
                                            context,
                                            "Número de Reporte",
                                            currentTicket.idTicket
                                        )
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar Ticket",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Estado badge
                        val isCurrResuelto = currentTicket.estadoTicket.contains("RESUELT", true) || currentTicket.estadoTicket.contains("CERRAD", true)
                        val currBadgeColor = if (isCurrResuelto) Color(0xFF16A34A) else Color(0xFFEA580C)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = currBadgeColor
                        ) {
                            Text(
                                text = if (isCurrResuelto) "RESUELTO" else "PENDIENTE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Sección 1: Información del Equipo
                    DetailSectionCard(title = "Información del Equipo", icon = Icons.Default.Info) {
                        DetailRowItem(label = "Sala", value = currentTicket.sala)
                        DetailRowItem(label = "Área", value = currentTicket.area)
                        DetailRowItem(label = "Marca", value = currentTicket.marca)
                        DetailRowItem(label = "Modelo", value = currentTicket.modelo)
                        DetailRowItem(label = "Número de Serie", value = currentTicket.serie)
                        DetailRowItem(label = "Asset Number", value = currentTicket.asset)
                        DetailRowItem(label = "Propietario", value = currentTicket.propietario.ifBlank { "WINPOT" })
                    }

                    // Sección 2: Datos del Registro
                    DetailSectionCard(title = "Datos del Registro", icon = Icons.Default.Person) {
                        DetailRowItem(label = "Técnico Responsable", value = currentTicket.tecnico)
                        DetailRowItem(label = "ID Técnico", value = currentTicket.idTecnico)
                        DetailRowItem(label = "Fecha de Reporte", value = formatExcelDate(currentTicket.fechaOrigen))
                        DetailRowItem(
                            label = "Fecha Reparación",
                            value = if (currentTicket.fechaReparacion.isNotBlank()) formatExcelDate(currentTicket.fechaReparacion) else "Pendiente"
                        )
                    }

                    // Sección 3: Falla Reportada
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Falla Reportada:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentTicket.falla.ifBlank { "Sin descripción detallada." },
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Sección 4: Gestión para SUPERUSER o Resolución solo lectura
                    if (isSuperUser) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Gestión de Estatus",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (updateMsg != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isUpdateSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isUpdateSuccess) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isUpdateSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                                contentDescription = null,
                                                tint = if (isUpdateSuccess) Color(0xFF16A34A) else Color(0xFFDC2626),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = updateMsg ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUpdateSuccess) Color(0xFF14532D) else Color(0xFF991B1B)
                                            )
                                        }
                                    }
                                }

                                // Selector Estado: PENDIENTE / RESUELTO
                                Text(
                                    text = "Estado del Ticket:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isPendienteSelected = editStatus == "PENDIENTE"
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(enabled = !isUpdating) {
                                                editStatus = "PENDIENTE"
                                                editOperativa = "NO"
                                                editFechaReparacion = ""
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPendienteSelected) Color(0xFFEA580C) else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isPendienteSelected) Color(0xFFEA580C) else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Text(
                                            text = "PENDIENTE",
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isPendienteSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    val isResueltoSelected = editStatus == "RESUELTO"
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(enabled = !isUpdating) {
                                                editStatus = "RESUELTO"
                                                editOperativa = "SI"
                                                if (editFechaReparacion.isBlank()) {
                                                    editFechaReparacion = defaultToday
                                                }
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isResueltoSelected) Color(0xFF16A34A) else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isResueltoSelected) Color(0xFF16A34A) else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Text(
                                            text = "RESUELTO",
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isResueltoSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // Selector ¿Máquina Operativa?: SI / NO
                                Text(
                                    text = "¿Máquina Operativa?:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isOpSi = editOperativa.equals("SI", true)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(enabled = !isUpdating) {
                                                editOperativa = "SI"
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isOpSi) Color(0xFF16A34A).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isOpSi) Color(0xFF16A34A) else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Text(
                                            text = "SÍ (Operativa)",
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isOpSi) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isOpSi) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    val isOpNo = editOperativa.equals("NO", true)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(enabled = !isUpdating) {
                                                editOperativa = "NO"
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isOpNo) Color(0xFFDC2626).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isOpNo) Color(0xFFDC2626) else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Text(
                                            text = "NO (Inoperativa)",
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isOpNo) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isOpNo) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // Fecha de Reparación + Botón HOY
                                Text(
                                    text = "Fecha de Reparación:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isDateEnabled = editStatus == "RESUELTO" && !isUpdating

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(enabled = isDateEnabled) {
                                                val cal = Calendar.getInstance()
                                                val parts = editFechaReparacion.split("/")
                                                val initYear = if (parts.size == 3) parts[2].toIntOrNull() ?: cal.get(Calendar.YEAR) else cal.get(Calendar.YEAR)
                                                val initMonth = if (parts.size == 3) (parts[1].toIntOrNull()?.minus(1)) ?: cal.get(Calendar.MONTH) else cal.get(Calendar.MONTH)
                                                val initDay = if (parts.size == 3) parts[0].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH) else cal.get(Calendar.DAY_OF_MONTH)

                                                android.app.DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        editFechaReparacion = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y)
                                                    },
                                                    initYear,
                                                    initMonth,
                                                    initDay
                                                ).show()
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDateEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = "Calendario",
                                                tint = if (isDateEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (editFechaReparacion.isNotBlank()) editFechaReparacion else "Seleccionar fecha...",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                                color = if (editFechaReparacion.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { editFechaReparacion = defaultToday },
                                        enabled = isDateEnabled,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Text("HOY", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    }
                                }

                                // Resolución / Notas Técnicas
                                Text(
                                    text = "Resolución / Notas Técnicas:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = editResolucion,
                                    onValueChange = { editResolucion = it },
                                    enabled = !isUpdating,
                                    placeholder = {
                                        Text(
                                            "Describa la solución aplicada o notas de seguimiento...",
                                            fontSize = 12.sp
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                // Botón Actualizar
                                Button(
                                    onClick = {
                                        isUpdating = true
                                        updateMsg = null
                                        onUpdateStatus(
                                            currentTicket.idTicket,
                                            editStatus,
                                            editOperativa,
                                            editResolucion,
                                            if (editStatus == "RESUELTO") editFechaReparacion else ""
                                        ) { success, msg ->
                                            isUpdating = false
                                            isUpdateSuccess = success
                                            updateMsg = msg
                                            if (success) {
                                                currentTicket = currentTicket.copy(
                                                    estadoTicket = editStatus,
                                                    operativa = editOperativa,
                                                    resolucion = editResolucion,
                                                    fechaReparacion = if (editStatus == "RESUELTO") editFechaReparacion else ""
                                                )
                                            }
                                        }
                                    },
                                    enabled = !isUpdating,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    if (isUpdating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Guardando en Base de Datos...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    } else {
                                        Text("Actualizar Estatus", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else if (currentTicket.resolucion.isNotBlank()) {
                        // Resolución solo lectura para rol Técnico / Admin no-superuser
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Resolución Aplicada:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentTicket.resolucion,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = Color(0xFF14532D)
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
private fun DetailSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            content()
        }
    }
}

@Composable
private fun DetailRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifBlank { "N/A" },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

