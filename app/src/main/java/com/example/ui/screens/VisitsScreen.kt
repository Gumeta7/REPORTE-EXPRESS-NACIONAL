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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    var showVenueDialog by remember { mutableStateOf(storedVenueName.isBlank()) }
    var venueInput by remember { mutableStateOf(storedVenueName) }
    var showManageProvidersDialog by remember { mutableStateOf(false) }

    // Form fields
    val currentDateStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    val currentTimeStr = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

    var fecha by remember { mutableStateOf(currentDateStr) }
    var proveedor by remember { mutableStateOf("ZITRO") }
    var tecnico by remember { mutableStateOf("") }
    var horaEntrada by remember { mutableStateOf(currentTimeStr) }
    var motivoVisita by remember { mutableStateOf("Atención de incidencia") }
    var assetInput by remember { mutableStateOf("") }
    var islaInput by remember { mutableStateOf("") }

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
                islaInput = machine.island
            }
        }
    }

    // Build formatted WhatsApp text
    val whatsappText = remember(storedVenueName, fecha, proveedor, tecnico, horaEntrada, motivoVisita, assetInput, islaInput) {
        val sb = StringBuilder()
        sb.append("*${storedVenueName.ifBlank { "SALA REGISTRADA" }.uppercase()}*\n")
        sb.append("*Fecha:* $fecha\n")
        sb.append("*Proveedor:* ${proveedor.uppercase()}\n")
        sb.append("*Técnico:* ${tecnico.uppercase()}\n")
        sb.append("*Hora de entrada:* $horaEntrada\n")
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
                onValueChange = { fecha = it },
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
                onValueChange = { horaEntrada = it },
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
            onValueChange = { proveedor = it },
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
                    onClick = { proveedor = nameUpper },
                    label = { Text(nameUpper, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Technician Input
        OutlinedTextField(
            value = tecnico,
            onValueChange = { tecnico = it },
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
            onValueChange = { motivoVisita = it },
            label = { Text("Motivo de visita") },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("visit_reason_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Asset (Optional) and Island Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = assetInput,
                onValueChange = { assetInput = it },
                label = { Text("Asset (Opcional)") },
                placeholder = { Text("Ej: 585") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("visit_asset_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = islaInput,
                onValueChange = { islaInput = it },
                label = { Text("Isla") },
                placeholder = { Text("Ej: AP") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
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
                fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                horaEntrada = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                assetInput = ""
                islaInput = ""
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
}
