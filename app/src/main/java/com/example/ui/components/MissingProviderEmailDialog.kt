package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.MissingProviderEmailState

@Composable
fun MissingProviderEmailDialog(
    state: MissingProviderEmailState,
    onDismiss: () -> Unit,
    onSaveEmailAndContinue: (newEmail: String) -> Unit,
    onContinueWithoutEmail: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MarkEmailUnread,
                    contentDescription = "Sin correo",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = "Correo No Definido",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "No se ha definido un correo para el proveedor '${state.providerName}'.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "¿Deseas agregar un correo electrónico ahora para este proveedor?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Correo Electrónico (ej. soporte@${state.providerName.lowercase().replace(" ", "")}.com)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("missing_provider_email_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveEmailAndContinue(emailInput.trim())
                },
                enabled = emailInput.isNotBlank(),
                modifier = Modifier.testTag("save_missing_email_button")
            ) {
                Text("Guardar y Continuar")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onContinueWithoutEmail,
                    modifier = Modifier.testTag("continue_without_email_button")
                ) {
                    Text("Continuar sin correo")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cancel_missing_email_button")
                ) {
                    Text("Cancelar")
                }
            }
        }
    )
}
