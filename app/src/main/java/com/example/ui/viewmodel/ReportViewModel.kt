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
import com.example.data.remote.IncidenciaItem
import com.example.data.repository.ReportRepository
import com.example.util.FileParserUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class EmailDraftState(
    val recipient: String = "",
    val cc: String = "",
    val subject: String = "",
    val body: String = "",
    val machineNumber: String = "",
    val issueDescription: String = "",
    val brand: String = "",
    val model: String = "",
    val serialNumber: String = "",
    val assetNumber: String = "",
    val sala: String = "",
    val area: String = "",
    val propietario: String = "",
    val ticketId: String? = null,
    val operativa: String = "NO",
    val prioridad: String = "MEDIA"
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
    val visitTecnico = MutableStateFlow("")
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
                        if (_currentUser.value?.isAdmin == true && found.sala.isNotBlank()) {
                            _adminSelectedSala.value = found.sala
                        }
                    } else {
                        _statusMessage.value = "Máquina con identificador '$paramKey' no encontrada en el catálogo."
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
            idSala = prefs.getString("user_id_sala", "") ?: "",
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
                        .putString("user_id_sala", technician.idSala)
                        .putString("user_sala", technician.sala)
                        .putString("user_usuario", technician.usuario)
                        .putString("user_estatus", technician.estatus)
                        .putString("user_rol", technician.rol)
                        .apply()

                    _currentUser.value = technician
                    if (technician.sala.isNotBlank() && !technician.isAdmin) {
                        saveVenueName(technician.sala)
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

    // --- Brand Filter State ---
    private val _selectedBrandFilter = MutableStateFlow("TODAS")
    val selectedBrandFilter: StateFlow<String> = _selectedBrandFilter.asStateFlow()

    fun updateSelectedBrandFilter(brand: String) {
        _selectedBrandFilter.value = brand
    }

    // Direct machine selection for report from Machine Card
    private val _requestedMachineForReport = MutableStateFlow<MachineEntity?>(null)
    val requestedMachineForReport: StateFlow<MachineEntity?> = _requestedMachineForReport.asStateFlow()

    fun requestReportForMachine(machine: MachineEntity) {
        _requestedMachineForReport.value = machine
        _targetTabFromDeepLink.value = 0 // Switch to 'Generar' tab
    }

    fun clearRequestedMachineForReport() {
        _requestedMachineForReport.value = null
    }

    val machineCatalog: StateFlow<List<MachineEntity>> = combine(
        _locationSearchQuery.flatMapLatest { query -> repository.searchMachines(query) },
        _currentUser,
        _adminSelectedSala,
        _selectedBrandFilter
    ) { machines, user, selectedSala, brandFilter ->
        val salaFiltered = if (user == null || user.isAdmin) {
            if (selectedSala.isNotBlank() && !selectedSala.equals("TODAS", ignoreCase = true) && !selectedSala.equals("Todas las Salas", ignoreCase = true)) {
                val filterSalaNormalized = selectedSala.trim().lowercase()
                machines.filter { m ->
                    val machineSalaNormalized = m.sala.trim().lowercase()
                    machineSalaNormalized == filterSalaNormalized ||
                        (filterSalaNormalized.isNotEmpty() && machineSalaNormalized.contains(filterSalaNormalized)) ||
                        (machineSalaNormalized.isNotEmpty() && userSalaNormalized(filterSalaNormalized, machineSalaNormalized))
                }
            } else {
                machines
            }
        } else {
            val userSalaNormalized = user.sala.trim().lowercase()
            machines.filter { m ->
                val machineSalaNormalized = m.sala.trim().lowercase()
                machineSalaNormalized == userSalaNormalized ||
                    (userSalaNormalized.isNotEmpty() && machineSalaNormalized.contains(userSalaNormalized)) ||
                    (machineSalaNormalized.isNotEmpty() && userSalaNormalized.contains(machineSalaNormalized))
            }
        }

        if (brandFilter.isNotBlank() && !brandFilter.equals("TODAS", ignoreCase = true)) {
            salaFiltered.filter { it.brand.trim().equals(brandFilter.trim(), ignoreCase = true) }
        } else {
            salaFiltered
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unfilteredUserMachines: StateFlow<List<MachineEntity>> = combine(
        allMachines,
        _currentUser
    ) { machines, user ->
        if (user == null || user.isAdmin) {
            machines
        } else {
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

    private fun userSalaNormalized(filter: String, target: String): Boolean {
        return target.contains(filter)
    }

    val distinctBrands: StateFlow<List<String>> = allMachines.map { machines ->
        listOf("TODAS") + machines.map { it.brand.trim().uppercase() }.filter { it.isNotBlank() }.distinct().sorted()
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("TODAS")
    )

    // --- Dynamic Incidencias Stream (From Google Sheets) ---
    private val _rawIncidencias = MutableStateFlow<List<IncidenciaItem>>(loadCachedIncidencias())
    val rawIncidencias: StateFlow<List<IncidenciaItem>> = _rawIncidencias.asStateFlow()

    // --- Available Salas for Admin (from Machines DB + Incidencias from Drive) ---
    val availableSalas: StateFlow<List<String>> = combine(
        repository.distinctSalas,
        _rawIncidencias
    ) { dbSalas, incs ->
        val fromIncs = incs.map { it.sala.trim() }.filter { it.isNotBlank() }
        val combined = (listOf("TODAS") + dbSalas.map { it.trim() } + fromIncs)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        combined
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("TODAS")
    )

    private val _incidenciasSearchQuery = MutableStateFlow("")
    val incidenciasSearchQuery: StateFlow<String> = _incidenciasSearchQuery.asStateFlow()

    fun updateIncidenciasSearchQuery(query: String) {
        _incidenciasSearchQuery.value = query
    }

    private val _selectedIncidenciaEstadoFilter = MutableStateFlow("TODOS")
    val selectedIncidenciaEstadoFilter: StateFlow<String> = _selectedIncidenciaEstadoFilter.asStateFlow()

    fun updateSelectedIncidenciaEstadoFilter(estado: String) {
        _selectedIncidenciaEstadoFilter.value = estado
    }

    // Stream de todas las incidencias de la sala activa (sin filtros de estado ni búsqueda, para KPIs estables)
    val salaIncidenciasList: StateFlow<List<IncidenciaItem>> = combine(
        _rawIncidencias,
        _currentUser,
        _adminSelectedSala
    ) { allInc, user, adminSala ->
        if (user == null || user.isAdmin) {
            if (adminSala.isNotBlank() && !adminSala.equals("TODAS", ignoreCase = true) && !adminSala.equals("Todas las Salas", ignoreCase = true)) {
                val filterSalaNorm = adminSala.trim().lowercase()
                allInc.filter { inc ->
                    val incSalaNorm = inc.sala.trim().lowercase()
                    incSalaNorm == filterSalaNorm || incSalaNorm.replace(" ", "") == filterSalaNorm.replace(" ", "")
                }
            } else {
                allInc
            }
        } else {
            val userSalaNorm = user.sala.trim().lowercase()
            allInc.filter { inc ->
                val incSalaNorm = inc.sala.trim().lowercase()
                userSalaNorm.isEmpty() || incSalaNorm == userSalaNorm || incSalaNorm.replace(" ", "") == userSalaNorm.replace(" ", "")
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Stream de incidencias filtrado para la lista según estado seleccionado y búsqueda
    val incidenciasList: StateFlow<List<IncidenciaItem>> = combine(
        salaIncidenciasList,
        _incidenciasSearchQuery,
        _selectedIncidenciaEstadoFilter
    ) { salaFiltered, query, estadoFilter ->
        // 1. Filtrar por Estado / KPI seleccionado
        val estadoFiltered = when (estadoFilter.trim().uppercase()) {
            "FUERA_SERVICIO", "FUERA SERVICIO", "FUERA DE SERVICIO", "INOPERATIVO", "INOPERATIVA" -> salaFiltered.filter {
                val st = it.estadoTicket.uppercase()
                it.operativa.equals("NO", ignoreCase = true) && !st.contains("RESUELT") && !st.contains("CERRAD")
            }
            "PENDIENTES", "PENDIENTE", "ABIERTO", "EN PROCESO" -> salaFiltered.filter {
                val st = it.estadoTicket.uppercase()
                !st.contains("RESUELT") && !st.contains("CERRAD")
            }
            "EN_SERVICIO", "EN SERVICIO", "OPERATIVO", "OPERATIVA", "OPERATIVAS" -> salaFiltered.filter {
                val st = it.estadoTicket.uppercase()
                it.operativa.equals("SI", ignoreCase = true) && !st.contains("RESUELT") && !st.contains("CERRAD")
            }
            "CRITICAS", "CRÍTICAS", "CRITICA", "CRÍTICA", "ALTA", "ALTAS" -> salaFiltered.filter {
                val st = it.estadoTicket.uppercase()
                (it.prioridad.equals("CRITICA", true) || it.prioridad.equals("ALTA", true)) && !st.contains("RESUELT") && !st.contains("CERRAD")
            }
            "RESUELTOS", "RESUELTO", "CERRADO", "CERRADOS" -> salaFiltered.filter {
                val st = it.estadoTicket.uppercase()
                st.contains("RESUELT") || st.contains("CERRAD")
            }
            "TODOS", "TOTAL" -> salaFiltered
            else -> salaFiltered
        }

        // 2. Filtrar por Búsqueda de Texto
        if (query.isBlank()) {
            estadoFiltered
        } else {
            val q = query.trim().lowercase()
            estadoFiltered.filter { inc ->
                inc.idTicket.lowercase().contains(q) ||
                inc.asset.lowercase().contains(q) ||
                inc.serie.lowercase().contains(q) ||
                inc.marca.lowercase().contains(q) ||
                inc.modelo.lowercase().contains(q) ||
                inc.falla.lowercase().contains(q) ||
                inc.area.lowercase().contains(q) ||
                inc.tecnico.lowercase().contains(q) ||
                inc.sala.lowercase().contains(q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun saveCachedIncidencias(list: List<IncidenciaItem>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject()
                obj.put("idTicket", item.idTicket)
                obj.put("sala", item.sala)
                obj.put("marca", item.marca)
                obj.put("modelo", item.modelo)
                obj.put("serie", item.serie)
                obj.put("asset", item.asset)
                obj.put("area", item.area)
                obj.put("propietario", item.propietario)
                obj.put("operativa", item.operativa)
                obj.put("estadoTicket", item.estadoTicket)
                obj.put("fechaOrigen", item.fechaOrigen)
                obj.put("fechaReparacion", item.fechaReparacion)
                obj.put("falla", item.falla)
                obj.put("prioridad", item.prioridad)
                obj.put("idTecnico", item.idTecnico)
                obj.put("tecnico", item.tecnico)
                obj.put("resolucion", item.resolucion)
                array.put(obj)
            }
            prefs.edit().putString("cached_incidencias_json", array.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun parseIncidenciaDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        val formats = listOf(
            "d/M/yyyy H:m:s",
            "d/M/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm:ss",
            "d/M/yyyy",
            "dd/MM/yyyy",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                val d = sdf.parse(dateStr.trim())
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return 0L
    }

    private fun sortIncidenciasByMostRecent(list: List<IncidenciaItem>): List<IncidenciaItem> {
        return list.asReversed().sortedWith(
            compareByDescending { parseIncidenciaDateToMillis(it.fechaOrigen) }
        )
    }

    private fun loadCachedIncidencias(): List<IncidenciaItem> {
        val json = prefs.getString("cached_incidencias_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val result = mutableListOf<IncidenciaItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    IncidenciaItem(
                        idTicket = obj.optString("idTicket", ""),
                        sala = obj.optString("sala", ""),
                        marca = obj.optString("marca", ""),
                        modelo = obj.optString("modelo", ""),
                        serie = obj.optString("serie", ""),
                        asset = obj.optString("asset", ""),
                        area = obj.optString("area", ""),
                        propietario = obj.optString("propietario", "WINPOT"),
                        operativa = obj.optString("operativa", "NO"),
                        estadoTicket = obj.optString("estadoTicket", "PENDIENTE"),
                        fechaOrigen = obj.optString("fechaOrigen", ""),
                        fechaReparacion = obj.optString("fechaReparacion", ""),
                        falla = obj.optString("falla", ""),
                        prioridad = obj.optString("prioridad", "MEDIA"),
                        idTecnico = obj.optString("idTecnico", ""),
                        tecnico = obj.optString("tecnico", ""),
                        resolucion = obj.optString("resolucion", "")
                    )
                )
            }
            sortIncidenciasByMostRecent(result)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun updateIncidenciaStatus(
        idTicket: String,
        nuevoEstado: String,
        operativa: String,
        resolucion: String,
        fechaReparacion: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null || (!user.isSuperUser && !user.isAdmin)) {
            onComplete(false, "Permisos insuficientes. Se requiere rol de Superusuario.")
            return
        }

        viewModelScope.launch {
            val result = com.example.data.remote.GoogleSheetsUpdateService.updateIncidenciaStatus(
                idTicket = idTicket,
                nuevoEstado = nuevoEstado,
                operativa = operativa,
                resolucion = resolucion,
                fechaReparacion = fechaReparacion
            )

            result.fold(
                onSuccess = { msg ->
                    val currentList = _rawIncidencias.value.toMutableList()
                    val idx = currentList.indexOfFirst { it.idTicket.equals(idTicket.trim(), ignoreCase = true) }
                    if (idx != -1) {
                        val old = currentList[idx]
                        currentList[idx] = old.copy(
                            estadoTicket = nuevoEstado,
                            operativa = operativa,
                            resolucion = resolucion,
                            fechaReparacion = if (nuevoEstado.contains("RESUELT", true)) fechaReparacion.ifBlank { old.fechaReparacion } else ""
                        )
                        _rawIncidencias.value = sortIncidenciasByMostRecent(currentList)
                        saveCachedIncidencias(_rawIncidencias.value)
                    }
                    _statusMessage.value = msg
                    onComplete(true, msg)
                    syncFromDrive(showProgressMessage = false)
                },
                onFailure = { err ->
                    val errMsg = err.message ?: "Error desconocido al actualizar en Google Sheets"
                    _statusMessage.value = errMsg
                    onComplete(false, errMsg)
                }
            )
        }
    }

    // --- Dynamic Email History Stream (Filtered by Technician's Sala) ---
    val reportHistory: StateFlow<List<EmailReportEntity>> = combine(
        _historySearchQuery.flatMapLatest { query -> repository.searchReports(query) },
        _currentUser,
        _adminSelectedSala
    ) { reports, user, adminSala ->
        if (user == null || user.isAdmin) {
            if (adminSala.isNotBlank() && !adminSala.equals("TODAS", ignoreCase = true) && !adminSala.equals("Todas las Salas", ignoreCase = true)) {
                val filterSalaNorm = adminSala.trim().lowercase()
                reports.filter { r ->
                    val combinedText = "${r.subject} ${r.body}".lowercase()
                    combinedText.contains(filterSalaNorm)
                }
            } else {
                reports
            }
        } else {
            val userSalaNorm = user.sala.trim().lowercase()
            if (userSalaNorm.isBlank()) {
                reports
            } else {
                reports.filter { r ->
                    val combinedText = "${r.subject} ${r.body}".lowercase()
                    combinedText.contains(userSalaNorm)
                }
            }
        }
    }.stateIn(
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
        showProgressMessage: Boolean = true,
        forceSyncMachines: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingDrive.value = true
            try {
                val bytes = DriveSyncService.downloadSpreadsheetBytes(url)
                if (bytes != null && bytes.isNotEmpty()) {
                    val parsedMachines = FileParserUtil.parseStreamToMachines(bytes.inputStream())
                    val parsedTechnicians = FileParserUtil.parseStreamToTechnicians(bytes.inputStream())
                    val parsedIncidencias = FileParserUtil.parseStreamToIncidencias(bytes.inputStream())
                    val parsedProviders = FileParserUtil.parseStreamToProviderEmails(bytes.inputStream())
                    if (parsedProviders.isNotEmpty()) {
                        repository.importProviderEmails(parsedProviders)
                    }

                    withContext(Dispatchers.Main) {
                        val sortedIncidencias = sortIncidenciasByMostRecent(parsedIncidencias)
                        _rawIncidencias.value = sortedIncidencias
                        saveCachedIncidencias(sortedIncidencias)
                    }

                    val hasLocalOverride = prefs.getBoolean("has_local_file_override", false)
                    val localMachineCount = repository.getMachineCount()

                    // Merge and complement machines without destroying existing non-empty fields like island,
                    // while purging machines that were removed from the catalog
                    if (parsedMachines.isNotEmpty()) {
                        repository.mergeAndImportMachines(parsedMachines, replaceOld = true)
                        if (forceSyncMachines) {
                            prefs.edit().putBoolean("has_local_file_override", false).apply()
                        }
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

                        _statusMessage.value = "Datos actualizados exitosamente"
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
                        val parsedIncidencias = FileParserUtil.parseStreamToIncidencias(bytes.inputStream())
                        val parsedProviders = FileParserUtil.parseStreamToProviderEmails(bytes.inputStream())
                        if (parsedProviders.isNotEmpty()) {
                            repository.importProviderEmails(parsedProviders)
                        }

                        withContext(Dispatchers.Main) {
                            val sortedIncidencias = sortIncidenciasByMostRecent(parsedIncidencias)
                            _rawIncidencias.value = sortedIncidencias
                            saveCachedIncidencias(sortedIncidencias)
                        }

                        if (parsedMachines.isNotEmpty()) {
                            repository.mergeAndImportMachines(parsedMachines, replaceOld = true)
                            // Mark local override so startup sync doesn't overwrite it
                            prefs.edit().putBoolean("has_local_file_override", true).apply()
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

                            _statusMessage.value = "Datos actualizados exitosamente"
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

    fun updateCurrentDraft(draft: EmailDraftState) {
        _currentDraft.value = draft
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
        sala: String? = null,
        operativa: String? = null,
        prioridad: String? = null
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
            sala = sala ?: _currentDraft.value.sala,
            operativa = operativa ?: _currentDraft.value.operativa,
            prioridad = prioridad ?: _currentDraft.value.prioridad
        )
    }

    fun openDraftDialog(draft: EmailDraftState) {
        _currentDraft.value = draft
        lastSavedDraftFingerprint = null
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

    // --- Provider Brand Matching Helper (AGS / CADILLAC JACK Equivalence) ---
    fun findProviderForBrand(
        brandName: String,
        registeredProviders: List<com.example.data.db.ProviderEmailEntity> = providerEmails.value
    ): com.example.data.db.ProviderEmailEntity? {
        val b = brandName.trim().lowercase()
        if (b.isBlank()) return null

        // AGS and CADILLAC JACK equivalence:
        // Use CADILLAC JACK emails for both AGS and CADILLAC JACK (fallback to AGS if CADILLAC JACK isn't found)
        val isAgsOrCadillac = b == "ags" || b.contains("ags") || b.contains("cadillac") || b.contains("cadillac jack")
        if (isAgsOrCadillac) {
            val cadillac = registeredProviders.find { p ->
                val pName = p.providerName.trim().lowercase()
                pName.contains("cadillac")
            }
            if (cadillac != null) return cadillac

            val ags = registeredProviders.find { p ->
                val pName = p.providerName.trim().lowercase()
                pName == "ags" || pName.contains("ags")
            }
            if (ags != null) return ags
        }

        val cleanB = b.replace(Regex("""[^a-z0-9]"""), "")
        return registeredProviders.find { p ->
            val pName = p.providerName.trim().lowercase()
            val cleanP = pName.replace(Regex("""[^a-z0-9]"""), "")
            pName.isNotBlank() && (b == pName || cleanB == cleanP || b.contains(pName) || pName.contains(b))
        }
    }

    // --- Provider Machine Matching Helper (Priority 1: Propietario, Priority 2: Brand) ---
    fun findProviderForMachine(
        machine: MachineEntity,
        registeredProviders: List<com.example.data.db.ProviderEmailEntity> = providerEmails.value
    ): com.example.data.db.ProviderEmailEntity? {
        // Priority 1: Match by machine's propietario (e.g. ZITRO, WINPOT, AGS)
        if (machine.propietario.isNotBlank()) {
            val byPropietario = findProviderForBrand(machine.propietario, registeredProviders)
            if (byPropietario != null && byPropietario.email.isNotBlank()) {
                return byPropietario
            }
        }

        // Priority 2: Fallback to machine's brand (e.g. Zitro, IGT, Cadillac Jack)
        if (machine.brand.isNotBlank()) {
            return findProviderForBrand(machine.brand, registeredProviders)
        }

        return null
    }

    // --- Step 5: Quick Prompt Report Generator ---
    fun generateQuickReport(
        promptText: String,
        customRecipient: String = "soporte@zitro.com",
        operativa: String = "NO",
        prioridad: String = "MEDIA"
    ) {
        viewModelScope.launch {
            val lower = promptText.lowercase().trim()
            val registeredProviders = providerEmails.value

            // 1. Detect provider mentioned in prompt
            var matchedProvider = if (lower.contains("ags") || lower.contains("cadillac")) {
                findProviderForBrand("CADILLAC JACK", registeredProviders)
            } else {
                registeredProviders.find { p ->
                    val pName = p.providerName.lowercase().trim()
                    pName.length >= 2 && lower.contains(pName)
                }
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
                    lower.contains("ags") || lower.contains("cadillac") -> "cadillac jack"
                    else -> ""
                }
                if (brandKeyword.isNotBlank()) {
                    matchedProvider = findProviderForBrand(brandKeyword, registeredProviders)
                }
            }

            // 2. Extract machine number if mentioned
            val machineNumRegex = Regex("""(?:máquina|maquina|asset|terminal|mâquina)\s*#?\s*([a-zA-Z0-9\-]+)""", RegexOption.IGNORE_CASE)
            val numberMatch = machineNumRegex.find(promptText)?.groupValues?.get(1)
                ?: promptText.split(Regex("""\s+""")).firstOrNull { word -> word.all { c -> c.isDigit() } && word.length >= 2 }
                ?: "456"

            // 3. Find machine in database catalog
            val foundMachine = repository.findMachine(numberMatch)

            if (foundMachine != null) {
                val machineProvider = findProviderForMachine(foundMachine, registeredProviders)
                if (machineProvider != null) {
                    matchedProvider = machineProvider
                }
            }

            val finalRecipient = when {
                matchedProvider != null && matchedProvider.email.isNotBlank() -> matchedProvider.email
                customRecipient.isNotBlank() -> customRecipient
                else -> "soporte@zitro.com"
            }
            val finalCc = matchedProvider?.ccEmails ?: ""

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
            val finalPropietario = foundMachine?.propietario?.ifBlank { "WINPOT" } ?: "WINPOT"
            val ticketId = com.example.util.TicketIdGenerator.generateTicketId(finalSala, finalSerial)

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
                cc = finalCc,
                subject = subjectLine,
                body = formattedBody,
                machineNumber = numberMatch,
                issueDescription = cleanedIssue,
                brand = finalBrand,
                model = finalModel,
                serialNumber = finalSerial,
                assetNumber = finalAsset,
                sala = finalSala,
                area = finalArea,
                propietario = finalPropietario,
                ticketId = ticketId,
                operativa = operativa,
                prioridad = prioridad
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

    fun generateReportForMachine(
        machine: MachineEntity,
        issueDescription: String,
        operativa: String = "NO",
        prioridad: String = "MEDIA"
    ) {
        generateReportForMultipleMachines(
            machines = listOf(machine),
            issueDescription = issueDescription,
            operativa = operativa,
            prioridad = prioridad
        )
    }

    fun generateReportForMultipleMachines(
        machines: List<MachineEntity>,
        issueDescription: String,
        customRecipient: String = "",
        operativa: String = "NO",
        prioridad: String = "MEDIA"
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

            // 7. Resolve Recipient Emails (Priority 1: Propietario, Priority 2: Brand)
            val matchedEmails = mutableListOf<String>()
            val matchedCcEmails = mutableListOf<String>()
            for (machine in machines) {
                val provider = findProviderForMachine(machine, registeredProviders)
                if (provider != null) {
                    if (provider.email.isNotBlank()) {
                        matchedEmails.add(provider.email.trim())
                    }
                    if (provider.ccEmails.isNotBlank()) {
                        val ccs = provider.ccEmails.split(',', ';').map { it.trim() }.filter { it.isNotBlank() }
                        matchedCcEmails.addAll(ccs)
                    }
                }
            }

            val finalRecipient = when {
                customRecipient.isNotBlank() -> customRecipient.trim()
                matchedEmails.isNotEmpty() -> matchedEmails.distinct().joinToString(", ")
                else -> ""
            }
            val finalCc = matchedCcEmails.distinct().joinToString(", ")

            val greeting = getTimeOfDayGreeting()
            val cleanedIssue = issueDescription.trim().ifBlank { "Falla reportada en terminales" }
            val isSingle = machines.size == 1
            val finalPropietario = machines.map { it.propietario.trim() }.firstOrNull { it.isNotBlank() } ?: "WINPOT"
            val ticketId = if (isSingle) com.example.util.TicketIdGenerator.generateTicketId(finalSala, finalSerial) else null

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
                cc = finalCc,
                subject = subjectLine,
                body = formattedBody,
                machineNumber = finalAsset,
                issueDescription = cleanedIssue,
                brand = finalBrand,
                model = finalModel,
                serialNumber = finalSerial,
                assetNumber = finalAsset,
                sala = finalSala,
                area = finalArea,
                propietario = finalPropietario,
                ticketId = ticketId,
                operativa = operativa,
                prioridad = prioridad
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
        text = text.replace(Regex("""\b(?:zitro|igt|aristocrat|novomatic|konami|bally|ainsworth|egt|ags|cadillac|cadillac\s+jack)\b""", RegexOption.IGNORE_CASE), "").trim()
        text = text.replace(Regex("""^(?:la\s+)?(?:máquina|maquina|terminal|asset|equipo)\b\s*""", RegexOption.IGNORE_CASE), "").trim()
        text = text.replace(Regex("""^(?:a|por|para|de|con|en)\s+""", RegexOption.IGNORE_CASE), "").trim()

        text = text.replace(Regex("""\s+"""), " ")
            .removePrefix(".").removePrefix(",").removePrefix(":").removePrefix("-").trim()

        if (text.isBlank()) {
            return "Presenta falla en el funcionamiento"
        }

        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    // --- Step 4: History Persistence & Drive Sheet Sync ---
    private val dispatchedTicketIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private var lastSavedDraftFingerprint: String? = null

    val incidenciasWebhookUrl = MutableStateFlow(
        prefs.getString("incidencias_webhook_url", com.example.data.remote.DriveSyncService.DEFAULT_INCIDENCIAS_WEBHOOK_URL)
            ?: com.example.data.remote.DriveSyncService.DEFAULT_INCIDENCIAS_WEBHOOK_URL
    )

    fun updateIncidenciasWebhookUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit().putString("incidencias_webhook_url", trimmed).apply()
        incidenciasWebhookUrl.value = trimmed
        com.example.data.remote.DriveSyncService.customWebhookUrl = trimmed.ifBlank {
            com.example.data.remote.DriveSyncService.DEFAULT_INCIDENCIAS_WEBHOOK_URL
        }
    }

    fun dispatchIncidenciaToDriveSheet(draft: EmailDraftState = _currentDraft.value) {
        val finalSala = draft.sala.ifBlank { venueName.value.ifBlank { "Sala Principal" } }
        val finalSerial = draft.serialNumber.ifBlank { "SN-${draft.machineNumber.ifBlank { "PENDIENTE" }}" }
        val ticketId = draft.ticketId?.ifBlank { null }
            ?: com.example.util.TicketIdGenerator.generateTicketId(finalSala, finalSerial)

        if (dispatchedTicketIds.contains(ticketId)) {
            // Prevenir duplicidad en Google Sheets si ya fue despachado
            return
        }
        dispatchedTicketIds.add(ticketId)

        viewModelScope.launch {
            val user = _currentUser.value
            val payload = com.example.data.remote.IncidenciaTicketPayload(
                idTicket = ticketId,
                sala = finalSala,
                marca = draft.brand,
                modelo = draft.model,
                serie = finalSerial,
                asset = draft.assetNumber,
                area = draft.area,
                propietario = draft.propietario.ifBlank { "WINPOT" },
                idTecnico = user?.technicianId ?: "",
                tecnico = user?.nombre ?: "",
                falla = draft.issueDescription,
                operativa = draft.operativa.ifBlank { "NO" },
                prioridad = draft.prioridad.ifBlank { "MEDIA" }
            )
            val configuredUrl = prefs.getString("incidencias_webhook_url", "")?.trim().orEmpty()
            com.example.data.remote.DriveSyncService.customWebhookUrl = configuredUrl.ifBlank {
                com.example.data.remote.DriveSyncService.DEFAULT_INCIDENCIAS_WEBHOOK_URL
            }
            val success = com.example.data.remote.DriveSyncService.postIncidenciaToDriveSheet(payload)
            if (success) {
                _statusMessage.value = "Incidencia registrada en Google Sheets ($ticketId)."
                // Sincronizar automáticamente para reflejar la nueva incidencia en la app y KPIs de inmediato
                syncFromDrive(showProgressMessage = false)
            } else {
                _statusMessage.value = "Ticket $ticketId guardado localmente."
            }
        }
    }

    fun saveDraftToHistory(dispatchToSheets: Boolean = false) {
        viewModelScope.launch {
            val draft = _currentDraft.value
            if (draft.body.isNotBlank()) {
                val currentFingerprint = "${draft.ticketId ?: ""}_${draft.subject}_${draft.machineNumber}"
                if (lastSavedDraftFingerprint == currentFingerprint) {
                    // Prevenir doble inserción en el historial si el usuario envía y luego guarda
                    if (dispatchToSheets) {
                        dispatchIncidenciaToDriveSheet(draft)
                    }
                    return@launch
                }
                lastSavedDraftFingerprint = currentFingerprint

                val reportEntity = EmailReportEntity(
                    recipient = draft.recipient,
                    cc = draft.cc,
                    subject = draft.subject,
                    body = draft.body,
                    machineNumber = draft.machineNumber,
                    issueDescription = draft.issueDescription,
                    brand = draft.brand,
                    model = draft.model,
                    serialNumber = draft.serialNumber,
                    assetNumber = draft.assetNumber,
                    timestamp = System.currentTimeMillis(),
                    status = if (dispatchToSheets) "Enviado" else "Borrador Local"
                )
                repository.saveReport(reportEntity)
                _statusMessage.value = "Correo guardado en el historial de reportes."

                // Solo si el usuario despachó por correo (dispatchToSheets = true), registrar en Google Sheets
                if (dispatchToSheets) {
                    dispatchIncidenciaToDriveSheet(draft)
                }
            }
        }
    }

    fun sendAndDispatchEmailReport() {
        saveDraftToHistory(dispatchToSheets = true)
    }

    fun saveVisitToHistory(
        sala: String,
        fecha: String,
        proveedor: String,
        tecnico: String,
        horaEntrada: String,
        horaSalida: String,
        motivoVisita: String,
        asset: String,
        isla: String,
        fullText: String
    ) {
        viewModelScope.launch {
            if (fullText.isNotBlank()) {
                val assetOrIsla = buildString {
                    if (asset.isNotBlank()) append(asset.trim())
                    if (isla.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append("Isla ${isla.trim()}")
                    }
                }.ifBlank { "N/A" }

                val visitReport = EmailReportEntity(
                    recipient = "WhatsApp",
                    subject = "Visita Técnica - ${sala.trim().ifBlank { "Corporativo" }} - ${proveedor.trim().ifBlank { "General" }}",
                    body = fullText,
                    machineNumber = assetOrIsla,
                    issueDescription = motivoVisita.trim().ifBlank { "Servicio / Mantenimiento de Visita" },
                    brand = proveedor.trim().ifBlank { "General" },
                    model = if (isla.isNotBlank()) "Isla $isla" else "",
                    serialNumber = if (tecnico.isNotBlank()) "Téc: ${tecnico.trim()}" else "",
                    assetNumber = asset.trim(),
                    timestamp = System.currentTimeMillis(),
                    status = "Enviado WhatsApp"
                )
                repository.saveReport(visitReport)
                _statusMessage.value = "Visita registrada y guardada en el historial."
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
    fun saveProviderEmail(id: Int = 0, providerName: String, email: String, ccEmails: String = "") {
        viewModelScope.launch {
            if (providerName.isNotBlank()) {
                repository.insertProviderEmail(
                    com.example.data.db.ProviderEmailEntity(
                        id = id,
                        providerName = providerName.trim(),
                        email = email.trim(),
                        ccEmails = ccEmails.trim()
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

    fun addProviderEmail(providerName: String, email: String, ccEmails: String = "") {
        saveProviderEmail(0, providerName, email, ccEmails)
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
