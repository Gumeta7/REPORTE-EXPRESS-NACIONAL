package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.MachineEntity
import com.example.ui.viewmodel.ReportViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun MachineLocationScreen(
    viewModel: ReportViewModel
) {
    val context = LocalContext.current
    val searchQuery by viewModel.locationSearchQuery.collectAsState()
    val machinesList by viewModel.machineCatalog.collectAsState()

    var showConfirmClearDialog by remember { mutableStateOf(false) }
    var selectedMachineForReport by remember { mutableStateOf<MachineEntity?>(null) }

    // File picker to upload custom Excel/CSV dataset
    val excelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    viewModel.importMachinesFromStream(stream)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("machine_location_screen")
    ) {
        // Search Header Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateLocationQuery(it) },
            placeholder = { Text("¿Dónde está la máquina? (Ej. 444, Zitro, Isla 03)") },
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
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Import Dataset Action (Full Width Excel Import)
        Button(
            onClick = { excelPickerLauncher.launch("*/*") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("import_excel_catalog_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.FileUpload, contentDescription = "Excel")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Subir Catálogo Excel / CSV", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Column Format Help Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Columnas Excel",
                    modifier = Modifier.height(16.dp).width(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Columnas reconocidas: ASSET, MARCA, MODELO REPORTE (PP/PV), TITULO JUEGO, AREA, ISLA, SERIE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Catálogo de Máquinas (${machinesList.size}):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                onClick = { showConfirmClearDialog = true },
                modifier = Modifier.testTag("clear_machine_catalog_button")
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                    contentDescription = "Eliminar Base",
                    modifier = Modifier.height(16.dp).width(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Eliminar Base",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

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
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Sin resultados",
                        modifier = Modifier.height(48.dp).width(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No se encontraron máquinas con la búsqueda." else "El catálogo de máquinas está vacío. Sube un archivo Excel o CSV.",
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
                items(machinesList, key = { it.id }) { machine ->
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

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text("¿Eliminar Base de Datos del Catálogo?") },
            text = {
                Text(
                    "Esta acción borrará permanentemente las ${machinesList.size} máquinas del catálogo actual en la base de datos local."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearMachineCatalog()
                        showConfirmClearDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar Base")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmClearDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun MachineLocationCard(
    machine: MachineEntity,
    onReportClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("machine_location_card_${machine.machineNumber}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Machine Number, Brand & Modelo Reporte (PP / PV) Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Máquina #${machine.machineNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = machine.brand,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // PP / PV MODELO REPORTE BADGE
                val upperModel = machine.model.trim().uppercase()
                val isPropia = upperModel.contains("(PP)") || upperModel.contains(" PP") || upperModel.endsWith("PP") || upperModel.contains("PROPIA")
                val isProveedor = upperModel.contains("(PV)") || upperModel.contains(" PV") || upperModel.endsWith("PV") || upperModel.contains("PROVEEDOR")

                val badgeText = when {
                    isPropia -> "(PP) Propia"
                    isProveedor -> "(PV) Proveedor"
                    machine.model.isNotBlank() -> machine.model
                    else -> "(PP/PV) Sin tipo"
                }

                val badgeBg = when {
                    isPropia -> Color(0xFFE8F5E9) // Soft Green
                    isProveedor -> Color(0xFFEDE7F6) // Soft Purple
                    else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                }
                val badgeFg = when {
                    isPropia -> Color(0xFF1B5E20) // Dark Green
                    isProveedor -> Color(0xFF4A148C) // Dark Purple
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = machine.model.ifBlank { "N/A" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Step 6 Highlights: Area, Game, Island
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Serie: ${machine.serialNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.height(14.dp).width(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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

