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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.ProviderEmailEntity
import com.example.ui.components.ManageProvidersDialog
import com.example.ui.viewmodel.ReportViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickReportScreen(
    viewModel: ReportViewModel
) {
    var promptInput by remember { mutableStateOf("") }
    val providerEmailsList by viewModel.providerEmails.collectAsState()
    var selectedRecipient by remember { mutableStateOf("soporte@zitro.com") }

    var showManageProvidersDialog by remember { mutableStateOf(false) }

    val samplePrompts = listOf(
        "reporta la maquina 444 a zitro por falla de botonera",
        "reportar maquina 1025 a IGT por pantalla negra",
        "reporta la maquina 882 a Aristocrat por problema en billetero",
        "falla en maquina 551 Novomatic no acepta monedas",
        "maquina 302 Konami pantalla tactil descalibrada"
    )

    // Auto-detect provider mentioned in prompt text to update active recipient chip
    val lowerPrompt = promptInput.lowercase()
    val detectedProvider = providerEmailsList.find { provider ->
        val name = provider.providerName.lowercase().trim()
        name.length >= 2 && lowerPrompt.contains(name)
    }
    val activeRecipient = detectedProvider?.email ?: selectedRecipient

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
                    .padding(20.dp),
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
                        contentDescription = "Quick Report",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Generador Rápido de Reporte",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Escribe el reporte en una frase y la app generará el correo completo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recipient Selection Row with Settings/Add Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Correo del Proveedor:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = { showManageProvidersDialog = true },
                modifier = Modifier.testTag("manage_provider_emails_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar",
                    modifier = Modifier.height(16.dp).width(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gestionar Correos", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        // Prompt Input Field
        Text(
            text = "Escribe tu reporte o selecciona un ejemplo:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = promptInput,
            onValueChange = { promptInput = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("quick_report_prompt_input"),
            placeholder = { Text("Ej: reporta la maquina 444 a zitro por falla de botonera") },
            maxLines = 4,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Example Chips
        Text(
            text = "Sugerencias de rápida selección:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            samplePrompts.forEach { sample ->
                Card(
                    modifier = Modifier.clickable { promptInput = sample },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = sample,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Generate Button
        Button(
            onClick = {
                if (promptInput.isNotBlank()) {
                    viewModel.generateQuickReport(promptInput, activeRecipient)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("generate_quick_report_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Send, contentDescription = "Generar")
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Generar y Revisar Correo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Requirement info box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Ayuda",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "El generador busca automáticamente la máquina en el catálogo local para incluir Marca, Modelo, Número de Serie, Asset, Área, Isla y Juego exactos en el cuerpo del correo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
