package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ExtractedMachineData
import com.example.data.db.AppDatabase
import com.example.data.db.EmailReportEntity
import com.example.data.db.MachineEntity
import com.example.data.db.TechnicianEntity
import com.example.data.remote.DriveSyncService
import com.example.data.repository.ReportRepository
import com.example.util.FileParserUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class EmailDraftState(
    val recipient: String = "",
    val subject: String = "",
    val body: String = "",
    val machineNumber: String = "",
    val issueDescription: String = "",
    val brand: String = "",
    val model: String = "",
    val serialNumber: String = "",
    val assetNumber: String = "",
    val sala: String = ""
)

data class MissingProviderEmailState(
    val providerName: String,
    val providerId: Int?,
    val draftToOpen: EmailDraftState
)

class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReportRepository
    private val prefs = application.getSharedPreferences("reportes_express_prefs", android.content.Context.MODE_PRIVATE)

    // --- Authentication & Current User Session ---
    private val _currentUser = MutableStateFlow<TechnicianEntity?>(restoreLoggedUserFromPrefs())
    val currentUser: StateFlow<TechnicianEntity?> = _currentUser.asStateFlow()

    private val _loginErrorMessage = MutableStateFlow<String?>(null)
    val loginErrorMessage: StateFlow<String?> = _loginErrorMessage.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _venueName = MutableStateFlow(
        _currentUser.value?.sala?.ifBlank { prefs.getString("venue_name", "") ?: "" }
            ?: (prefs.getString("venue_name", "") ?: "")
    )
    val venueName: StateFlow<String> = _venueName.asStateFlow()

    private val _isDarkTheme = MutableStateFlow<Boolean?>(
        if (prefs.contains("is_dark_theme")) prefs.getBoolean("is_dark_theme", false) else null
    )
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    // --- Google Drive Synchronization State ---
    private val _isSyncingDrive = MutableStateFlow(false)
    val isSyncingDrive: StateFlow<Boolean> = _isSyncingDrive.asStateFlow()

    private val _lastSyncTimestampFormatted = MutableStateFlow(
        prefs.getString("last_sync_formatted", "Sin sincronizaciones previas") ?: "Sin sincronizaciones previas"
    )
    val lastSyncTimestampFormatted: StateFlow<String> = _lastSyncTimestampFormatted.asStateFlow()

    // --- Visit Form State (Persisted across tab navigation) ---
    val visitFecha = MutableStateFlow(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    val visitProveedor = MutableStateFlow("ZITRO")
    val visitTecnico = MutableStateFlow(_currentUser.value?.nombre ?: "")
    val visitHoraEntrada = MutableStateFlow("")
    val visitHoraSalida = MutableStateFlow("")
    val visitMotivoVisita = MutableStateFlow("Atención de incidencia")
    val visitAssetInput = MutableStateFlow("")
    val visitIslaInput = MutableStateFlow("")

    fun updateVisitFecha(value: String) { visitFecha.value = value }
    fun updateVisitProveedor(value: String) { visitProveedor.value = value }
    fun updateVisitTecnico(value: String) { visitTecnico.value = value }
    fun updateVisitHoraEntrada(value: String) { visitHoraEntrada.value = value }
    fun updateVisitHoraSalida(value: String) { visitHoraSalida.value = value }
    fun updateVisitMotivoVisita(value: String) { visitMotivoVisita.value = value }
    fun updateVisitAssetInput(value: String) { visitAssetInput.value = value }
    fun updateVisitIslaInput(value: String) { visitIslaInput.value = value }

    fun toggleDarkTheme(currentActiveIsDark: Boolean) {
        val newValue = !currentActiveIsDark
        prefs.edit().putBoolean("is_dark_theme", newValue).apply()
        _isDarkTheme.value = newValue
    }

    fun saveVenueName(name: String) {
        val trimmed = name.trim()
        prefs.edit().putString("venue_name", trimmed).apply()
        _venueName.value = trimmed
    }

    suspend fun getMachineForAsset(assetOrNum: String): MachineEntity? {
        return repository.findMachine(assetOrNum)
    }

    // --- Deep Link & QR Code Navigation State ---
    private val _deepLinkMachine = MutableStateFlow<MachineEntity?>(null)
    val deepLinkMachine: StateFlow<MachineEntity?> = _deepLinkMachine.asStateFlow()

    private val _targetTabFromDeepLink = MutableStateFlow<Int?>(null)
    val targetTabFromDeepLink: StateFlow<Int?> = _targetTabFromDeepLink.asStateFlow()

    private val _deepLinkSalaMismatchError = MutableStateFlow<String?>(null)
    val deepLinkSalaMismatchError: StateFlow<String?> = _deepLinkSalaMismatchError.asStateFlow()

    fun clearDeepLinkMachine() {
        _deepLinkMachine.value = null
        _targetTabFromDeepLink.value = null
    }

    fun clearDeepLinkSalaMismatchError() {
        _deepLinkSalaMismatchError.value = null
    }

    fun handleDeepLink(uri: android.net.Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            try {
                val paramKey = uri.getQueryParameter("serie")
                    ?: uri.getQueryParameter("serial")
                    ?: uri.getQueryParameter("asset")
                    ?: uri.getQueryParameter("qr")
                    ?: uri.getQueryParameter("qrid")
                    ?: uri.getQueryParameter("id")
                    ?: uri.lastPathSegment?.takeIf { it != "maquina" }

                if (!paramKey.isNullOrBlank()) {
                    // If no machines loaded yet, ensure initial sync is triggered
                    if (repository.getMachineCount() == 0) {
                        syncFromDrive(showProgressMessage = false)
                    }

                    val found = repository.findMachineBySerialOrAssetOrQr(paramKey)
                    if (found != null) {
                        val user = _currentUser.value
                        if (user != null && !user.isAdmin) {
                            // Check if machine belongs to technician's assigned Sala
                            val userSalaNormalized = user.sala.trim().lowercase()
                            val machineSalaNormalized = found.sala.trim().lowercase()
                            val isSameSala = machineSalaNormalized == userSalaNormalized ||
                                (userSalaNormalized.isNotEmpty() && machineSalaNormalized.contains(userSalaNormalized)) ||
                                (machineSalaNormalized.isNotEmpty() && userSalaNormalized.contains(machineSalaNormalized))

                            if (!isSameSala) {
                                _deepLinkSalaMismatchError.value = "Esta máquina pertenece a '${found.sala}'. Tu usuario (${user.nombre}) está asignado a '${user.sala}'. No tienes permisos para acceder a máquinas de otras salas."
                                return@launch
                            }
                        }

                        _deepLinkMachine.value = found
                        _locationSearchQuery.value = found.serialNumber.ifBlank { found.assetNumber }
                        if (_currentUser.value?.isAdmin == true && found.sala.isNotBlank()) {
                            _adminSelectedSala.value = found.sala
                        }
                        _targetTabFromDeepLink.value = 2 // Switch to Tab 'Máquinas'
                    } else {
                        _locationSearchQuery.value = paramKey
                        _targetTabFromDeepLink.value = 2
                        _statusMessage.value = "Máquina con identificador '$paramKey' cargada en la búsqueda."
                    }
                }
            } catch (_: Exception) {
                // Ignore deep link parse errors
            }
        }
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ReportRepository(
            database.machineDao(),
            database.emailReportDao(),
            database.providerEmailDao(),
            database.technicianDao()
        )
        viewModelScope.launch {
            repository.checkAndInitializeDemoData()
            // Automatic initial sync from Google Drive spreadsheet on startup
            syncFromDrive(showProgressMessage = false)
        }
    }

    private fun restoreLoggedUserFromPrefs(): TechnicianEntity? {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (!isLoggedIn) return null

        val userUsuario = prefs.getString("user_usuario", "") ?: ""
        if (userUsuario.isBlank()) return null

        return TechnicianEntity(
            technicianId = prefs.getString("user_technician_id", "") ?: "",
            nombre = prefs.getString("user_nombre", "") ?: "",
            sala = prefs.getString("user_sala", "") ?: "",
            usuario = userUsuario,
            password = "",
            estatus = prefs.getString("user_estatus", "ACTIVO") ?: "ACTIVO",
            rol = prefs.getString("user_rol", "TECNICO") ?: "TECNICO"
        )
    }

    // --- Easter Egg State (10 failed login attempts) ---
    private val _failedAttemptsCount = MutableStateFlow(0)
    val failedAttemptsCount: StateFlow<Int> = _failedAttemptsCount.asStateFlow()

    private val _showGorillaEasterEgg = MutableStateFlow(false)
    val showGorillaEasterEgg: StateFlow<Boolean> = _showGorillaEasterEgg.asStateFlow()

    fun dismissGorillaEasterEgg() {
        _showGorillaEasterEgg.value = false
        _failedAttemptsCount.value = 0
    }

    // --- Authentication Actions ---
    fun login(usuarioInput: String, passwordInput: String) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            _loginErrorMessage.value = null
            try {
                val cleanUser = usuarioInput.trim()
                val cleanPass = passwordInput.trim()

                if (cleanUser.isEmpty() || cleanPass.isEmpty()) {
                    _loginErrorMessage.value = "Por favor ingresa tu usuario y contraseña."
                    return@launch
                }

                // If no technicians in database yet, try a quick sync
                if (repository.getTechnicianCount() == 0) {
                    syncFromDrive(showProgressMessage = false)
                }

                val technician = repository.authenticateTechnician(cleanUser, cleanPass)
                if (technician != null) {
                    if (technician.estatus.trim().uppercase() == "INACTIVO") {
                        _loginErrorMessage.value = "Tu usuario se encuentra inactivo. Contacta al administrador."
                        return@launch
                    }

                    // Reset failed attempts on success
                    _failedAttemptsCount.value = 0
                    _showGorillaEasterEgg.value = false

                    // Save session
                    prefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("user_technician_id", technician.technicianId)
                        .putString("user_nombre", technician.nombre)
                        .putString("user_sala", technician.sala)
                        .putString("user_usuario", technician.usuario)
                        .putString("user_estatus", technician.estatus)
                        .putString("user_rol", technician.rol)
                        .apply()

                    _currentUser.value = technician
                    if (technician.sala.isNotBlank() && !technician.isAdmin) {
                        saveVenueName(technician.sala)
                    }
                    if (technician.nombre.isNotBlank()) {
                        visitTecnico.value = technician.nombre
                    }
                } else {
                    _failedAttemptsCount.value += 1
                    if (_failedAttemptsCount.value >= 10) {
                        _showGorillaEasterEgg.value = true
                    }
                    _loginErrorMessage.value = "Usuario o contraseña incorrectos."
                }
            } catch (e: Exception) {
                _loginErrorMessage.value = "Error al iniciar sesión: ${e.message}"
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("user_technician_id")
            .remove("user_nombre")
            .remove("user_sala")
            .remove("user_usuario")
            .remove("user_estatus")
            .remove("user_rol")
            .apply()

        _currentUser.value = null
        _loginErrorMessage.value = null
    }

    fun clearLoginError() {
        _loginErrorMessage.value = null
    }

    // --- Distinct Salas Stream ---
    val distinctSalas: StateFlow<List<String>> = repository.distinctSalas
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Provider Emails Stream ---
    val providerEmails: StateFlow<List<com.example.data.db.ProviderEmailEntity>> = repository.allProviderEmails
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Admin Selected Sala Filter ---
    private val _adminSelectedSala = MutableStateFlow("")
    val adminSelectedSala: StateFlow<String> = _adminSelectedSala.asStateFlow()

    fun setAdminSelectedSala(sala: String) {
        _adminSelectedSala.value = sala
    }

    // --- Search Queries ---
    private val _locationSearchQuery = MutableStateFlow("")
    val locationSearchQuery: StateFlow<String> = _locationSearchQuery.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    // --- Dynamic Machine Catalog Stream (Filtered by User's Sala or Admin's Selection) ---
    val allMachines: StateFlow<List<MachineEntity>> = repository.allMachines
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val machineCatalog: StateFlow<List<MachineEntity>> = combine(
        _locationSearchQuery.flatMapLatest { query -> repository.searchMachines(query) },
        _currentUser,
        _adminSelectedSala
    ) { machines, user, selectedSala ->
        if (user == null || user.isAdmin) {
            // Admin: Filter by selectedSala if specified and not "TODAS"
            if (selectedSala.isNotBlank() && !selectedSala.equals("TODAS", ignoreCase = true) && !selectedSala.equals("Todas las Salas", ignoreCase = true)) {
                val filterSalaNormalized = selectedSala.trim().lowercase()
                machines.filter { m ->
                    val machineSalaNormalized = m.sala.trim().lowercase()
                    machineSalaNormalized == filterSalaNormalized ||
                        (filterSalaNormalized.isNotEmpty() && machineSalaNormalized.contains(filterSalaNormalized)) ||
                        (machineSalaNormalized.isNotEmpty() && filterSalaNormalized.contains(machineSalaNormalized))
                }
            } else {
                machines
            }
        } else {
            // Regular technicians see ONLY machines belonging to their assigned Sala
            val userSalaNormalized = user.sala.trim().lowercase()
            machines.filter { m ->
                val machineSalaNormalized = m.sala.trim().lowercase()
                machineSalaNormalized == userSalaNormalized ||
                    (userSalaNormalized.isNotEmpty() && machineSalaNormalized.contains(userSalaNormalized)) ||
                    (machineSalaNormalized.isNotEmpty() && userSalaNormalized.contains(machineSalaNormalized))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Dynamic Email History Stream ---
    val reportHistory: StateFlow<List<EmailReportEntity>> = _historySearchQuery
        .flatMapLatest { query -> repository.searchReports(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Active Draft & Dialog State ---
    private val _currentDraft = MutableStateFlow(EmailDraftState())
    val currentDraft: StateFlow<EmailDraftState> = _currentDraft.asStateFlow()

    private val _showDraftDialog = MutableStateFlow(false)
    val showDraftDialog: StateFlow<Boolean> = _showDraftDialog.asStateFlow()

    private val _extractionResult = MutableStateFlow<ExtractedMachineData?>(null)
    val extractionResult: StateFlow<ExtractedMachineData?> = _extractionResult.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _missingProviderEmailState = MutableStateFlow<MissingProviderEmailState?>(null)
    val missingProviderEmailState: StateFlow<MissingProviderEmailState?> = _missingProviderEmailState.asStateFlow()

    fun closeMissingEmailDialog() {
        _missingProviderEmailState.value = null
    }

    fun updateLocationQuery(query: String) {
        _locationSearchQuery.value = query
    }

    fun updateHistoryQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // --- Google Drive Spreadsheet Sync Function ---
    fun syncFromDrive(
        url: String = DriveSyncService.DEFAULT_DRIVE_SHEET_URL,
        showProgressMessage: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingDrive.value = true
            try {
                val bytes = DriveSyncService.downloadSpreadsheetBytes(url)
                if (bytes != null && bytes.isNotEmpty()) {
                    val parsedMachines = FileParserUtil.parseStreamToMachines(bytes.inputStream())
                    val parsedTechnicians = FileParserUtil.parseStreamToTechnicians(bytes.inputStream())

                    if (parsedMachines.isNotEmpty()) {
                        repository.clearAllMachines()
                        repository.importMachineCatalog(parsedMachines)
                    }

                    if (parsedTechnicians.isNotEmpty()) {
                        repository.importTechnicians(parsedTechnicians)

                        // Refresh active session if user data was updated in the cloud
                        val current = _currentUser.value
                        if (current != null) {
                            val refreshed = parsedTechnicians.find {
                                it.usuario.trim().equals(current.usuario.trim(), ignoreCase = true)
                            }
                            if (refreshed != null) {
                                withContext(Dispatchers.Main) {
                                    _currentUser.value = refreshed
                                    if (refreshed.sala.isNotBlank() && !refreshed.isAdmin) {
                                        saveVenueName(refreshed.sala)
                                    }
                                }
                            }
                        }
                    }

                    if (parsedMachines.isNotEmpty() || parsedTechnicians.isNotEmpty()) {
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                        val nowFormatted = dateFormat.format(Date())
                        prefs.edit().putString("last_sync_formatted", nowFormatted).apply()
                        _lastSyncTimestampFormatted.value = nowFormatted

                        val current = _currentUser.value
                        if (current != null && !current.isAdmin) {
                            val userSalaNormalized = current.sala.trim().lowercase()
                            val userMachinesCount = parsedMachines.count { m ->
                                val machineSalaNormalized = m.sala.trim().lowercase()
                                machineSalaNormalized == userSalaNormalized ||
                                    (userSalaNormalized.isNotEmpty() && machineSalaNormalized.contains(userSalaNormalized)) ||
                                    (machineSalaNormalized.isNotEmpty() && userSalaNormalized.contains(machineSalaNormalized))
                            }
                            _statusMessage.value = "Datos actualizados exitosamente ($userMachinesCount máquinas)."
                        } else {
                            _statusMessage.value = "Datos actualizados exitosamente (${parsedMachines.size} máquinas, ${parsedTechnicians.size} técnicos)."
                        }
                    } else {
                        if (showProgressMessage) {
                            _statusMessage.value = "No se pudieron extraer registros válidos de la hoja de cálculo."
                        }
                    }
                } else {
                    if (showProgressMessage) {
                        _statusMessage.value = "No se pudo conectar con Google Drive. Verifique su conexión a internet."
                    }
                }
            } catch (e: Exception) {
                if (showProgressMessage) {
                    _statusMessage.value = "Error al sincronizar con Google Drive: ${e.message}"
                }
            } finally {
                _isSyncingDrive.value = false
            }
        }
    }

    // --- Local Excel File Import Function ---
    fun importFromLocalExcelUri(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingDrive.value = true
            try {
                val userDefaultSala = _currentUser.value?.sala?.trim()?.ifBlank { venueName.value.trim() } ?: venueName.value.trim()
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    inputStream.close()

                    if (bytes.isNotEmpty()) {
                        val parsedMachines = FileParserUtil.parseStreamToMachines(bytes.inputStream(), defaultSala = userDefaultSala)
                        val parsedTechnicians = FileParserUtil.parseStreamToTechnicians(bytes.inputStream())

                        if (parsedMachines.isNotEmpty()) {
                            repository.clearAllMachines()
                            repository.importMachineCatalog(parsedMachines)
                        }

                        if (parsedTechnicians.isNotEmpty()) {
                            repository.importTechnicians(parsedTechnicians)

                            val current = _currentUser.value
                            if (current != null) {
                                val refreshed = parsedTechnicians.find {
                                    it.usuario.trim().equals(current.usuario.trim(), ignoreCase = true)
                                }
                                if (refreshed != null) {
                                    withContext(Dispatchers.Main) {
                                        _currentUser.value = refreshed
                                        if (refreshed.sala.isNotBlank() && !refreshed.isAdmin) {
                                            saveVenueName(refreshed.sala)
                                        }
                                    }
                                }
                            }
                        }

                        if (parsedMachines.isNotEmpty() || parsedTechnicians.isNotEmpty()) {
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                            val nowFormatted = dateFormat.format(Date())
                            prefs.edit().putString("last_sync_formatted", nowFormatted).apply()
                            _lastSyncTimestampFormatted.value = nowFormatted

                            val current = _currentUser.value
                            if (current != null && !current.isAdmin) {
                                val userSalaNormalized = current.sala.trim().lowercase()
                                val userMachinesCount = parsedMachines.count { m ->
                                    val machineSalaNormalized = m.sala.trim().lowercase()
                                    machineSalaNormalized == userSalaNormalized ||
                                        (userSalaNormalized.isNotEmpty() && machineSalaNormalized.contains(userSalaNormalized)) ||
                                        (machineSalaNormalized.isNotEmpty() && userSalaNormalized.contains(machineSalaNormalized))
                                }
                                _statusMessage.value = "Archivo local cargado exitosamente ($userMachinesCount máquinas de tu sala)."
                            } else {
                                _statusMessage.value = "Archivo local cargado exitosamente (${parsedMachines.size} máquinas, ${parsedTechnicians.size} técnicos)."
                            }
                        } else {
                            _statusMessage.value = "No se pudieron extraer registros válidos del archivo Excel seleccionado."
                        }
                    }
                } else {
                    _statusMessage.value = "No se pudo abrir el archivo seleccionado."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error al procesar archivo Excel: ${e.message}"
            } finally {
                _isSyncingDrive.value = false
            }
        }
    }

    fun updateCurrentDraft(
        recipient: String? = null,
        subject: String? = null,
        body: String? = null,
        machineNumber: String? = null,
        issueDescription: String? = null,
        brand: String? = null,
        model: String? = null,
        serialNumber: String? = null,
        assetNumber: String? = null,
        sala: String? = null
    ) {
        _currentDraft.value = _currentDraft.value.copy(
            recipient = recipient ?: _currentDraft.value.recipient,
            subject = subject ?: _currentDraft.value.subject,
            body = body ?: _currentDraft.value.body,
            machineNumber = machineNumber ?: _currentDraft.value.machineNumber,
            issueDescription = issueDescription ?: _currentDraft.value.issueDescription,
            brand = brand ?: _currentDraft.value.brand,
            model = model ?: _currentDraft.value.model,
            serialNumber = serialNumber ?: _currentDraft.value.serialNumber,
            assetNumber = assetNumber ?: _currentDraft.value.assetNumber,
            sala = sala ?: _currentDraft.value.sala
        )
    }

    fun openDraftDialog(draft: EmailDraftState) {
        _currentDraft.value = draft
        _showDraftDialog.value = true
    }

    fun closeDraftDialog() {
        _showDraftDialog.value = false
    }

    // --- Time of Day Greeting helper ---
    fun getTimeOfDayGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    // --- Step 5: Quick Prompt Report Generator ---
    fun generateQuickReport(promptText: String, customRecipient: String = "soporte@zitro.com") {
        viewModelScope.launch {
            val lower = promptText.lowercase().trim()
            val registeredProviders = providerEmails.value

            // 1. Detect provider mentioned in prompt
            var matchedProvider = registeredProviders.find { p ->
                val pName = p.providerName.lowercase().trim()
                pName.length >= 2 && lower.contains(pName)
            }

            if (matchedProvider == null) {
                val brandKeyword = when {
                    lower.contains("zitro") -> "zitro"
                    lower.contains("igt") -> "igt"
                    lower.contains("aristocrat") -> "aristocrat"
                    lower.contains("novomatic") -> "novomatic"
                    lower.contains("konami") -> "konami"
                    lower.contains("bally") -> "bally"
                    lower.contains("ainsworth") -> "ainsworth"
                    lower.contains("egt") -> "egt"
                    else -> ""
                }
                if (brandKeyword.isNotBlank()) {
                    matchedProvider = registeredProviders.find { p ->
                        p.providerName.lowercase().contains(brandKeyword)
                    }
                }
            }

            val finalRecipient = when {
                matchedProvider != null -> matchedProvider.email
                customRecipient.isNotBlank() -> customRecipient
                else -> "soporte@zitro.com"
            }

            // 2. Extract machine number if mentioned
            val machineNumRegex = Regex("""(?:máquina|maquina|asset|terminal|mâquina)\s*#?\s*([a-zA-Z0-9\-]+)""", RegexOption.IGNORE_CASE)
            val numberMatch = machineNumRegex.find(promptText)?.groupValues?.get(1)
                ?: promptText.split(Regex("""\s+""")).firstOrNull { word -> word.all { c -> c.isDigit() } && word.length >= 2 }
                ?: "456"

            // 3. Find machine in database catalog
            val foundMachine = repository.findMachine(numberMatch)

            // 4. Extract issue description cleanly
            val cleanedIssue = cleanIssueDescription(promptText, numberMatch, matchedProvider?.providerName)

            val greeting = getTimeOfDayGreeting()
            val finalBrand = when {
                foundMachine != null && foundMachine.brand.isNotBlank() -> foundMachine.brand
                matchedProvider != null -> matchedProvider.providerName
                else -> "Zitro"
            }
            val finalModel = foundMachine?.model ?: "Estándar"
            val finalSerial = foundMachine?.serialNumber ?: "SN-$numberMatch"
            val finalAsset = foundMachine?.assetNumber ?: numberMatch
            val finalSala = foundMachine?.sala?.ifBlank { null } ?: venueName.value.ifBlank { "Sala Principal" }
            val finalArea = foundMachine?.area ?: "Sala Principal"
            val finalGame = foundMachine?.game?.ifBlank { "General" } ?: "General"
            val formattedBody = buildString {
                appendLine("$greeting estimados, nos podrían apoyar con la revisión y atención de la siguiente terminal, la cual presenta el siguiente inconveniente:")
                appendLine()
                appendLine("Detalle de la falla: $cleanedIssue.")
                appendLine()
                appendLine("--- Datos del equipo ---")
                appendLine("• Sala / Ubicación: $finalSala")
                appendLine("• Marca: $finalBrand")
                appendLine("• Modelo: $finalModel")
                appendLine("• Asset Number: $finalAsset")
                appendLine("• Número de Serie: $finalSerial")
                appendLine("• Área: $finalArea")
                appendLine()
                appendLine("Quedamos a la espera de sus comentarios y apoyo.")
                appendLine()
                append("Saludos cordiales.")
            }

            val subjectLine = "REPORTE DE TERMINAL - $finalSala (ASSET: $finalAsset)"

            val draft = EmailDraftState(
                recipient = finalRecipient,
                subject = subjectLine,
                body = formattedBody,
                machineNumber = numberMatch,
                issueDescription = cleanedIssue,
                brand = finalBrand,
                model = finalModel,
                serialNumber = finalSerial,
                assetNumber = finalAsset,
                sala = finalSala
            )

            if (matchedProvider != null && matchedProvider.email.isBlank()) {
                _missingProviderEmailState.value = MissingProviderEmailState(
                    providerName = matchedProvider.providerName,
                    providerId = matchedProvider.id,
                    draftToOpen = draft.copy(recipient = "")
                )
            } else if (finalRecipient.isBlank()) {
                _missingProviderEmailState.value = MissingProviderEmailState(
                    providerName = finalBrand,
                    providerId = matchedProvider?.id,
                    draftToOpen = draft.copy(recipient = "")
                )
            } else {
                openDraftDialog(draft)
            }
        }
    }

    fun generateReportForMachine(machine: MachineEntity, issueDescription: String) {
        generateReportForMultipleMachines(listOf(machine), issueDescription)
    }

    fun generateReportForMultipleMachines(
        machines: List<MachineEntity>,
        issueDescription: String,
        customRecipient: String = ""
    ) {
        if (machines.isEmpty()) return
        viewModelScope.launch {
            val registeredProviders = providerEmails.value

            // 1. Unify Salas
            val uniqueSalas = machines.map { it.sala.trim() }.filter { it.isNotBlank() }.distinct()
            val finalSala = if (uniqueSalas.isNotEmpty()) {
                uniqueSalas.joinToString(", ")
            } else {
                venueName.value.ifBlank { "Sala Principal" }
            }

            // 2. Unify Brands
            val uniqueBrands = machines.map { it.brand.trim() }.filter { it.isNotBlank() }.distinct()
            val finalBrand = if (uniqueBrands.isNotEmpty()) uniqueBrands.joinToString(", ") else "Zitro"

            // 3. Unify Models
            val uniqueModels = machines.map { it.model.trim() }.filter { it.isNotBlank() }.distinct()
            val finalModel = if (uniqueModels.isNotEmpty()) uniqueModels.joinToString(", ") else "Estándar"

            // 4. Unify Serial Numbers
            val uniqueSerials = machines.map { it.serialNumber.trim() }.filter { it.isNotBlank() }.distinct()
            val finalSerial = if (uniqueSerials.isNotEmpty()) uniqueSerials.joinToString(", ") else "N/A"

            // 5. Unify Assets
            val uniqueAssets = machines.map { it.assetNumber.trim().ifBlank { it.machineNumber.trim() } }.filter { it.isNotBlank() }.distinct()
            val finalAsset = if (uniqueAssets.isNotEmpty()) uniqueAssets.joinToString(", ") else "N/A"

            // 6. Unify Areas
            val uniqueAreas = machines.map { it.area.trim() }.filter { it.isNotBlank() }.distinct()
            val finalArea = if (uniqueAreas.isNotEmpty()) uniqueAreas.joinToString(", ") else "General"

            // 7. Resolve Recipient Emails
            val matchedEmails = mutableListOf<String>()
            for (brand in uniqueBrands) {
                val lowerBrand = brand.lowercase()
                val provider = registeredProviders.find { p ->
                    val pName = p.providerName.lowercase().trim()
                    pName.isNotBlank() && (lowerBrand.contains(pName) || pName.contains(lowerBrand))
                }
                if (provider != null && provider.email.isNotBlank()) {
                    matchedEmails.add(provider.email.trim())
                }
            }

            val finalRecipient = when {
                customRecipient.isNotBlank() -> customRecipient.trim()
                matchedEmails.isNotEmpty() -> matchedEmails.distinct().joinToString(", ")
                else -> ""
            }

            val greeting = getTimeOfDayGreeting()
            val cleanedIssue = issueDescription.trim().ifBlank { "Falla reportada en terminales" }
            val isSingle = machines.size == 1

            val introLine = if (isSingle) {
                "$greeting estimados, nos podrían apoyar con la revisión y atención de la siguiente terminal, la cual presenta el siguiente inconveniente:"
            } else {
                "$greeting estimados, nos podrían apoyar con la revisión y atención de las siguientes terminales, las cuales presentan el siguiente inconveniente:"
            }

            val formattedBody = buildString {
                appendLine(introLine)
                appendLine()
                appendLine("Detalle de la falla: $cleanedIssue.")
                appendLine()
                appendLine(if (isSingle) "--- Datos del equipo ---" else "--- Datos de los equipos ---")
                appendLine("• Sala / Ubicación: $finalSala")
                appendLine("• Marca: $finalBrand")
                appendLine("• Modelo: $finalModel")
                appendLine("• Asset Number: $finalAsset")
                appendLine("• Número de Serie: $finalSerial")
                appendLine("• Área: $finalArea")
                appendLine()
                appendLine("Quedamos a la espera de sus comentarios y apoyo.")
                appendLine()
                append("Saludos cordiales.")
            }

            val subjectLine = if (isSingle) {
                "REPORTE DE TERMINAL - $finalSala (ASSET: $finalAsset)"
            } else {
                "REPORTE DE TERMINALES - $finalSala (ASSETS: $finalAsset)"
            }

            val draft = EmailDraftState(
                recipient = finalRecipient,
                subject = subjectLine,
                body = formattedBody,
                machineNumber = finalAsset,
                issueDescription = cleanedIssue,
                brand = finalBrand,
                model = finalModel,
                serialNumber = finalSerial,
                assetNumber = finalAsset,
                sala = finalSala
            )

            if (finalRecipient.isBlank()) {
                _missingProviderEmailState.value = MissingProviderEmailState(
                    providerName = if (uniqueBrands.isNotEmpty()) uniqueBrands.first() else "Proveedor",
                    providerId = null,
                    draftToOpen = draft
                )
            } else {
                openDraftDialog(draft)
            }
        }
    }

    private fun cleanIssueDescription(promptText: String, numberMatch: String, matchedProviderName: String?): String {
        var text = promptText.trim()

        val actionRegex = Regex("""^(?:reporta|reportar|reporte|falla\s+en|falla\s+de|favor\s+de\s+reportar|revisar|revision|atender)\s+""", RegexOption.IGNORE_CASE)
        var modified = true
        while (modified) {
            val newText = text.replace(actionRegex, "").trim()
            modified = (newText != text)
            text = newText
        }

        if (numberMatch.isNotBlank()) {
            text = text.replace(Regex("""\b(?:la\s+)?(?:máquina|maquina|terminal|asset|equipo|num|número|#)\s*#?\s*""" + Regex.escape(numberMatch) + """\b""", RegexOption.IGNORE_CASE), "").trim()
            text = text.replace(Regex("""\b""" + Regex.escape(numberMatch) + """\b"""), "").trim()
        }

        if (!matchedProviderName.isNullOrBlank()) {
            text = text.replace(Regex("""\b""" + Regex.escape(matchedProviderName) + """\b""", RegexOption.IGNORE_CASE), "").trim()
        }
        text = text.replace(Regex("""\b(?:zitro|igt|aristocrat|novomatic|konami|bally|ainsworth|egt)\b""", RegexOption.IGNORE_CASE), "").trim()
        text = text.replace(Regex("""^(?:la\s+)?(?:máquina|maquina|terminal|asset|equipo)\b\s*""", RegexOption.IGNORE_CASE), "").trim()
        text = text.replace(Regex("""^(?:a|por|para|de|con|en)\s+""", RegexOption.IGNORE_CASE), "").trim()

        text = text.replace(Regex("""\s+"""), " ")
            .removePrefix(".").removePrefix(",").removePrefix(":").removePrefix("-").trim()

        if (text.isBlank()) {
            return "Presenta falla en el funcionamiento"
        }

        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    // --- Step 4: History Persistence ---
    fun saveDraftToHistory() {
        viewModelScope.launch {
            val draft = _currentDraft.value
            if (draft.body.isNotBlank()) {
                val reportEntity = EmailReportEntity(
                    recipient = draft.recipient,
                    subject = draft.subject,
                    body = draft.body,
                    machineNumber = draft.machineNumber,
                    issueDescription = draft.issueDescription,
                    brand = draft.brand,
                    model = draft.model,
                    serialNumber = draft.serialNumber,
                    assetNumber = draft.assetNumber,
                    timestamp = System.currentTimeMillis(),
                    status = "Enviado"
                )
                repository.saveReport(reportEntity)
                _statusMessage.value = "Correo guardado en el historial de reportes."
            }
        }
    }

    fun deleteHistoryReport(id: Int) {
        viewModelScope.launch {
            repository.deleteReport(id)
            _statusMessage.value = "Reporte eliminado del historial."
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllReports()
            _statusMessage.value = "Historial limpiado completamente."
        }
    }

    fun addManualMachine(machine: MachineEntity) {
        viewModelScope.launch {
            repository.insertMachine(machine)
            _statusMessage.value = "Máquina ${machine.machineNumber} guardada en el catálogo."
        }
    }

    // --- Provider Email Management ---
    fun saveProviderEmail(id: Int = 0, providerName: String, email: String) {
        viewModelScope.launch {
            if (providerName.isNotBlank()) {
                repository.insertProviderEmail(
                    com.example.data.db.ProviderEmailEntity(
                        id = id,
                        providerName = providerName.trim(),
                        email = email.trim()
                    )
                )
                val emailInfo = if (email.isNotBlank()) " (${email.trim()})" else ""
                _statusMessage.value = if (id == 0) {
                    "Proveedor ${providerName.trim()}$emailInfo guardado correctamente."
                } else {
                    "Proveedor ${providerName.trim()}$emailInfo actualizado correctamente."
                }
            }
        }
    }

    fun addProviderEmail(providerName: String, email: String) {
        saveProviderEmail(0, providerName, email)
    }

    fun deleteProviderEmail(id: Int) {
        viewModelScope.launch {
            repository.deleteProviderEmail(id)
            _statusMessage.value = "Correo de proveedor eliminado."
        }
    }

    fun restoreDefaultProviders() {
        viewModelScope.launch {
            repository.restoreDemoProviders()
            _statusMessage.value = "Lista de proveedores restablecida."
        }
    }
}
