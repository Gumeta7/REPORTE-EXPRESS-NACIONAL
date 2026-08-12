package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.MachineEntity
import com.example.ui.viewmodel.ReportViewModel

@Composable
fun MachineLocationScreen(
    viewModel: ReportViewModel,
) {
    val searchQuery by viewModel.locationSearchQuery.collectAsState()
    val machinesList by viewModel.machineCatalog.collectAsState()

    var selectedMachineForReport by remember { mutableStateOf<MachineEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("machine_location_screen")
    ) {
        // Search Header Input with Animated Floating Label
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateLocationQuery(it) },
            label = {
                Text(
                    text = "Buscar máquina",
                    maxLines = 1,
                    softWrap = false
                )
            },
            placeholder = {
                Text(
                    text = "Ej. 444, Zitro, Isla 03",
                    maxLines = 1,
                    softWrap = false
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar Máquina"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateLocationQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("machine_location_search_input"),
            singleLine = true,
            maxLines = 1,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Catalog Count Header
        Text(
            text = "Catálogo de Máquinas (${machinesList.size}):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (machinesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Sin resultados",
                        modifier = Modifier.height(48.dp).width(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No se encontraron máquinas con la búsqueda." else "El catálogo de máquinas está vacío. Adjunta un archivo Excel en la pestaña Extraer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = machinesList,
                    key = { it.id },
                    contentType = { "machine_card" }
                ) { machine ->
                    MachineLocationCard(
                        machine = machine,
                        onReportClick = {
                            selectedMachineForReport = machine
                        }
                    )
                }
            }
        }
    }

    if (selectedMachineForReport != null) {
        ReportMachineFailureDialog(
            machine = selectedMachineForReport!!,
            onDismiss = { selectedMachineForReport = null },
            onConfirm = { failureDescription ->
                viewModel.generateReportForMachine(selectedMachineForReport!!, failureDescription)
                selectedMachineForReport = null
            }
        )
    }
}

@Composable
fun MachineLocationCard(
    machine: MachineEntity,
    onReportClick: () -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBgColor = if (isDarkTheme) Color(0xFF1B2430) else MaterialTheme.colorScheme.surface
    val cardBorder = if (isDarkTheme) BorderStroke(1.dp, Color(0xFF2E3D52)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("machine_location_card_${machine.machineNumber}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Machine Number, Brand & Modelo Reporte (PP / PV) Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDarkTheme) Color(0xFF243242) else MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Máquina #${machine.machineNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (machine.brand.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = machine.brand,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // PP / PV MODELO REPORTE BADGE
                val upperModel = remember(machine.model) { machine.model.trim().uppercase() }
                val isPropia = remember(upperModel) { upperModel.contains("(PP)") || upperModel.contains(" PP") || upperModel.endsWith("PP") || upperModel.contains("PROPIA") }
                val isProveedor = remember(upperModel) { upperModel.contains("(PV)") || upperModel.contains(" PV") || upperModel.endsWith("PV") || upperModel.contains("PROVEEDOR") }

                val badgeText = remember(isPropia, isProveedor, machine.model) {
                    when {
                        isPropia -> "(PP) Propia"
                        isProveedor -> "(PV) Proveedor"
                        machine.model.isNotBlank() -> machine.model
                        else -> "(PP/PV) Sin tipo"
                    }
                }

                val badgeBg = when {
                    isPropia -> if (isDarkTheme) com.example.ui.theme.PropiaGreenBgDark else com.example.ui.theme.PropiaGreenBgLight
                    isProveedor -> if (isDarkTheme) com.example.ui.theme.ProveedorPurpleBgDark else com.example.ui.theme.ProveedorPurpleBgLight
                    else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                }
                val badgeFg = when {
                    isPropia -> if (isDarkTheme) com.example.ui.theme.PropiaGreenFgDark else com.example.ui.theme.PropiaGreenFgLight
                    isProveedor -> if (isDarkTheme) com.example.ui.theme.ProveedorPurpleFgDark else com.example.ui.theme.ProveedorPurpleFgLight
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated Modelo Reporte display row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modelo Reporte:",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme) Color(0xFF90A4AE) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = machine.model.ifBlank { "N/A" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Highlights: Area, Island, Game
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Area
                InfoHighlightChip(
                    icon = Icons.Default.Place,
                    title = "Área",
                    value = machine.area,
                    modifier = Modifier.weight(1f)
                )

                // Island
                InfoHighlightChip(
                    icon = Icons.Default.GridView,
                    title = "Isla",
                    value = machine.island,
                    modifier = Modifier.weight(1f)
                )

                // Game
                InfoHighlightChip(
                    icon = Icons.Default.Casino,
                    title = "Juego",
                    value = machine.game,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Details (Asset & Serial)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Asset: ${machine.assetNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme) Color(0xFFB0BEC5) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Serie: ${machine.serialNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme) Color(0xFFB0BEC5) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direct Quick Report Button
            Button(
                onClick = onReportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.ReportProblem, contentDescription = "Reportar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reportar Falla de esta Máquina")
            }
        }
    }
}

@Composable
fun InfoHighlightChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val chipBgColor = if (isDarkTheme) Color(0xFF263344) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val chipBorder = if (isDarkTheme) BorderStroke(1.dp, Color(0xFF38495E)) else null
    val titleColor = if (isDarkTheme) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
    val valueColor = if (isDarkTheme) Color(0xFFF0F6FC) else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = chipBgColor,
        border = chipBorder
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.height(14.dp).width(14.dp),
                    tint = titleColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value.ifBlank { "N/A" },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ReportMachineFailureDialog(
    machine: MachineEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var issueInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.ReportProblem,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Reportar Falla de Máquina",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Machine Details Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Máquina #${machine.machineNumber} - ${machine.brand}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Modelo: ${machine.model} | Serie: ${machine.serialNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Asset: ${machine.assetNumber} | Isla: ${machine.island} | Área: ${machine.area}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Describa el inconveniente o falla:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = issueInput,
                    onValueChange = { issueInput = it },
                    placeholder = { Text("Ej: Billetero no acepta billetes, pantalla táctil descalibrada, error de comunicación, etc.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_report_failure_input"),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(issueInput) },
                enabled = issueInput.isNotBlank(),
                modifier = Modifier.testTag("confirm_location_report_failure_button")
            ) {
                Text("Generar Vista Previa")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_location_report_failure_button")
            ) {
                Text("Cancelar")
            }
        }
    )
}
