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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.data.db.MachineEntity
import com.example.ui.viewmodel.ReportViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun ExtractFileScreen(
    viewModel: ReportViewModel
) {
    val context = LocalContext.current
    var rawTextContent by remember {
        mutableStateOf(
            """
            REGISTRO TÉCNICO DE MÁQUINA
            Marca: Zitro
            Modelo: Altius Glare
            Número de Serie: SN-ZTR-998231
            Número de Asset: AST-0444
            Falla: Falla intermitente en botonera principal y sensor de billetero
            """.trimIndent()
        )
    }

    val isExtracting by viewModel.isExtractingFile.collectAsState()
    val extractionResult by viewModel.extractionResult.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // File Picker Contract
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val extractedText = com.example.util.FileParserUtil.extractTextFromStream(stream)
                    if (extractedText.isNotBlank()) {
                        rawTextContent = extractedText
                        viewModel.extractDataFromTextOrFile(extractedText)
                    }
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("extract_file_screen")
    ) {
        // Top Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                            MaterialTheme.colorScheme.secondary,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
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
                        text = "Extracción de Datos de Archivos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sube un archivo (CSV, TXT, Excel o documento) para extraer Serie, Marca, Modelo y Asset automáticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // File Selector Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("select_file_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Archivo")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seleccionar Archivo")
            }

            OutlinedButton(
                onClick = { viewModel.extractDataFromTextOrFile(rawTextContent, isCsvCatalog = true) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("import_csv_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.List, contentDescription = "Catálogo")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Importar Catálogo")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Raw Text Box
        Text(
            text = "O pega/edita el contenido del documento manualmente:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = rawTextContent,
            onValueChange = { rawTextContent = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .testTag("raw_file_content_input"),
            maxLines = 8,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Process Button
        Button(
            onClick = {
                viewModel.extractDataFromTextOrFile(rawTextContent)
            },
            enabled = !isExtracting && rawTextContent.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("extract_data_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isExtracting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp).width(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Procesando Datos con IA...")
            } else {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Extraer")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Extraer Serie, Marca, Modelo y Asset", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Status Toast Message Display
        statusMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "OK", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Extracted Result Card
        extractionResult?.let { res ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("extracted_result_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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

                    ExtractedDetailRow(label = "1. Número de Serie:", value = res.serialNumber.ifBlank { "N/A" })
                    ExtractedDetailRow(label = "2. Marca:", value = res.brand.ifBlank { "N/A" })
                    ExtractedDetailRow(label = "3. Modelo:", value = res.model.ifBlank { "N/A" })
                    ExtractedDetailRow(label = "4. Número de Asset:", value = res.assetNumber.ifBlank { "N/A" })

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.openDraftDialog(viewModel.currentDraft.value)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("generate_email_from_extracted_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = "Correo")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generar Correo")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.addManualMachine(
                                    MachineEntity(
                                        machineNumber = res.machineNumber.ifBlank { res.assetNumber },
                                        assetNumber = res.assetNumber.ifBlank { "AST-NEW" },
                                        serialNumber = res.serialNumber.ifBlank { "SN-NEW" },
                                        brand = res.brand.ifBlank { "Generica" },
                                        model = res.model.ifBlank { "Estandar" },
                                        area = "Sala Principal",
                                        game = "Multijuego",
                                        island = "Isla Central"
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_to_catalog_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Guardar")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar Catálogo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtractedDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
