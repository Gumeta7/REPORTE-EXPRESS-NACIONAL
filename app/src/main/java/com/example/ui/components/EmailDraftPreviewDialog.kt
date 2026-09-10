package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DarkOledBorder
import com.example.ui.theme.DarkOledSurface
import com.example.ui.theme.DarkOledSurfaceVariant
import com.example.ui.theme.GmailRed
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.viewmodel.EmailDraftState
import com.example.util.EmailIntentUtil

@Composable
fun EmailDraftPreviewDialog(
    draftState: EmailDraftState,
    onDismiss: () -> Unit,
    onDraftUpdated: (EmailDraftState) -> Unit,
    onSendEmail: () -> Unit
) {
    val context = LocalContext.current

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val dialogBg = if (isDark) DarkOledSurface else Color(0xFFF1F5F9)
    val headerBg = if (isDark) DarkOledSurfaceVariant else Color.White
    val cardBg = if (isDark) DarkOledSurfaceVariant else Color.White
    val borderCol = if (isDark) DarkOledBorder else Color(0xFFCBD5E1)
    val dividerCol = if (isDark) DarkOledBorder else Color(0xFFE2E8F0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("email_draft_preview_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = dialogBg,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Header fijo (siempre visible en el tope de la pantalla, nunca recortado)
                Surface(
                    color = headerBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.2f else 0.12f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Previsualización de Correo",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) TextPrimaryDark else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Revisa los datos antes de despachar",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
                            )
                        }
                    }
                }

                HorizontalDivider(color = dividerCol)

                // 2. Cuerpo desplazable (scrollable body para ver todo el contenido fluidamente)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Asunto del Correo (No editable)
                    OutlinedTextField(
                        value = draftState.subject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Asunto del Correo", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_subject_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            focusedBorderColor = borderCol,
                            unfocusedBorderColor = borderCol,
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Cuerpo del Correo (No editable)
                    OutlinedTextField(
                        value = draftState.body,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cuerpo del Correo", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("email_body_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            focusedBorderColor = borderCol,
                            unfocusedBorderColor = borderCol,
                            focusedTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                            unfocusedTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Resumen de Datos Extraídos (Alto Contraste)
                    if (draftState.serialNumber.isNotBlank() || draftState.assetNumber.isNotBlank() || !draftState.ticketId.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Resumen de Datos Extraídos",
                                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) TextPrimaryDark else Color(0xFF0F172A)
                                    )
                                    if (!draftState.ticketId.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = draftState.ticketId,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = dividerCol
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Serie: ${draftState.serialNumber.ifBlank { "N/A" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                    )
                                    Text(
                                        text = "Asset: ${draftState.assetNumber.ifBlank { "N/A" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Marca: ${draftState.brand.ifBlank { "N/A" }}  •  Modelo: ${draftState.model.ifBlank { "N/A" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    // Parámetros de la Incidencia (Operativa y Prioridad)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Fila 1: ¿Operativa?
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "¿Máquina Operativa?",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) TextPrimaryDark else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (draftState.operativa == "NO") "Fuera de servicio" else "En funcionamiento",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (draftState.operativa == "NO") MaterialTheme.colorScheme.error else Color(0xFF16A34A)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = draftState.operativa == "NO",
                                        onClick = { onDraftUpdated(draftState.copy(operativa = "NO")) },
                                        label = { Text("NO", fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.error,
                                            selectedLabelColor = MaterialTheme.colorScheme.onError
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    FilterChip(
                                        selected = draftState.operativa == "SI",
                                        onClick = { onDraftUpdated(draftState.copy(operativa = "SI")) },
                                        label = { Text("SÍ", fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF16A34A),
                                            selectedLabelColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = dividerCol)

                            // Fila 2: Prioridad
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Nivel de Prioridad:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) TextPrimaryDark else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = when (draftState.prioridad) {
                                            "CRITICA" -> "Atención Inmediata"
                                            "MEDIA" -> "Media"
                                            else -> "Baja"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (draftState.prioridad) {
                                            "CRITICA" -> Color(0xFFDC2626)
                                            "MEDIA" -> Color(0xFFEA580C)
                                            else -> Color(0xFFCA8A04)
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = draftState.prioridad == "BAJA",
                                        onClick = { onDraftUpdated(draftState.copy(prioridad = "BAJA")) },
                                        label = {
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Text("Baja", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFEAB308),
                                            selectedLabelColor = Color(0xFF1C1917)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    FilterChip(
                                        selected = draftState.prioridad == "MEDIA",
                                        onClick = { onDraftUpdated(draftState.copy(prioridad = "MEDIA")) },
                                        label = {
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Text("Media", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFEA580C),
                                            selectedLabelColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    FilterChip(
                                        selected = draftState.prioridad == "CRITICA",
                                        onClick = { onDraftUpdated(draftState.copy(prioridad = "CRITICA")) },
                                        label = {
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Text("Crítica", fontWeight = FontWeight.ExtraBold)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFDC2626),
                                            selectedLabelColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Opciones de Envío e Integración
                    Text(
                        text = "Opciones de Envío e Integración",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    )

                    // Gmail Direct
                    Button(
                        onClick = {
                            onSendEmail()
                            EmailIntentUtil.sendViaGmail(
                                context,
                                draftState.recipient,
                                draftState.subject,
                                draftState.body,
                                draftState.cc
                            )
                            onDismiss()
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

                    // Cancel button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cancel_dialog_button"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Cancelar",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                        )
                    }
                }
            }
        }
    }
}
