package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.db.MachineEntity
import com.example.data.db.ProviderEmailEntity
import com.example.ui.components.ManageProvidersDialog
import com.example.ui.viewmodel.ReportViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickReportScreen(
    viewModel: ReportViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val allMachinesList by viewModel.allMachines.collectAsState()
    val userCatalog by viewModel.machineCatalog.collectAsState()
    val providerEmailsList by viewModel.providerEmails.collectAsState()

    // Multiple Selected Machines state
    val selectedMachines = remember { mutableStateListOf<MachineEntity>() }
    var assetSearchInput by remember { mutableStateOf("") }
    var failureDescriptionInput by remember { mutableStateOf("") }
    var selectedRecipient by remember { mutableStateOf("") }

    var showManageProvidersDialog by remember { mutableStateOf(false) }
    var showAssetPickerModal by remember { mutableStateOf(false) }
    var assetPickerSearchQuery by remember { mutableStateOf("") }

    // Auto-detect provider email from selected machines' brands
    val autoDetectedProviderEmail = remember(selectedMachines.toList(), providerEmailsList) {
        if (selectedMachines.isEmpty()) return@remember ""
        val uniqueBrands = selectedMachines.map { it.brand.trim().lowercase() }.distinct()
        for (b in uniqueBrands) {
            val p = providerEmailsList.find { provider ->
                val pName = provider.providerName.trim().lowercase()
                pName.isNotBlank() && (b.contains(pName) || pName.contains(b))
            }
            if (p != null && p.email.isNotBlank()) {
                return@remember p.email.trim()
            }
        }
        ""
    }

    val activeRecipient = if (selectedRecipient.isNotBlank()) selectedRecipient else autoDetectedProviderEmail

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("quick_report_screen")
    ) {
        // Hero Header
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
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Generar Reporte",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Generador de Reportes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Selecciona las máquinas por Asset y redacta la falla para generar el correo agrupado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 1. CASILLA PARA SELECCIONAR MÁQUINAS POR ASSET
        Text(
            text = "Máquinas a Reportar (Selección por Asset):",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Row with Asset Text Input & Picker Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = assetSearchInput,
                onValueChange = { assetSearchInput = it },
                label = { Text("Escribir Asset (Ej: 361)") },
                placeholder = { Text("Ej. 361") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Numbers,
                        contentDescription = "Asset"
                    )
                },
                trailingIcon = {
                    if (assetSearchInput.isNotEmpty()) {
                        IconButton(onClick = { assetSearchInput = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("asset_search_input"),
                shape = RoundedCornerShape(14.dp)
            )

            // Add button
            Button(
                onClick = {
                    val key = assetSearchInput.trim()
                    if (key.isNotBlank()) {
                        coroutineScope.launch {
                            val machine = viewModel.getMachineForAsset(key)
                                ?: userCatalog.find {
                                    it.assetNumber.trim().equals(key, ignoreCase = true) ||
                                    it.machineNumber.trim().equals(key, ignoreCase = true) ||
                                    it.serialNumber.trim().equals(key, ignoreCase = true)
                                }
                            if (machine != null) {
                                if (selectedMachines.none { it.id == machine.id }) {
                                    selectedMachines.add(machine)
                                }
                                assetSearchInput = ""
                            } else {
                                viewModel.saveVenueName(key) // fallback search notification
                            }
                        }
                    }
                },
                enabled = assetSearchInput.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(54.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Agregar")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Open Full Asset Selector Modal Button
        OutlinedButton(
            onClick = { showAssetPickerModal = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "Seleccionar de la lista",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Seleccionar de la lista del catálogo (${userCatalog.size} disponibles)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display Selected Machines Chips / Cards
        if (selectedMachines.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Máquinas seleccionadas (${selectedMachines.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { selectedMachines.clear() }
                        ) {
                            Text("Quitar todas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedMachines.forEach { m ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Asset: ${m.assetNumber.ifBlank { m.machineNumber }} · ${m.model} (${m.serialNumber})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    IconButton(
                                        onClick = { selectedMachines.remove(m) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Quitar",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ninguna máquina seleccionada. Escribe un Asset arriba o presiona 'Seleccionar de la lista'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. CAMPO: DESCRIPCIÓN DE LA FALLA
        Text(
            text = "Descripción de la falla:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = failureDescriptionInput,
            onValueChange = { failureDescriptionInput = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("failure_description_input"),
            placeholder = {
                Text("Describe la falla o inconveniente que presentan las máquinas seleccionadas (ej. Billetero traba billetes, pantalla táctil descalibrada, error de comunicación, botón de cobro pegado, etc.)")
            },
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 3. SELECCIÓN DE CORREO DEL PROVEEDOR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Correo del Proveedor:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = { showManageProvidersDialog = true },
                modifier = Modifier.testTag("manage_provider_emails_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Gestionar",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gestionar Correos", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            providerEmailsList.forEach { provider ->
                val isSelected = activeRecipient == provider.email
                AssistChip(
                    onClick = { selectedRecipient = provider.email },
                    label = { Text("${provider.providerName}: ${provider.email}") },
                    colors = if (isSelected) {
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        AssistChipDefaults.assistChipColors()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. BOTÓN GENERAR REPORTE
        Button(
            onClick = {
                if (selectedMachines.isNotEmpty() && failureDescriptionInput.isNotBlank()) {
                    viewModel.generateReportForMultipleMachines(
                        machines = selectedMachines.toList(),
                        issueDescription = failureDescriptionInput,
                        customRecipient = activeRecipient
                    )
                }
            },
            enabled = selectedMachines.isNotEmpty() && failureDescriptionInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("generate_quick_report_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Send, contentDescription = "Generar")
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (selectedMachines.size > 1) "Generar Reporte (${selectedMachines.size} Máquinas)" else "Generar y Revisar Correo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Modal Asset Multi-Selector Dialog
    if (showAssetPickerModal) {
        Dialog(onDismissRequest = { showAssetPickerModal = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seleccionar Máquinas por Asset",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showAssetPickerModal = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = assetPickerSearchQuery,
                        onValueChange = { assetPickerSearchQuery = it },
                        placeholder = { Text("Buscar por Asset, Serie, Modelo...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (assetPickerSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { assetPickerSearchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredList = remember(userCatalog, assetPickerSearchQuery) {
                        if (assetPickerSearchQuery.isBlank()) {
                            userCatalog
                        } else {
                            val q = assetPickerSearchQuery.trim().lowercase()
                            userCatalog.filter {
                                it.assetNumber.lowercase().contains(q) ||
                                it.serialNumber.lowercase().contains(q) ||
                                it.model.lowercase().contains(q) ||
                                it.brand.lowercase().contains(q) ||
                                it.area.lowercase().contains(q)
                            }
                        }
                    }

                    Text(
                        text = "${filteredList.size} máquinas encontradas (${selectedMachines.size} seleccionadas):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items = filteredList, key = { it.id }) { machine ->
                            val isChecked = selectedMachines.any { it.id == machine.id }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) {
                                            selectedMachines.removeAll { it.id == machine.id }
                                        } else {
                                            selectedMachines.add(machine)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (selectedMachines.none { it.id == machine.id }) {
                                                    selectedMachines.add(machine)
                                                }
                                            } else {
                                                selectedMachines.removeAll { it.id == machine.id }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Asset: ${machine.assetNumber.ifBlank { machine.machineNumber }} · ${machine.brand} - ${machine.model}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Serie: ${machine.serialNumber} | Área: ${machine.area}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showAssetPickerModal = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Aceptar Selección (${selectedMachines.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showManageProvidersDialog) {
        ManageProvidersDialog(
            providersList = providerEmailsList,
            onDismiss = { showManageProvidersDialog = false },
            onSaveProvider = { id, name, email ->
                viewModel.saveProviderEmail(id, name, email)
            },
            onDeleteProvider = { id ->
                viewModel.deleteProviderEmail(id)
            },
            onRestoreDefaults = {
                viewModel.restoreDefaultProviders()
            }
        )
    }
}
