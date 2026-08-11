package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.components.EmailDraftPreviewDialog
import com.example.ui.screens.ExtractFileScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MachineLocationScreen
import com.example.ui.screens.QuickReportScreen
import com.example.ui.screens.VisitsScreen
import com.example.ui.theme.ReportesExpressTheme
import com.example.ui.viewmodel.ReportViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReportesExpressTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ReportViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val showDraftDialog by viewModel.showDraftDialog.collectAsState()
    val currentDraftState by viewModel.currentDraft.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_app_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "Reportes Express"
                            1 -> "Extraer Datos de Archivo"
                            2 -> "¿Dónde está la Máquina?"
                            3 -> "Registro de Visitas"
                            else -> "Historial de Reportes"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Generar") },
                    label = { Text("Generar") },
                    modifier = Modifier.testTag("tab_quick_report")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Extraer") },
                    label = { Text("Extraer") },
                    modifier = Modifier.testTag("tab_extract_file")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.Place, contentDescription = "Ubicación") },
                    label = { Text("Ubicación") },
                    modifier = Modifier.testTag("tab_machine_location")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.AssignmentInd, contentDescription = "Visitas") },
                    label = { Text("Visitas") },
                    modifier = Modifier.testTag("tab_visits")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(imageVector = Icons.Default.History, contentDescription = "Historial") },
                    label = { Text("Historial") },
                    modifier = Modifier.testTag("tab_history")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> QuickReportScreen(viewModel = viewModel)
                1 -> ExtractFileScreen(viewModel = viewModel)
                2 -> MachineLocationScreen(viewModel = viewModel)
                3 -> VisitsScreen(viewModel = viewModel)
                4 -> HistoryScreen(viewModel = viewModel)
            }
        }

        // Step 2 & 3: Email Review and Direct Dispatch Modal
        if (showDraftDialog) {
            EmailDraftPreviewDialog(
                draftState = currentDraftState,
                onDismiss = { viewModel.closeDraftDialog() },
                onDraftUpdated = { updatedDraft ->
                    viewModel.updateCurrentDraft(
                        recipient = updatedDraft.recipient,
                        subject = updatedDraft.subject,
                        body = updatedDraft.body
                    )
                },
                onSaveToHistory = {
                    viewModel.saveDraftToHistory()
                }
            )
        }
    }
}
