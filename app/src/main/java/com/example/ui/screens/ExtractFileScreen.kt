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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.ReportViewModel

@Composable
fun ExtractFileScreen(
    viewModel: ReportViewModel,
) {
    val context = LocalContext.current

    val isExtracting by viewModel.isExtractingFile.collectAsState()
    val extractionResult by viewModel.extractionResult.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val catalogMachines by viewModel.allMachines.collectAsState()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showReplaceConfirmDialog by remember { mutableStateOf(false) }

    // File Picker Contract for Excel / CSV files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            if (catalogMachines.isNotEmpty()) {
                pendingUri = selectedUri
                showReplaceConfirmDialog = true
            } else {
                try {
                    context.contentResolver.openInputStream(selectedUri)?.use { stream ->
                        val bytes = stream.readBytes()
                        viewModel.importMachinesFromBytes(bytes, clearExistingFirst = false)
                    }
                } catch (_: Throwable) {
                    // handle error gracefully
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("extract_file_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(24.dp)
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
                            MaterialTheme.colorScheme.secondary,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload",
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Cargar Catálogo Excel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adjunta tu archivo (.xlsx, .xls o .csv) para actualizar e importar automáticamente las máquinas al catálogo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Single Full-Width Action Button: Seleccionar Archivo Excel
        Button(
            onClick = { filePickerLauncher.launch("*/*") },
            enabled = !isExtracting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("select_file_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isExtracting) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(24.dp)
                        .width(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Procesando Archivo Excel...", fontWeight = FontWeight.Bold)
            } else {
                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Archivo")
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Seleccionar Archivo Excel / CSV",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Status Message Notification Card
        statusMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Éxito",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Extracted Result Preview Card
        extractionResult?.let { res ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("extracted_result_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Campos Requeridos Extraídos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completado",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ExtractedDetailRow(label = "1. Número de Serie", value = res.serialNumber.ifBlank { "N/A" })
                    ExtractedDetailRow(label = "2. Marca", value = res.brand.ifBlank { "N/A" })
                    ExtractedDetailRow(label = "3. Modelo", value = res.model.ifBlank { "N/A" })
                    ExtractedDetailRow(label = "4. Número de Asset", value = res.assetNumber.ifBlank { "N/A" })
                }
            }
        }
    }

    // Confirmation Dialog for Replacing vs Appending Catalog Data
    if (showReplaceConfirmDialog && pendingUri != null) {
        AlertDialog(
            onDismissRequest = {
                showReplaceConfirmDialog = false
                pendingUri = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Confirmación",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "¿Sustituir catálogo actual?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Actualmente existen ${catalogMachines.size} máquinas en la base de datos. ¿Deseas reemplazar completamente el catálogo para evitar duplicados o prefieres añadir las nuevas máquinas manteniendo las existentes?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uriToProcess = pendingUri
                        showReplaceConfirmDialog = false
                        pendingUri = null
                        uriToProcess?.let { uri ->
                            try {
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    val bytes = stream.readBytes()
                                    viewModel.importMachinesFromBytes(bytes, clearExistingFirst = true)
                                }
                            } catch (_: Throwable) {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Sustituir Base", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showReplaceConfirmDialog = false
                            pendingUri = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                    OutlinedButton(
                        onClick = {
                            val uriToProcess = pendingUri
                            showReplaceConfirmDialog = false
                            pendingUri = null
                            uriToProcess?.let { uri ->
                                try {
                                    context.contentResolver.openInputStream(uri)?.use { stream ->
                                        val bytes = stream.readBytes()
                                        viewModel.importMachinesFromBytes(bytes, clearExistingFirst = false)
                                    }
                                } catch (_: Throwable) {}
                            }
                        }
                    ) {
                        Text("Añadir a Existente")
                    }
                }
            }
        )
    }
}

@Composable
fun ExtractedDetailRow(label: String, value: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value.ifBlank { "N/A" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
