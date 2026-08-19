package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.EmailReportEntity
import com.example.ui.theme.GmailRed
import com.example.ui.theme.OutlookBlue
import com.example.ui.viewmodel.EmailDraftState
import com.example.ui.viewmodel.ReportViewModel
import com.example.util.EmailIntentUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: ReportViewModel
) {
    val context = LocalContext.current
    val searchQuery by viewModel.historySearchQuery.collectAsState()
    val reportsList by viewModel.reportHistory.collectAsState()

    var showConfirmClearAllDialog by remember { mutableStateOf(false) }
    var reportToDelete by remember { mutableStateOf<EmailReportEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("history_screen")
    ) {
        // Search Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateHistoryQuery(it) },
                placeholder = { Text("Buscar en historial...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateHistoryQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            if (reportsList.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showConfirmClearAllDialog = true },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Limpiar Todo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Filter Chips for History Types (Todos, Incidencias, Visitas)
        var selectedTypeFilter by remember { mutableStateOf("TODOS") }

        val filteredReports = remember(reportsList, selectedTypeFilter) {
            when (selectedTypeFilter) {
                "INCIDENCIAS" -> reportsList.filter { !it.subject.contains("Visita", ignoreCase = true) && !it.subject.contains("Técnica", ignoreCase = true) }
                "VISITAS" -> reportsList.filter { it.subject.contains("Visita", ignoreCase = true) || it.subject.contains("Técnica", ignoreCase = true) }
                else -> reportsList
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isAll = selectedTypeFilter == "TODOS"
            androidx.compose.material3.FilterChip(
                selected = isAll,
                onClick = { selectedTypeFilter = "TODOS" },
                label = { Text("Todos (${reportsList.size})") },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )

            val isIncidencias = selectedTypeFilter == "INCIDENCIAS"
            val incidenciasCount = remember(reportsList) { reportsList.count { !it.subject.contains("Visita", ignoreCase = true) } }
            androidx.compose.material3.FilterChip(
                selected = isIncidencias,
                onClick = { selectedTypeFilter = "INCIDENCIAS" },
                label = { Text("🚨 Incidencias ($incidenciasCount)") },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(10.dp)
            )

            val isVisitas = selectedTypeFilter == "VISITAS"
            val visitasCount = remember(reportsList) { reportsList.count { it.subject.contains("Visita", ignoreCase = true) } }
            androidx.compose.material3.FilterChip(
                selected = isVisitas,
                onClick = { selectedTypeFilter = "VISITAS" },
                label = { Text("📋 Visitas ($visitasCount)") },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Historial de Correos Enviados (${filteredReports.size}):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Sin historial",
                        modifier = Modifier.height(48.dp).width(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedTypeFilter != "TODOS") "No se encontraron registros con el filtro aplicado." else "Aún no se han enviado ni guardado correos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredReports,
                    key = { it.id },
                    contentType = { "history_card" }
                ) { report ->
                    HistoryReportCard(
                        report = report,
                        onReOpen = {
                            viewModel.openDraftDialog(
                                EmailDraftState(
                                    recipient = report.recipient,
                                    subject = report.subject,
                                    body = report.body,
                                    machineNumber = report.machineNumber,
                                    issueDescription = report.issueDescription,
                                    brand = report.brand,
                                    model = report.model,
                                    serialNumber = report.serialNumber,
                                    assetNumber = report.assetNumber
                                )
                            )
                        },
                        onCopy = {
                            EmailIntentUtil.copyToClipboard(
                                context,
                                "Reporte de Correo",
                                "Asunto: ${report.subject}\n\n${report.body}"
                            )
                        },
                        onDelete = { reportToDelete = report },
                        onSendGmail = {
                            EmailIntentUtil.sendViaGmail(
                                context,
                                report.recipient,
                                report.subject,
                                report.body
                            )
                        },
                        onSendOutlook = {
                            EmailIntentUtil.sendViaOutlook(
                                context,
                                report.recipient,
                                report.subject,
                                report.body
                            )
                        }
                    )
                }
            }
        }
    }

    if (showConfirmClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearAllDialog = false },
            title = { Text("¿Eliminar Todo el Historial?") },
            text = { Text("Esta acción eliminará permanentemente todos los ${reportsList.size} reportes guardados en el historial.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showConfirmClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar Historial")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearAllDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    reportToDelete?.let { report ->
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            title = { Text("¿Eliminar Reporte?") },
            text = { Text("¿Deseas eliminar el reporte enviado a ${report.recipient}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHistoryReport(report.id)
                        reportToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun HistoryReportCard(
    report: EmailReportEntity,
    onReOpen: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onSendGmail: () -> Unit,
    onSendOutlook: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(report.timestamp) { dateFormat.format(Date(report.timestamp)) }
    val displayAsset = report.assetNumber.ifBlank { report.machineNumber.ifBlank { "N/A" } }
    val displayIssue = report.issueDescription.ifBlank { "Falla sin especificar" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_card_${report.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            // Header Row: Recipient & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Para: ${report.recipient}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Asset Number Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Asset Number: ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = displayAsset,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Falla (Issue Description) Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Falla: ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = displayIssue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Compact Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onSendGmail, modifier = Modifier.height(32.dp).width(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Reenviar Gmail",
                            tint = GmailRed,
                            modifier = Modifier.height(18.dp).width(18.dp)
                        )
                    }

                    IconButton(onClick = onSendOutlook, modifier = Modifier.height(32.dp).width(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Reenviar Outlook",
                            tint = OutlookBlue,
                            modifier = Modifier.height(18.dp).width(18.dp)
                        )
                    }

                    IconButton(onClick = onCopy, modifier = Modifier.height(32.dp).width(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar",
                            modifier = Modifier.height(18.dp).width(18.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = onReOpen,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Ver / Editar",
                            modifier = Modifier.height(14.dp).width(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver / Editar", style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.height(32.dp).width(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.height(18.dp).width(18.dp)
                        )
                    }
                }
            }
        }
    }
}

