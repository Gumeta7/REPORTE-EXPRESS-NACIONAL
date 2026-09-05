package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.GmailRed
import com.example.ui.theme.OutlookBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.EmailDraftState
import com.example.util.EmailIntentUtil

@Composable
fun EmailDraftPreviewDialog(
    draftState: EmailDraftState,
    onDismiss: () -> Unit,
    onDraftUpdated: (EmailDraftState) -> Unit,
    onSaveToHistory: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
                .testTag("email_draft_preview_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Title
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
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Revisar y Editar Correo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ajusta el contenido antes de enviarlo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recipient
                OutlinedTextField(
                    value = draftState.recipient,
                    onValueChange = { onDraftUpdated(draftState.copy(recipient = it)) },
                    label = { Text("Para (Destinatario)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_recipient_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subject
                OutlinedTextField(
                    value = draftState.subject,
                    onValueChange = { onDraftUpdated(draftState.copy(subject = it)) },
                    label = { Text("Asunto del Correo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_subject_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Body Editor
                OutlinedTextField(
                    value = draftState.body,
                    onValueChange = { onDraftUpdated(draftState.copy(body = it)) },
                    label = { Text("Cuerpo del Correo (Editables)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("email_body_input"),
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Extracted Machine Highlights Badge Card
                if (draftState.serialNumber.isNotBlank() || draftState.assetNumber.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Resumen de Datos Extraídos",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Serie: ${draftState.serialNumber.ifBlank { "N/A" }} | Asset: ${draftState.assetNumber.ifBlank { "N/A" }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Marca: ${draftState.brand.ifBlank { "N/A" }} | Modelo: ${draftState.model.ifBlank { "N/A" }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action Buttons for Sending / Copying / Saving
                Text(
                    text = "Opciones de Envío e Integración",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Gmail Direct
                Button(
                    onClick = {
                        onSaveToHistory()
                        EmailIntentUtil.sendViaGmail(
                            context,
                            draftState.recipient,
                            draftState.subject,
                            draftState.body
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_via_gmail_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GmailRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Gmail")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar con Gmail", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Outlook Direct
                Button(
                    onClick = {
                        onSaveToHistory()
                        EmailIntentUtil.sendViaOutlook(
                            context,
                            draftState.recipient,
                            draftState.subject,
                            draftState.body
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_via_outlook_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = OutlookBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Outlook")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar con Outlook", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Standard Chooser
                    OutlinedButton(
                        onClick = {
                            onSaveToHistory()
                            EmailIntentUtil.sendViaStandardEmailChooser(
                                context,
                                draftState.recipient,
                                draftState.subject,
                                draftState.body
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_via_chooser_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = "Mail")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Otra App")
                    }

                    // Copy to Clipboard
                    OutlinedButton(
                        onClick = {
                            EmailIntentUtil.copyToClipboard(
                                context,
                                "Correo de Reporte",
                                "Asunto: ${draftState.subject}\n\n${draftState.body}"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_email_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copiar")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Save to history only button
                Button(
                    onClick = {
                        onSaveToHistory()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_history_only_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Guardar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar en Historial", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
