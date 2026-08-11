package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.MachineEntity
import com.example.ui.components.ManageProvidersDialog
import com.example.ui.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisitsScreen(
    viewModel: ReportViewModel
) {
    val context = LocalContext.current
    val storedVenueName by viewModel.venueName.collectAsState()
    val providerEmailsList by viewModel.providerEmails.collectAsState()
    val allMachinesList by viewModel.allMachines.collectAsState()

    var showVenueDialog by remember { mutableStateOf(storedVenueName.isBlank()) }
    var venueInput by remember { mutableStateOf(storedVenueName) }
    var showManageProvidersDialog by remember { mutableStateOf(false) }
    var showAssetGridDialog by remember { mutableStateOf(false) }
    var showIslaGridDialog by remember { mutableStateOf(false) }

    // Form fields from ViewModel (persisted across tab changes)
    val fecha by viewModel.visitFecha.collectAsState()
    val proveedor by viewModel.visitProveedor.collectAsState()
    val tecnico by viewModel.visitTecnico.collectAsState()
    val horaEntrada by viewModel.visitHoraEntrada.collectAsState()
    val horaSalida by viewModel.visitHoraSalida.collectAsState()
    val motivoVisita by viewModel.visitMotivoVisita.collectAsState()
    val assetInput by viewModel.visitAssetInput.collectAsState()
    val islaInput by viewModel.visitIslaInput.collectAsState()

    // Synchronize venueInput when storedVenueName changes
    LaunchedEffect(storedVenueName) {
        if (storedVenueName.isNotBlank()) {
            venueInput = storedVenueName
            showVenueDialog = false
        } else {
            showVenueDialog = true
        }
    }

    // Auto-lookup island from asset input against local database
    LaunchedEffect(assetInput) {
        val trimmedAsset = assetInput.trim()
        if (trimmedAsset.isNotBlank()) {
            val machine = viewModel.getMachineForAsset(trimmedAsset)
            if (machine != null && machine.island.isNotBlank()) {
                viewModel.updateVisitIslaInput(machine.island)
            }
        }
    }

    // Build formatted WhatsApp text
    val whatsappText = remember(storedVenueName, fecha, proveedor, tecnico, horaEntrada, horaSalida, motivoVisita, assetInput, islaInput) {
        val sb = StringBuilder()
        sb.append("*${storedVenueName.ifBlank { "SALA REGISTRADA" }.uppercase()}*\n")
        sb.append("*Fecha:* $fecha\n")
        sb.append("*Proveedor:* ${proveedor.uppercase()}\n")
        sb.append("*Técnico:* ${tecnico.uppercase()}\n")
        sb.append("*Hora de entrada:* $horaEntrada\n")
        if (horaSalida.isNotBlank()) {
            sb.append("*Hora de salida:* ${horaSalida.trim()}\n")
        }
        sb.append("*Motivo de visita:* $motivoVisita")
        if (assetInput.isNotBlank()) {
            sb.append("\n*Asset:* ${assetInput.trim()}")
        }
        if (islaInput.isNotBlank()) {
            sb.append("\n*Isla:* ${islaInput.trim()}")
        }
        sb.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("visits_screen_column")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Venue Name Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("venue_name_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = "Sala",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "Sala / Casino:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = storedVenueName.ifBlank { "Presiona para configurar" }.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                IconButton(
                    onClick = {
                        venueInput = storedVenueName
                        showVenueDialog = true
                    },
                    modifier = Modifier.testTag("edit_venue_name_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Cambiar Sala",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form Title
        Text(
            text = "Registro de Visita Técnica",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Date and Entry Time Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = fecha,
                onValueChange = { viewModel.updateVisitFecha(it) },
                label = { Text("Fecha") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("visit_date_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = horaEntrada,
                onValueChange = { viewModel.updateVisitHoraEntrada(it) },
                label = { Text("Hora de entrada") },
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("visit_time_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Departure Time (Optional)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = horaSalida,
                onValueChange = { viewModel.updateVisitHoraSalida(it) },
                label = { Text("Hora de salida (Opcional)") },
                placeholder = { Text("Ej: 18:30") },
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                trailingIcon = {
                    if (horaSalida.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateVisitHoraSalida("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar hora de salida")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("visit_departure_time_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedButton(
                onClick = {
                    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    viewModel.updateVisitHoraSalida(currentTime)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("now_departure_time_button")
            ) {
                Text("Ahora", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Provider Row with Manage button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Proveedor:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = { showManageProvidersDialog = true },
                modifier = Modifier.testTag("manage_providers_visits_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Gestionar",
                    modifier = Modifier.height(16.dp).width(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gestionar Proveedores", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Provider Choice Input
        OutlinedTextField(
            value = proveedor,
            onValueChange = { viewModel.updateVisitProveedor(it) },
            label = { Text("Nombre del Proveedor") },
            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("visit_provider_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Provider quick chips from database
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            providerEmailsList.forEach { provider ->
                val nameUpper = provider.providerName.uppercase()
                AssistChip(
                    onClick = { viewModel.updateVisitProveedor(nameUpper) },
                    label = { Text(nameUpper, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Technician Input
        OutlinedTextField(
            value = tecnico,
            onValueChange = { viewModel.updateVisitTecnico(it) },
            label = { Text("Técnico(s)") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("visit_technician_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Reason for Visit Input
        OutlinedTextField(
            value = motivoVisita,
            onValueChange = { viewModel.updateVisitMotivoVisita(it) },
            label = { Text("Motivo de visita") },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("visit_reason_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Action row for multi-selection buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showAssetGridDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("select_assets_grid_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Seleccionar Asset(s)", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = { showIslaGridDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("select_islands_grid_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Seleccionar Isla(s)", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Asset (Optional) and Island Row with Grid search buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = assetInput,
                onValueChange = { viewModel.updateVisitAssetInput(it) },
                label = { Text("Asset(s)") },
                placeholder = { Text("Ej: 515, 516") },
                trailingIcon = {
                    IconButton(onClick = { showAssetGridDialog = true }) {
                        Icon(Icons.Default.GridView, contentDescription = "Buscar Assets")
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("visit_asset_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = islaInput,
                onValueChange = { viewModel.updateVisitIslaInput(it) },
                label = { Text("Isla(s)") },
                placeholder = { Text("Ej: P, AM") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showIslaGridDialog = true }) {
                        Icon(Icons.Default.GridView, contentDescription = "Buscar Islas")
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("visit_island_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Formatted Preview Title
        Text(
            text = "Vista Previa Formato WhatsApp",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Preview Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("whatsapp_preview_card"),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = whatsappText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Registro Visita WhatsApp", whatsappText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Texto copiado para WhatsApp", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("copy_whatsapp_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copiar", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, whatsappText)
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(whatsappIntent)
                    } catch (e: Exception) {
                        val shareIntent = Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, whatsappText)
                            },
                            "Enviar por WhatsApp"
                        )
                        context.startActivity(shareIntent)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("send_whatsapp_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // Official WhatsApp Green
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "WhatsApp", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                viewModel.updateVisitFecha(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
                viewModel.updateVisitHoraEntrada(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
                viewModel.updateVisitHoraSalida("")
                viewModel.updateVisitAssetInput("")
                viewModel.updateVisitIslaInput("")
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_form_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Limpiar Campos de Visita")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialog for Initial or Edit Venue Name
    if (showVenueDialog) {
        AlertDialog(
            onDismissRequest = {
                if (storedVenueName.isNotBlank()) {
                    showVenueDialog = false
                }
            },
            title = {
                Text(
                    text = "Nombre de la Sala / Casino",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Ingresa el nombre de la sala para mantenerlo fijo en tus reportes de visitas.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = venueInput,
                        onValueChange = { venueInput = it },
                        label = { Text("Nombre de Sala") },
                        placeholder = { Text("Ej: WINPOT PUERTA DE HIERRO") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("venue_name_dialog_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (venueInput.isNotBlank()) {
                            viewModel.saveVenueName(venueInput)
                            showVenueDialog = false
                        }
                    },
                    enabled = venueInput.isNotBlank(),
                    modifier = Modifier.testTag("save_venue_name_button")
                ) {
                    Text("Guardar Sala")
                }
            },
            dismissButton = {
                if (storedVenueName.isNotBlank()) {
                    TextButton(onClick = { showVenueDialog = false }) {
                        Text("Cancelar")
                    }
                }
            }
        )
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

    if (showAssetGridDialog) {
        AssetSelectionGridDialog(
            allMachines = allMachinesList,
            currentAssetInput = assetInput,
            onDismiss = { showAssetGridDialog = false },
            onConfirm = { selectedAssets, detectedIslands ->
                if (selectedAssets.isNotEmpty()) {
                    viewModel.updateVisitAssetInput(selectedAssets.joinToString(", "))
                }
                if (detectedIslands.isNotEmpty()) {
                    viewModel.updateVisitIslaInput(detectedIslands.joinToString(", "))
                }
                showAssetGridDialog = false
            }
        )
    }

    if (showIslaGridDialog) {
        IslaSelectionGridDialog(
            allMachines = allMachinesList,
            currentIslaInput = islaInput,
            onDismiss = { showIslaGridDialog = false },
            onConfirm = { selectedIslands ->
                if (selectedIslands.isNotEmpty()) {
                    viewModel.updateVisitIslaInput(selectedIslands.joinToString(", "))
                }
                showIslaGridDialog = false
            }
        )
    }
}

@Composable
fun AssetSelectionGridDialog(
    allMachines: List<MachineEntity>,
    currentAssetInput: String,
    onDismiss: () -> Unit,
    onConfirm: (selectedAssets: List<String>, detectedIslands: List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val initialSelectedSet = remember(currentAssetInput) {
        currentAssetInput.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }
    var selectedAssetNumbers by remember { mutableStateOf(initialSelectedSet) }

    val filteredMachines = remember(searchQuery, allMachines) {
        if (searchQuery.isBlank()) {
            allMachines
        } else {
            val q = searchQuery.trim().lowercase()
            allMachines.filter { machine ->
                machine.assetNumber.lowercase().contains(q) ||
                machine.machineNumber.lowercase().contains(q) ||
                machine.brand.lowercase().contains(q) ||
                machine.model.lowercase().contains(q) ||
                machine.game.lowercase().contains(q) ||
                machine.island.lowercase().contains(q) ||
                machine.area.lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seleccionar Asset(s)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar asset, máquina, juego, isla...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("asset_grid_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Selector Action Bar (Count + Bulk actions)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedAssetNumbers.size} seleccionado(s)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                val newSet = selectedAssetNumbers.toMutableSet()
                                filteredMachines.forEach { m ->
                                    val assetKey = m.assetNumber.ifBlank { m.machineNumber }
                                    if (assetKey.isNotBlank()) newSet.add(assetKey)
                                }
                                selectedAssetNumbers = newSet
                            }
                        ) {
                            Text("Visibles", style = MaterialTheme.typography.labelSmall)
                        }

                        if (selectedAssetNumbers.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    selectedAssetNumbers = emptySet()
                                }
                            ) {
                                Text("Limpiar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grid View of Machines/Assets
                if (filteredMachines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (allMachines.isEmpty()) "El catálogo está vacío. Sube un Excel primero." else "No se encontraron máquinas para '$searchQuery'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredMachines, key = { it.id }) { machine ->
                            val assetKey = machine.assetNumber.ifBlank { machine.machineNumber }
                            val isSelected = selectedAssetNumbers.contains(assetKey)

                            Card(
                                onClick = {
                                    val newSet = selectedAssetNumbers.toMutableSet()
                                    if (isSelected) {
                                        newSet.remove(assetKey)
                                    } else {
                                        if (assetKey.isNotBlank()) newSet.add(assetKey)
                                    }
                                    selectedAssetNumbers = newSet
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Asset: $assetKey",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                val newSet = selectedAssetNumbers.toMutableSet()
                                                if (checked) {
                                                    if (assetKey.isNotBlank()) newSet.add(assetKey)
                                                } else {
                                                    newSet.remove(assetKey)
                                                }
                                                selectedAssetNumbers = newSet
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Isla: ${machine.island.ifBlank { "N/A" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                    )

                                    if (machine.game.isNotBlank()) {
                                        Text(
                                            text = machine.game,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val selectedList = selectedAssetNumbers.toList().sorted()
                            // Auto-detect islands for selected assets
                            val detectedIslands = allMachines
                                .filter { m ->
                                    val key = m.assetNumber.ifBlank { m.machineNumber }
                                    selectedAssetNumbers.contains(key)
                                }
                                .map { it.island.trim() }
                                .filter { it.isNotBlank() }
                                .distinct()
                                .sorted()

                            onConfirm(selectedList, detectedIslands)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_asset_grid_selection_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aceptar (${selectedAssetNumbers.size})")
                    }
                }
            }
        }
    }
}

@Composable
fun IslaSelectionGridDialog(
    allMachines: List<MachineEntity>,
    currentIslaInput: String,
    onDismiss: () -> Unit,
    onConfirm: (selectedIslands: List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val initialSelectedSet = remember(currentIslaInput) {
        currentIslaInput.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }
    var selectedIslandsSet by remember { mutableStateOf(initialSelectedSet) }

    val availableIslands = remember(allMachines) {
        val extracted = allMachines.map { it.island.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        if (extracted.isNotEmpty()) {
            extracted
        } else {
            listOf("ISLA 01", "ISLA 02", "ISLA 03", "FUMAR", "NO FUMAR", "AM", "AP", "SALA 1")
        }
    }

    val filteredIslands = remember(searchQuery, availableIslands) {
        if (searchQuery.isBlank()) {
            availableIslands
        } else {
            val q = searchQuery.trim().lowercase()
            availableIslands.filter { it.lowercase().contains(q) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seleccionar Isla(s)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar isla...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("island_grid_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar (Count + Bulk actions)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedIslandsSet.size} isla(s) seleccionada(s)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                val newSet = selectedIslandsSet.toMutableSet()
                                newSet.addAll(filteredIslands)
                                selectedIslandsSet = newSet
                            }
                        ) {
                            Text("Visibles", style = MaterialTheme.typography.labelSmall)
                        }

                        if (selectedIslandsSet.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    selectedIslandsSet = emptySet()
                                }
                            ) {
                                Text("Limpiar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grid View of Islands
                if (filteredIslands.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron islas para '$searchQuery'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredIslands, key = { it }) { islaName ->
                            val isSelected = selectedIslandsSet.contains(islaName)
                            val countInCatalog = remember(allMachines, islaName) {
                                allMachines.count { it.island.trim().equals(islaName, ignoreCase = true) }
                            }

                            Card(
                                onClick = {
                                    val newSet = selectedIslandsSet.toMutableSet()
                                    if (isSelected) {
                                        newSet.remove(islaName)
                                    } else {
                                        newSet.add(islaName)
                                    }
                                    selectedIslandsSet = newSet
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = islaName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                val newSet = selectedIslandsSet.toMutableSet()
                                                if (checked) {
                                                    newSet.add(islaName)
                                                } else {
                                                    newSet.remove(islaName)
                                                }
                                                selectedIslandsSet = newSet
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    if (countInCatalog > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$countInCatalog máquina(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val selectedList = selectedIslandsSet.toList().sorted()
                            onConfirm(selectedList)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_islands_grid_selection_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aceptar (${selectedIslandsSet.size})")
                    }
                }
            }
        }
    }
}
