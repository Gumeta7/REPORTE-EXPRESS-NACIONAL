package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.db.ProviderEmailEntity

@Composable
fun ManageProvidersDialog(
    providersList: List<ProviderEmailEntity>,
    onDismiss: () -> Unit,
    onSaveProvider: (id: Int, name: String, email: String) -> Unit,
    onDeleteProvider: (Int) -> Unit,
    onRestoreDefaults: () -> Unit
) {
    var editingProviderId by remember { mutableStateOf<Int?>(null) }
    var providerNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Correos Proveedores",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gestionar Proveedores")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (editingProviderId == null) "Agregar nuevo proveedor:" else "Editar proveedor:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (editingProviderId == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = providerNameInput,
                    onValueChange = { providerNameInput = it },
                    label = { Text("Nombre / Proveedor (ej. Zitro, IGT)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_provider_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Correo Electrónico (ej. soporte@zitro.com)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_provider_email_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (providerNameInput.isNotBlank() && emailInput.isNotBlank()) {
                                onSaveProvider(
                                    editingProviderId ?: 0,
                                    providerNameInput,
                                    emailInput
                                )
                                providerNameInput = ""
                                emailInput = ""
                                editingProviderId = null
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_new_provider_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (editingProviderId == null) Icons.Default.Add else Icons.Default.Save,
                            contentDescription = "Guardar"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (editingProviderId == null) "Guardar Proveedor" else "Actualizar Proveedor")
                    }

                    if (editingProviderId != null) {
                        TextButton(
                            onClick = {
                                editingProviderId = null
                                providerNameInput = ""
                                emailInput = ""
                            }
                        ) {
                            Text("Cancelar")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Divider()
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Proveedores Registrados (${providersList.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                if (providersList.isEmpty()) {
                    Text(
                        text = "No hay proveedores guardados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        providersList.forEach { provider ->
                            val isEditingThis = editingProviderId == provider.id
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isEditingThis) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = provider.providerName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = provider.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingProviderId = provider.id
                                                providerNameInput = provider.providerName
                                                emailInput = provider.email
                                            },
                                            modifier = Modifier.testTag("edit_provider_${provider.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (editingProviderId == provider.id) {
                                                    editingProviderId = null
                                                    providerNameInput = ""
                                                    emailInput = ""
                                                }
                                                onDeleteProvider(provider.id)
                                            },
                                            modifier = Modifier.testTag("delete_provider_${provider.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = {
                        editingProviderId = null
                        providerNameInput = ""
                        emailInput = ""
                        onRestoreDefaults()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Restablecer Lista por Defecto", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Listo")
            }
        }
    )
}
