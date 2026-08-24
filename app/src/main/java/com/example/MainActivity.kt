package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.withTimeout
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import com.example.ui.components.EmailDraftPreviewDialog
import com.example.ui.components.MissingProviderEmailDialog
import com.example.ui.screens.ExtractFileScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MachineLocationScreen
import com.example.ui.screens.QuickReportScreen
import com.example.ui.screens.ReportMachineFailureDialog
import com.example.ui.screens.VisitsScreen
import com.example.ui.theme.ReportesExpressTheme
import com.example.ui.viewmodel.ReportViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle Deep Link / QR scan when opening the app
        viewModel.handleDeepLink(intent?.data)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle Deep Link / QR scan if app was already running in background
        viewModel.handleDeepLink(intent.data)
    }
}

// Helper para vibración háptica al disparar evento secreto
fun triggerHapticVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(160, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(160, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(160)
            }
        }
    } catch (_: Exception) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ReportViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 5 }
    val context = LocalContext.current

    val currentUser by viewModel.currentUser.collectAsState()
    val missingEmailState by viewModel.missingProviderEmailState.collectAsState()
    val showDraftDialog by viewModel.showDraftDialog.collectAsState()
    val currentDraftState by viewModel.currentDraft.collectAsState()
    val isDarkThemePref by viewModel.isDarkTheme.collectAsState()
    val systemInDark = isSystemInDarkTheme()
    val activeDarkTheme = isDarkThemePref ?: systemInDark

    // QR Deep Link Navigation State
    val targetTab by viewModel.targetTabFromDeepLink.collectAsState()
    val deepLinkMachine by viewModel.deepLinkMachine.collectAsState()

    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showParraSecretPreview by remember { mutableStateOf(false) }

    LaunchedEffect(targetTab) {
        targetTab?.let { page ->
            pagerState.animateScrollToPage(page)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_app_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            currentUser?.let { user ->
                                com.example.ui.components.TechnicianMonogramAvatar(
                                    name = user.nombre,
                                    size = 38
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = user.nombre,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (user.isAdmin) "Administrador Corporativo" else "Técnico",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } ?: run {
                                Text(
                                    text = "Reportes Express",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Casino Venue Brand Badge / Personal User Logo in Top Bar (Exclusivo para usuario aparra)
                        val isParraUser = currentUser?.usuario?.trim()?.equals("aparra", ignoreCase = true) == true

                        if (isParraUser) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            var triggered = false
                                            try {
                                                withTimeout(3000L) {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.firstOrNull { it.id == down.id }
                                                        if (change == null || !change.pressed) {
                                                            break
                                                        }
                                                    }
                                                }
                                            } catch (_: PointerEventTimeoutCancellationException) {
                                                triggered = true
                                            }
                                            if (triggered) {
                                                triggerHapticVibration(context)
                                                showParraSecretPreview = true
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo_parra),
                                    contentDescription = "Logo Personal",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .height(34.dp)
                                        .widthIn(min = 45.dp, max = 95.dp)
                                )
                            }
                        } else {
                            val activeVenue = currentUser?.sala?.trim()?.ifBlank { "CORPORATIVO" } ?: "CORPORATIVO"
                            com.example.ui.components.VenueLogoBadge(
                                venueName = activeVenue,
                                compact = true
                            )
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
            val navItems = remember {
                listOf(
                    com.example.ui.components.NavigationTabItem("Generar", Icons.Default.FlashOn, "tab_quick_report"),
                    com.example.ui.components.NavigationTabItem("Actualizar", Icons.Default.CloudSync, "tab_extract_file"),
                    com.example.ui.components.NavigationTabItem("Máquinas", Icons.Default.Casino, "tab_machine_location"),
                    com.example.ui.components.NavigationTabItem("Visitas", Icons.Default.AssignmentInd, "tab_visits"),
                    com.example.ui.components.NavigationTabItem("Historial", Icons.Default.History, "tab_history")
                )
            }
            com.example.ui.components.AnimatedExpandingBottomBar(
                selectedIndex = pagerState.currentPage,
                onItemSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            index,
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 350,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                    }
                },
                items = navItems
            )
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

        // Deep Link Machine Report Dialog (Auto opened from QR scan)
        deepLinkMachine?.let { machine ->
            ReportMachineFailureDialog(
                machine = machine,
                onDismiss = {
                    viewModel.clearDeepLinkMachine()
                },
                onConfirm = { failureDescription ->
                    viewModel.generateReportForMachine(machine, failureDescription)
                    viewModel.clearDeepLinkMachine()
                }
            )
        }

        // Deep Link Sala Mismatch Warning Dialog (Technician trying to scan other venue's QR)
        val deepLinkSalaMismatchError by viewModel.deepLinkSalaMismatchError.collectAsState()
        deepLinkSalaMismatchError?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { viewModel.clearDeepLinkSalaMismatchError() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Acceso Restringido",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Ubicación No Compatible",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearDeepLinkSalaMismatchError() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Entendido", fontWeight = FontWeight.Bold)
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

        // Modal de Visualización Personal para aparra (3 segundos)
        if (showParraSecretPreview) {
            Dialog(
                onDismissRequest = { showParraSecretPreview = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnClickOutside = true
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.82f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showParraSecretPreview = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                                scaleIn(
                                    initialScale = 0.75f,
                                    animationSpec = androidx.compose.animation.core.spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                    )
                                ),
                        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) +
                                scaleOut(targetScale = 0.75f)
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(28.dp)
                                .wrapContentSize(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo_parra),
                                    contentDescription = "Logo Personal aparra",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
