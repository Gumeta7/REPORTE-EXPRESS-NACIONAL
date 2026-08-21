package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.MachineEntity
import com.example.ui.viewmodel.ReportViewModel

@Composable
fun MachineLocationScreen(
    viewModel: ReportViewModel,
) {
    val searchQuery by viewModel.locationSearchQuery.collectAsState()
    val machinesList by viewModel.machineCatalog.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val distinctSalas by viewModel.distinctSalas.collectAsState()
    val distinctBrands by viewModel.distinctBrands.collectAsState()
    val selectedBrandFilter by viewModel.selectedBrandFilter.collectAsState()
    val adminSelectedSala by viewModel.adminSelectedSala.collectAsState()

    val isAdmin = currentUser?.isAdmin == true

    var selectedMachineForReport by remember { mutableStateOf<MachineEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("machine_location_screen")
    ) {
        // Search Header Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateLocationQuery(it) },
            label = {
                Text(
                    text = "Buscar por Sala, Marca, Modelo, Asset, Serie, Área o Propietario",
                    maxLines = 1,
                    softWrap = false
                )
            },
            placeholder = {
                Text(
                    text = "Ej. Winpot, A560H, 361, FUMADORES",
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

        // Admin Sala Selector Filter Chips
        if (isAdmin && distinctSalas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Seleccionar Sala (Vista Administrador):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isAllSelected = adminSelectedSala.isBlank() || adminSelectedSala.equals("TODAS", ignoreCase = true)
                FilterChip(
                    selected = isAllSelected,
                    onClick = { viewModel.setAdminSelectedSala("TODAS") },
                    label = { Text("Todas las Salas", style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = {
                        if (isAllSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        } else {
                            Icon(imageVector = Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                distinctSalas.forEach { sName ->
                    val isSelected = adminSelectedSala.trim().equals(sName.trim(), ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setAdminSelectedSala(sName) },
                        label = { Text(sName, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            } else {
                                Icon(imageVector = Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Brand Filter Chips Bar
        if (distinctBrands.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Filtrar por Marca:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                distinctBrands.forEach { bName ->
                    val isSelected = selectedBrandFilter.trim().equals(bName.trim(), ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateSelectedBrandFilter(bName) },
                        label = { Text(bName, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            } else {
                                Icon(imageVector = Icons.Default.Label, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Catalog Count Header with Dynamic Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val headerTitle = if (isAdmin && adminSelectedSala.isNotBlank() && !adminSelectedSala.equals("TODAS", ignoreCase = true)) {
                "Catálogo - $adminSelectedSala"
            } else {
                "Catálogo de Máquinas"
            }

            Text(
                text = headerTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${machinesList.size} máquinas",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedBrandFilter != "TODAS") "No se encontraron máquinas con los filtros aplicados." else "No hay máquinas registradas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    val isDarkTheme = remember(surfaceColor) { surfaceColor.luminance() < 0.5f }
    val cardBgColor = remember(isDarkTheme, surfaceColor) { if (isDarkTheme) Color(0xFF19222D) else surfaceColor }
    val cardBorder = remember(isDarkTheme, outlineVariantColor) {
        if (isDarkTheme) BorderStroke(1.dp, Color(0xFF2E3E50)) else BorderStroke(1.dp, outlineVariantColor.copy(alpha = 0.4f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("machine_location_card_${machine.machineNumber}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // 1. CABECERA: SALA & PROPIETARIO (Con leyenda clara 'Propietario:')
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sala
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Sala",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = machine.sala.ifBlank { "Sala General" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Propietario con etiqueta explícita
                if (machine.propietario.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Propietario",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Propietario: ${machine.propietario}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. CUADRÍCULA SIMÉTRICA 2x2: (Fila 1: Marca & Modelo | Fila 2: Asset & Área)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MachineGridBox(
                    icon = Icons.Default.Label,
                    label = "Marca",
                    value = machine.brand.ifBlank { "N/A" },
                    modifier = Modifier.weight(1f)
                )

                MachineGridBox(
                    icon = Icons.Default.Tune,
                    label = "Modelo",
                    value = machine.model.ifBlank { "N/A" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MachineGridBox(
                    icon = Icons.Default.Numbers,
                    label = "Asset",
                    value = machine.assetNumber.ifBlank { machine.machineNumber },
                    modifier = Modifier.weight(1f)
                )

                val areaAndIsland = buildString {
                    append(machine.area.ifBlank { "General" })
                    if (machine.island.isNotBlank()) {
                        append(" · Isla ${machine.island}")
                    }
                }

                MachineGridBox(
                    icon = Icons.Default.Place,
                    label = if (machine.island.isNotBlank()) "Área / Isla" else "Área",
                    value = areaAndIsland,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. BARRA SIMÉTRICA DE SERIE
            val boxBg = if (isDarkTheme) Color(0xFF222F3E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            val boxBorder = if (isDarkTheme) BorderStroke(1.dp, Color(0xFF324458)) else null

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = boxBg,
                border = boxBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Serie",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Número de Serie:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = machine.serialNumber.ifBlank { "N/A" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Action Button: Reportar Falla
            Button(
                onClick = onReportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReportProblem,
                    contentDescription = "Reportar Falla",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reportar Falla de esta Máquina",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MachineGridBox(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val boxBg = if (isDarkTheme) Color(0xFF222F3E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val boxBorder = if (isDarkTheme) BorderStroke(1.dp, Color(0xFF324458)) else null

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = boxBg,
        border = boxBorder
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
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
                // Machine Summary Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${machine.brand} - ${machine.model}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (machine.sala.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "• Sala: ${machine.sala}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "• Asset: ${machine.assetNumber} | Serie: ${machine.serialNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val propText = if (machine.propietario.isNotBlank()) " | Propietario: ${machine.propietario}" else ""
                        Text(
                            text = "• Área: ${machine.area}$propText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Describa la falla o inconveniente:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = issueInput,
                    onValueChange = { issueInput = it },
                    placeholder = { Text("Ej: Billetero traba billetes, touch descalibrado, error de comunicación, etc.") },
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
