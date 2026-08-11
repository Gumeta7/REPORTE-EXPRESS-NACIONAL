package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ExtractedMachineData
import com.example.data.api.GeminiExtractionService
import com.example.data.db.AppDatabase
import com.example.data.db.EmailReportEntity
import com.example.data.db.MachineEntity
import com.example.data.repository.ReportRepository
import com.example.util.FileParserUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class EmailDraftState(
    val recipient: String = "",
    val subject: String = "",
    val body: String = "",
    val machineNumber: String = "",
    val issueDescription: String = "",
    val brand: String = "",
    val model: String = "",
    val serialNumber: String = "",
    val assetNumber: String = ""
)

class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReportRepository
    private val prefs = application.getSharedPreferences("reportes_express_prefs", android.content.Context.MODE_PRIVATE)

    private val _venueName = MutableStateFlow(prefs.getString("venue_name", "") ?: "")
    val venueName: StateFlow<String> = _venueName.asStateFlow()

    private val _isDarkTheme = MutableStateFlow<Boolean?>(
        if (prefs.contains("is_dark_theme")) prefs.getBoolean("is_dark_theme", false) else null
    )
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

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

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ReportRepository(
            database.machineDao(),
            database.emailReportDao(),
            database.providerEmailDao()
        )
        viewModelScope.launch {
            repository.checkAndInitializeDemoData()
        }
    }

    // --- Provider Emails Stream ---
    val providerEmails: StateFlow<List<com.example.data.db.ProviderEmailEntity>> = repository.allProviderEmails
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Search Queries ---
    private val _locationSearchQuery = MutableStateFlow("")
    val locationSearchQuery: StateFlow<String> = _locationSearchQuery.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    // --- Dynamic Machine Catalog Stream ---
    val machineCatalog: StateFlow<List<MachineEntity>> = _locationSearchQuery
        .flatMapLatest { query -> repository.searchMachines(query) }
        .stateIn(
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

    private val _isExtractingFile = MutableStateFlow(false)
    val isExtractingFile: StateFlow<Boolean> = _isExtractingFile.asStateFlow()

    private val _extractionResult = MutableStateFlow<ExtractedMachineData?>(null)
    val extractionResult: StateFlow<ExtractedMachineData?> = _extractionResult.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun updateLocationQuery(query: String) {
        _locationSearchQuery.value = query
    }

    fun updateHistoryQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
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
        assetNumber: String? = null
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
            assetNumber = assetNumber ?: _currentDraft.value.assetNumber
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

            // 2. Extract machine number if mentioned (e.g., "maquina 473", "473", "asset 1025")
            val machineNumRegex = Regex("""(?:máquina|maquina|asset|terminal|mâquina)\s*#?\s*([a-zA-Z0-9\-]+)""", RegexOption.IGNORE_CASE)
            val numberMatch = machineNumRegex.find(promptText)?.groupValues?.get(1)
                ?: promptText.split(Regex("""\s+""")).firstOrNull { word -> word.all { c -> c.isDigit() } && word.length >= 2 }
                ?: "473"

            // 3. Find machine in database catalog
            val foundMachine = repository.findMachine(numberMatch)

            // 4. Extract issue description cleanly without repeating prompt phrases
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
            val finalArea = foundMachine?.area ?: "Sala Principal"
            val finalIsland = foundMachine?.island ?: "Isla 01"
            val finalGame = foundMachine?.game ?: "General"

            val formattedBody = """
                $greeting estimados,
                
                Nos podrían apoyar con la revisión y atención de la siguiente terminal, la cual presenta el siguiente inconveniente:
                
                Detalle de la falla: $cleanedIssue.
                
                --- DATOS DEL EQUIPO ---
                • Asset Number: $finalAsset
                • Número de Serie: $finalSerial
                • Marca / Proveedor: $finalBrand
                • Modelo: $finalModel
                • Área / Ubicación: $finalArea
                • Juego Instalado: $finalGame
                
                Quedamos a la espera de sus comentarios y apoyo.
                
                Saludos cordiales.
            """.trimIndent()

            val subjectLine = "REPORTE DE TERMINAL"

            val draft = EmailDraftState(
                recipient = finalRecipient,
                subject = subjectLine,
                body = formattedBody,
                machineNumber = numberMatch,
                issueDescription = cleanedIssue,
                brand = finalBrand,
                model = finalModel,
                serialNumber = finalSerial,
                assetNumber = finalAsset
            )

            openDraftDialog(draft)
        }
    }

    fun generateReportForMachine(machine: MachineEntity, issueDescription: String) {
        viewModelScope.launch {
            val registeredProviders = providerEmails.value
            val lowerBrand = machine.brand.lowercase().trim()

            val matchedProvider = registeredProviders.find { p ->
                val pName = p.providerName.lowercase().trim()
                pName.isNotBlank() && (lowerBrand.contains(pName) || pName.contains(lowerBrand))
            }

            val finalRecipient = matchedProvider?.email
                ?: "soporte@${if (machine.brand.isNotBlank()) machine.brand.lowercase().replace(" ", "") else "zitro"}.com"

            val greeting = getTimeOfDayGreeting()
            val cleanedIssue = issueDescription.trim().ifBlank { "Falla reportada en terminal" }

            val formattedBody = """
                $greeting estimados,
                
                Nos podrían apoyar con la revisión y atención de la siguiente terminal, la cual presenta el siguiente inconveniente:
                
                Detalle de la falla: $cleanedIssue.
                
                --- DATOS DEL EQUIPO ---
                • Asset Number: ${machine.assetNumber}
                • Número de Serie: ${machine.serialNumber}
                • Marca / Proveedor: ${machine.brand}
                • Modelo: ${machine.model}
                • Área / Ubicación: ${machine.area} (Isla: ${machine.island})
                • Juego Instalado: ${machine.game}
                
                Quedamos a la espera de sus comentarios y apoyo.
                
                Saludos cordiales.
            """.trimIndent()

            val subjectLine = "REPORTE DE TERMINAL - MAQ ${machine.machineNumber}"

            val draft = EmailDraftState(
                recipient = finalRecipient,
                subject = subjectLine,
                body = formattedBody,
                machineNumber = machine.machineNumber,
                issueDescription = cleanedIssue,
                brand = machine.brand,
                model = machine.model,
                serialNumber = machine.serialNumber,
                assetNumber = machine.assetNumber
            )

            openDraftDialog(draft)
        }
    }

    private fun cleanIssueDescription(promptText: String, numberMatch: String, matchedProviderName: String?): String {
        var text = promptText.trim()

        // 1. Remove action verbs at beginning
        val actionRegex = Regex("""^(?:reporta|reportar|reporte|falla\s+en|falla\s+de|favor\s+de\s+reportar|revisar|revision|atender)\s+""", RegexOption.IGNORE_CASE)
        var modified = true
        while (modified) {
            val newText = text.replace(actionRegex, "").trim()
            modified = (newText != text)
            text = newText
        }

        // 2. Remove machine reference and number
        if (numberMatch.isNotBlank()) {
            text = text.replace(Regex("""\b(?:la\s+)?(?:máquina|maquina|terminal|asset|equipo|num|número|#)\s*#?\s*""" + Regex.escape(numberMatch) + """\b""", RegexOption.IGNORE_CASE), "").trim()
            text = text.replace(Regex("""\b""" + Regex.escape(numberMatch) + """\b"""), "").trim()
        }

        // 3. Remove provider name if present
        if (!matchedProviderName.isNullOrBlank()) {
            text = text.replace(Regex("""\b""" + Regex.escape(matchedProviderName) + """\b""", RegexOption.IGNORE_CASE), "").trim()
        }
        text = text.replace(Regex("""\b(?:zitro|igt|aristocrat|novomatic|konami|bally)\b""", RegexOption.IGNORE_CASE), "").trim()

        // 4. Remove residual machine words at beginning
        text = text.replace(Regex("""^(?:la\s+)?(?:máquina|maquina|terminal|asset|equipo)\b\s*""", RegexOption.IGNORE_CASE), "").trim()

        // 5. Remove leading connectors/prepositions ("a", "por", "para", "de", "con", "en", "el", "la") at beginning
        text = text.replace(Regex("""^(?:a|por|para|de|con|en)\s+""", RegexOption.IGNORE_CASE), "").trim()

        // 6. Clean punctuation & extra whitespaces
        text = text.replace(Regex("""\s+"""), " ")
            .removePrefix(".").removePrefix(",").removePrefix(":").removePrefix("-").trim()

        if (text.isBlank()) {
            return "Presenta falla en el funcionamiento"
        }

        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    // --- Stream-based Excel / CSV Catalog Import ---
    fun importMachinesFromStream(inputStream: java.io.InputStream) {
        viewModelScope.launch {
            try {
                val machines = FileParserUtil.parseStreamToMachines(inputStream)
                if (machines.isNotEmpty()) {
                    repository.importMachineCatalog(machines)
                    _statusMessage.value = "Se importaron ${machines.size} máquinas del archivo Excel/CSV al catálogo."
                } else {
                    _statusMessage.value = "No se encontraron máquinas válidas en la hoja de cálculo."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error al procesar la hoja de cálculo: ${e.message}"
            }
        }
    }

    fun extractDataFromStream(inputStream: java.io.InputStream, isCsvCatalog: Boolean = false) {
        viewModelScope.launch {
            _isExtractingFile.value = true
            try {
                if (isCsvCatalog) {
                    val machines = FileParserUtil.parseStreamToMachines(inputStream)
                    if (machines.isNotEmpty()) {
                        repository.importMachineCatalog(machines)
                        _statusMessage.value = "Se importaron ${machines.size} máquinas al catálogo correctamente."
                    }
                    return@launch
                }

                val extractedText = FileParserUtil.extractTextFromStream(inputStream)
                extractDataFromTextOrFile(extractedText, isCsvCatalog = false)
            } catch (e: Exception) {
                _statusMessage.value = "Error al procesar archivo: ${e.message}"
            } finally {
                _isExtractingFile.value = false
            }
        }
    }

    // --- Step 1: File Data Extraction ---
    fun extractDataFromTextOrFile(fileContent: String, isCsvCatalog: Boolean = false) {
        viewModelScope.launch {
            _isExtractingFile.value = true
            try {
                if (isCsvCatalog || fileContent.contains(",") && fileContent.contains("\n")) {
                    val machines = FileParserUtil.parseCsvToMachines(fileContent)
                    if (machines.isNotEmpty()) {
                        repository.importMachineCatalog(machines)
                        _statusMessage.value = "Se importaron ${machines.size} máquinas al catálogo correctamente."
                    }
                }

                // Call Gemini / Fallback to extract 4 key points: Serial, Brand, Model, Asset
                val result = GeminiExtractionService.extractDataFromTextOrDocument(fileContent)
                _extractionResult.value = result

                val greeting = getTimeOfDayGreeting()
                val machineNum = result.machineNumber.ifBlank { "N/A" }
                val issue = result.issueDescription.ifBlank { "Falla reportada en archivo adjunto" }

                val generatedBody = """
                    $greeting estimados,
                    
                    Nos podrían apoyar con la revisión y atención de la siguiente terminal, la cual presenta el siguiente inconveniente:
                    
                    Detalle de la falla: $issue.
                    
                    --- DATOS EXTRAÍDOS ---
                    • Asset Number: ${result.assetNumber.ifBlank { "AST-EXTRAIDO" }}
                    • Número de Serie: ${result.serialNumber.ifBlank { "SN-DESCONOCIDO" }}
                    • Marca: ${result.brand.ifBlank { "N/A" }}
                    • Modelo: ${result.model.ifBlank { "N/A" }}
                    
                    Quedamos a la espera de sus comentarios y apoyo.
                    
                    Saludos cordiales.
                """.trimIndent()

                val draft = EmailDraftState(
                    recipient = "soporte@casino.com",
                    subject = "REPORTE DE TERMINAL",
                    body = generatedBody,
                    machineNumber = machineNum,
                    issueDescription = issue,
                    brand = result.brand,
                    model = result.model,
                    serialNumber = result.serialNumber,
                    assetNumber = result.assetNumber
                )

                _currentDraft.value = draft
            } catch (e: Exception) {
                _statusMessage.value = "Error extrayendo datos: ${e.message}"
            } finally {
                _isExtractingFile.value = false
            }
        }
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

    // --- Machine Catalog Deletion / Management ---
    fun clearMachineCatalog() {
        viewModelScope.launch {
            repository.clearAllMachines()
            _statusMessage.value = "Base del catálogo de máquinas eliminada completamente."
        }
    }

    fun restoreDemoMachines() {
        viewModelScope.launch {
            repository.restoreDemoMachines()
            _statusMessage.value = "Catálogo de ejemplo restablecido."
        }
    }

    // --- Provider Email Management ---
    fun saveProviderEmail(id: Int = 0, providerName: String, email: String) {
        viewModelScope.launch {
            if (providerName.isNotBlank() && email.isNotBlank()) {
                repository.insertProviderEmail(
                    com.example.data.db.ProviderEmailEntity(
                        id = id,
                        providerName = providerName.trim(),
                        email = email.trim()
                    )
                )
                _statusMessage.value = if (id == 0) {
                    "Proveedor ${providerName.trim()} ($email) guardado correctamente."
                } else {
                    "Proveedor ${providerName.trim()} ($email) actualizado correctamente."
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
