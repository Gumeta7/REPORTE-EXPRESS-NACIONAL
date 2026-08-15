package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmailDraftPreviewDialog
import com.example.ui.components.MissingProviderEmailDialog
import com.example.ui.screens.ExtractFileScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MachineLocationScreen
import com.example.ui.screens.QuickReportScreen
import com.example.ui.screens.VisitsScreen
import com.example.ui.theme.ReportesExpressTheme
import com.example.ui.viewmodel.ReportViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkThemePref by viewModel.isDarkTheme.collectAsState()
            val systemInDark = isSystemInDarkTheme()
            val activeDarkTheme = isDarkThemePref ?: systemInDark
            val currentUser by viewModel.currentUser.collectAsState()

            ReportesExpressTheme(darkTheme = activeDarkTheme) {
                AnimatedContent(targetState = currentUser != null, label = "AuthTransition") { isLoggedIn ->
                    if (isLoggedIn) {
                        MainAppScreen(viewModel = viewModel)
                    } else {
                        LoginScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ReportViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 5 }

    val currentUser by viewModel.currentUser.collectAsState()
    val missingEmailState by viewModel.missingProviderEmailState.collectAsState()
    val showDraftDialog by viewModel.showDraftDialog.collectAsState()
    val currentDraftState by viewModel.currentDraft.collectAsState()
    val isDarkThemePref by viewModel.isDarkTheme.collectAsState()
    val systemInDark = isSystemInDarkTheme()
    val activeDarkTheme = isDarkThemePref ?: systemInDark

    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_app_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stylized "G" Badge Logo
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when (pagerState.currentPage) {
                                    0 -> "Reportes Express"
                                    1 -> "Actualizar Información"
                                    2 -> "Máquinas"
                                    3 -> "Registro de Visitas"
                                    else -> "Historial de Reportes"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            currentUser?.let { user ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (user.isAdmin) Icons.Default.Shield else Icons.Default.Person,
                                        contentDescription = "User",
                                        modifier = Modifier.size(12.dp),
                                        tint = if (user.isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    val userDisplay = if (user.isAdmin) {
                                        "${user.nombre} (ADMIN)"
                                    } else {
                                        "${user.nombre} · ${user.sala}"
                                    }
                                    Text(
                                        text = userDisplay,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkTheme(activeDarkTheme) },
                        modifier = Modifier.testTag("toggle_dark_theme_button")
                    ) {
                        Icon(
                            imageVector = if (activeDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (activeDarkTheme) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Logout Button
                    IconButton(
                        onClick = { showLogoutConfirmDialog = true },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    val pageAnimSpec = androidx.compose.animation.core.tween<Float>(
                        durationMillis = 380,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0, animationSpec = pageAnimSpec)
                            }
                        },
                        icon = { Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Generar") },
                        label = {
                            Text(
                                text = "Generar",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_quick_report")
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1, animationSpec = pageAnimSpec)
                            }
                        },
                        icon = { Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Actualizar") },
                        label = {
                            Text(
                                text = "Actualizar",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_extract_file")
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2, animationSpec = pageAnimSpec)
                            }
                        },
                        icon = { Icon(imageVector = Icons.Default.Casino, contentDescription = "Máquinas") },
                        label = {
                            Text(
                                text = "Máquinas",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_machine_location")
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 3,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(3, animationSpec = pageAnimSpec)
                            }
                        },
                        icon = { Icon(imageVector = Icons.Default.AssignmentInd, contentDescription = "Visitas") },
                        label = {
                            Text(
                                text = "Visitas",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (pagerState.currentPage == 3) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_visits")
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 4,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(4, animationSpec = pageAnimSpec)
                            }
                        },
                        icon = { Icon(imageVector = Icons.Default.History, contentDescription = "Historial") },
                        label = {
                            Text(
                                text = "Historial",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (pagerState.currentPage == 4) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_history")
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> QuickReportScreen(viewModel = viewModel)
                1 -> ExtractFileScreen(viewModel = viewModel)
                2 -> MachineLocationScreen(viewModel = viewModel)
                3 -> VisitsScreen(viewModel = viewModel)
                4 -> HistoryScreen(viewModel = viewModel)
            }
        }

        // Logout Confirmation Dialog
        if (showLogoutConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirmDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Salir",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("¿Cerrar Sesión?", fontWeight = FontWeight.Bold) },
                text = { Text("¿Estás seguro de que deseas salir de tu cuenta (${currentUser?.nombre ?: ""})?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutConfirmDialog = false
                            viewModel.logout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirmDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Missing Provider Email Alert Dialog
        missingEmailState?.let { state ->
            MissingProviderEmailDialog(
                state = state,
                onDismiss = {
                    viewModel.closeMissingEmailDialog()
                },
                onSaveEmailAndContinue = { newEmail ->
                    viewModel.saveProviderEmail(
                        id = state.providerId ?: 0,
                        providerName = state.providerName,
                        email = newEmail
                    )
                    val updatedDraft = state.draftToOpen.copy(recipient = newEmail)
                    viewModel.closeMissingEmailDialog()
                    viewModel.openDraftDialog(updatedDraft)
                },
                onContinueWithoutEmail = {
                    val draftWithBlank = state.draftToOpen.copy(recipient = "")
                    viewModel.closeMissingEmailDialog()
                    viewModel.openDraftDialog(draftWithBlank)
                }
            )
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
