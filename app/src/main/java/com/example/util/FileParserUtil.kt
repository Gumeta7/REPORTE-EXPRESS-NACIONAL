package com.example.util

import com.example.data.db.MachineEntity
import com.example.data.db.ProviderEmailEntity
import com.example.data.db.TechnicianEntity
import com.example.data.remote.IncidenciaItem
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

object FileParserUtil {

    fun parseStreamToMachines(inputStream: InputStream, defaultSala: String = ""): List<MachineEntity> {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) return emptyList()

        // 1. Try parsing with Apache POI as Excel (.xls or .xlsx)
        try {
            bytes.inputStream().use { stream ->
                val workbook = WorkbookFactory.create(stream)
                val allMachines = mutableListOf<MachineEntity>()

                // Identify sheets to process: target specifically sheet named "maquinas" (ignoring accents/case)
                val targetSheetIndices = mutableListOf<Int>()
                for (sheetIndex in 0 until workbook.numberOfSheets) {
                    val rawName = workbook.getSheetName(sheetIndex).trim()
                    val normalizedName = rawName.lowercase()
                        .replace("á", "a")
                        .replace("é", "e")
                        .replace("í", "i")
                        .replace("ó", "o")
                        .replace("ú", "u")
                    if (normalizedName == "maquinas" || normalizedName == "maquina") {
                        targetSheetIndices.add(sheetIndex)
                    }
                }

                // If 'maquinas' sheet was found, process ONLY that sheet; otherwise fallback to all sheets
                val sheetsToProcess = if (targetSheetIndices.isNotEmpty()) {
                    targetSheetIndices
                } else {
                    (0 until workbook.numberOfSheets).toList()
                }

                for (sheetIndex in sheetsToProcess) {
                    val sheet = workbook.getSheetAt(sheetIndex)
                    if ((sheet == null) || (sheet.physicalNumberOfRows == 0)) continue

                    // Find header row in first 20 rows
                    var headerRowIndex = -1
                    var columnIndices = emptyMap<String, Int>()

                    for (r in 0..minOf(20, sheet.lastRowNum)) {
                        val row = sheet.getRow(r) ?: continue
                        val rowCells = (0 until row.lastCellNum).map { c ->
                            getCellValueAsString(row.getCell(c))
                        }
                        val map = findHeaderIndices(rowCells)
                        if (map.containsKey("asset") || map.containsKey("serie") || map.containsKey("marca") || map.containsKey("sala") || map.containsKey("maquina") || map.containsKey("modelo")) {
                            headerRowIndex = r
                            columnIndices = map
                            break
                        }
                    }

                    val startRow = if (headerRowIndex != -1) headerRowIndex + 1 else 0
                    val isHeaderFound = headerRowIndex != -1

                    for (r in startRow..sheet.lastRowNum) {
                        val row = sheet.getRow(r) ?: continue
                        fun getVal(key: String, fallbackCol: Int): String {
                            val idx = columnIndices[key] ?: if (isHeaderFound) -1 else fallbackCol
                            if (idx < 0) return ""
                            val cell = row.getCell(idx) ?: return ""
                            return getCellValueAsString(cell).trim()
                        }

                        val asset = cleanNumericString(getVal("asset", 0))
                        val salaRaw = getVal("sala", -1)
                        val sala = salaRaw.ifBlank { defaultSala }
                        val marca = getVal("marca", 1)
                        val modelo = getVal("modelo", 2)
                        val juego = getVal("juego", -1)
                        val area = getVal("area", 4)
                        val isla = cleanNumericString(getVal("isla", -1))
                        val serie = getVal("serie", 6)
                        val propietario = getVal("propietario", -1)
                        val qrId = getVal("qrid", -1)
                        val maquina = cleanNumericString(getVal("maquina", -1)).ifBlank { asset }.ifBlank { serie }

                        if (maquina.isNotBlank() || asset.isNotBlank() || serie.isNotBlank() || marca.isNotBlank()) {
                            allMachines.add(
                                MachineEntity(
                                    machineNumber = maquina.ifBlank { "M-${allMachines.size + 1}" },
                                    brand = marca.ifBlank { "General" },
                                    model = modelo.ifBlank { "Estándar" },
                                    serialNumber = serie.ifBlank { if (maquina.isNotBlank()) "SN-$maquina" else "SN-DESCONOCIDO" },
                                    assetNumber = asset.ifBlank { maquina },
                                    area = area.ifBlank { "Sala Principal" },
                                    game = juego,
                                    island = isla,
                                    sala = sala,
                                    qrId = qrId,
                                    propietario = propietario
                                )
                            )
                        }
                    }
                }
                workbook.close()
                if (allMachines.isNotEmpty()) {
                    return allMachines
                }
            }
        } catch (_: Throwable) {
            // POI error fallback to CSV
        }

        val text = String(bytes, Charsets.UTF_8)
        return parseCsvToMachines(text, defaultSala)
    }

    fun parseStreamToTechnicians(inputStream: InputStream): List<TechnicianEntity> {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) return emptyList()

        try {
            bytes.inputStream().use { stream ->
                val workbook = WorkbookFactory.create(stream)
                val allTechnicians = mutableListOf<TechnicianEntity>()

                // Locate sheet named "tecnicos" or "técnicos"
                var targetSheetIndex = -1
                for (sheetIndex in 0 until workbook.numberOfSheets) {
                    val rawName = workbook.getSheetName(sheetIndex).trim()
                    val normalizedName = rawName.lowercase()
                        .replace("á", "a")
                        .replace("é", "e")
                        .replace("í", "i")
                        .replace("ó", "o")
                        .replace("ú", "u")
                    if (normalizedName == "tecnicos" || normalizedName == "tecnico" || normalizedName == "usuarios") {
                        targetSheetIndex = sheetIndex
                        break
                    }
                }

                if (targetSheetIndex != -1) {
                    val sheet = workbook.getSheetAt(targetSheetIndex)
                    if (sheet != null && sheet.physicalNumberOfRows > 0) {
                        var headerRowIndex = -1
                        var colMap = emptyMap<String, Int>()

                        for (r in 0..minOf(15, sheet.lastRowNum)) {
                            val row = sheet.getRow(r) ?: continue
                            val rowCells = (0 until row.lastCellNum).map { c ->
                                getCellValueAsString(row.getCell(c))
                            }
                            val map = findTechnicianHeaderIndices(rowCells)
                            if (map.containsKey("usuario") || map.containsKey("nombre")) {
                                headerRowIndex = r
                                colMap = map
                                break
                            }
                        }

                        val startRow = if (headerRowIndex != -1) headerRowIndex + 1 else 0

                        for (r in startRow..sheet.lastRowNum) {
                            val row = sheet.getRow(r) ?: continue
                            fun getVal(key: String): String {
                                val idx = colMap[key] ?: return ""
                                if (idx < 0) return ""
                                val cell = row.getCell(idx) ?: return ""
                                return getCellValueAsString(cell).trim()
                            }

                            val idTecnico = getVal("id_tecnico")
                            val nombre = getVal("nombre")
                            val idSala = getVal("id_sala")
                            val sala = getVal("sala")
                            val usuario = getVal("usuario")
                            val password = getVal("password")
                            val estatus = getVal("estatus").ifBlank { "ACTIVO" }
                            val rol = getVal("rol").ifBlank { "TECNICO" }

                            if (usuario.isNotBlank() || nombre.isNotBlank()) {
                                allTechnicians.add(
                                    TechnicianEntity(
                                        technicianId = idTecnico,
                                        nombre = nombre,
                                        idSala = idSala,
                                        sala = sala,
                                        usuario = usuario,
                                        password = password,
                                        estatus = estatus,
                                        rol = rol
                                    )
                                )
                            }
                        }
                    }
                }

                workbook.close()
                return allTechnicians
            }
        } catch (_: Throwable) {
            return emptyList()
        }
    }

    fun parseStreamToIncidencias(inputStream: InputStream): List<IncidenciaItem> {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) return emptyList()

        try {
            bytes.inputStream().use { stream ->
                val workbook = WorkbookFactory.create(stream)
                val allIncidencias = mutableListOf<IncidenciaItem>()

                // Locate sheet named "incidencias" or "incidencia"
                var targetSheetIndex = -1
                for (sheetIndex in 0 until workbook.numberOfSheets) {
                    val rawName = workbook.getSheetName(sheetIndex).trim().lowercase()
                        .replace("á", "a")
                        .replace("é", "e")
                        .replace("í", "i")
                        .replace("ó", "o")
                        .replace("ú", "u")
                    if (rawName == "incidencias" || rawName == "incidencia") {
                        targetSheetIndex = sheetIndex
                        break
                    }
                }

                if (targetSheetIndex != -1) {
                    val sheet = workbook.getSheetAt(targetSheetIndex)
                    if (sheet != null && sheet.physicalNumberOfRows > 0) {
                        var headerRowIndex = -1
                        var colMap = emptyMap<String, Int>()

                        for (r in 0..minOf(15, sheet.lastRowNum)) {
                            val row = sheet.getRow(r) ?: continue
                            val rowCells = (0 until row.lastCellNum).map { c ->
                                getCellValueAsString(row.getCell(c))
                            }
                            val map = findIncidenciaHeaderIndices(rowCells)
                            if (map.containsKey("id_ticket") || map.containsKey("falla") || map.containsKey("sala")) {
                                headerRowIndex = r
                                colMap = map
                                break
                            }
                        }

                        val startRow = if (headerRowIndex != -1) headerRowIndex + 1 else 1

                        for (r in startRow..sheet.lastRowNum) {
                            val row = sheet.getRow(r) ?: continue
                            fun getVal(key: String): String {
                                val idx = colMap[key] ?: return ""
                                if (idx < 0) return ""
                                val cell = row.getCell(idx) ?: return ""
                                return getCellValueAsString(cell).trim()
                            }

                            val idTicket = getVal("id_ticket")
                            val sala = getVal("sala")
                            val marca = getVal("marca")
                            val modelo = getVal("modelo")
                            val serie = getVal("serie")
                            val asset = getVal("asset")
                            val area = getVal("area")
                            val propietario = getVal("propietario").ifBlank { "WINPOT" }
                            val operativa = getVal("operativa").uppercase().ifBlank { "NO" }
                            val estadoTicket = getVal("estado_ticket").uppercase().ifBlank { "PENDIENTE" }
                            val fechaOrigen = getVal("fecha_origen")
                            val fechaReparacion = getVal("fecha_reparacion")
                            val falla = getVal("falla")
                            val prioridad = getVal("prioridad").uppercase().ifBlank { "MEDIA" }
                            val idTecnico = getVal("id_tecnico")
                            val tecnico = getVal("tecnico")
                            val resolucion = getVal("resolucion")

                            if (idTicket.isNotBlank() || falla.isNotBlank() || asset.isNotBlank() || serie.isNotBlank()) {
                                allIncidencias.add(
                                    IncidenciaItem(
                                        idTicket = idTicket.ifBlank { "INC-$r" },
                                        sala = sala,
                                        marca = marca,
                                        modelo = modelo,
                                        serie = serie,
                                        asset = asset,
                                        area = area,
                                        propietario = propietario,
                                        operativa = operativa,
                                        estadoTicket = estadoTicket,
                                        fechaOrigen = fechaOrigen,
                                        fechaReparacion = fechaReparacion,
                                        falla = falla,
                                        prioridad = prioridad,
                                        idTecnico = idTecnico,
                                        tecnico = tecnico,
                                        resolucion = resolucion
                                    )
                                )
                            }
                        }
                    }
                }

                workbook.close()
                return allIncidencias
            }
        } catch (_: Throwable) {
            return emptyList()
        }
    }

    fun parseStreamToProviderEmails(inputStream: InputStream): List<ProviderEmailEntity> {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) return emptyList()

        try {
            bytes.inputStream().use { stream ->
                val workbook = WorkbookFactory.create(stream)
                val allProviders = mutableListOf<ProviderEmailEntity>()

                // Locate sheet named "propietario", "propietarios", "proveedor", "proveedores"
                var targetSheetIndex = -1
                for (sheetIndex in 0 until workbook.numberOfSheets) {
                    val rawName = workbook.getSheetName(sheetIndex).trim().lowercase()
                        .replace("á", "a")
                        .replace("é", "e")
                        .replace("í", "i")
                        .replace("ó", "o")
                        .replace("ú", "u")
                    if (rawName == "propietario" || rawName == "propietarios" || rawName == "proveedor" || rawName == "proveedores") {
                        targetSheetIndex = sheetIndex
                        break
                    }
                }

                if (targetSheetIndex != -1) {
                    val sheet = workbook.getSheetAt(targetSheetIndex)
                    if (sheet != null && sheet.physicalNumberOfRows > 0) {
                        var headerRowIndex = -1
                        var proveedorCol = -1
                        var correo1Col = -1
                        val ccCols = mutableListOf<Int>()

                        for (r in 0..minOf(15, sheet.lastRowNum)) {
                            val row = sheet.getRow(r) ?: continue
                            for (c in 0 until row.lastCellNum) {
                                val rawHeader = getCellValueAsString(row.getCell(c))
                                val col = sanitizeHeader(rawHeader)
                                val colNoSpaces = col.replace(" ", "")

                                if (col.contains("PROVEEDOR") || col.contains("MARCA") || col == "PROPIETARIO") {
                                    proveedorCol = c
                                } else if (colNoSpaces == "CORREO1" || colNoSpaces == "EMAIL1" || col == "CORREO 1" || col == "EMAIL 1" || (col == "CORREO" && correo1Col == -1)) {
                                    correo1Col = c
                                } else if (Regex("""^(?:CORREO|EMAIL)\s*([2-9]|\d{2,}).*$""").matches(col) || col.contains("CC") || col.contains("COPIA")) {
                                    ccCols.add(c)
                                }
                            }
                            if (proveedorCol != -1 && (correo1Col != -1 || ccCols.isNotEmpty())) {
                                headerRowIndex = r
                                break
                            }
                        }

                        val startRow = if (headerRowIndex != -1) headerRowIndex + 1 else 0
                        for (r in startRow..sheet.lastRowNum) {
                            val row = sheet.getRow(r) ?: continue
                            val providerName = if (proveedorCol != -1) getCellValueAsString(row.getCell(proveedorCol)).trim() else ""
                            if (providerName.isBlank()) continue

                            val email1 = if (correo1Col != -1) getCellValueAsString(row.getCell(correo1Col)).trim() else ""
                            val ccList = mutableListOf<String>()
                            for (col in ccCols) {
                                val ccVal = getCellValueAsString(row.getCell(col)).trim()
                                if (ccVal.isNotBlank()) {
                                    // In case cell has multiple emails separated by comma or semicolon
                                    val parts = ccVal.split(',', ';').map { it.trim() }.filter { it.isNotBlank() }
                                    ccList.addAll(parts)
                                }
                            }

                            if (email1.isNotBlank() || ccList.isNotEmpty()) {
                                allProviders.add(
                                    ProviderEmailEntity(
                                        providerName = providerName,
                                        email = email1,
                                        ccEmails = ccList.distinct().joinToString(", ")
                                    )
                                )
                            }
                        }
                    }
                }

                workbook.close()
                return allProviders
            }
        } catch (_: Throwable) {
            return emptyList()
        }
    }

    private fun findIncidenciaHeaderIndices(rowCells: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        rowCells.forEachIndexed { idx, cellStr ->
            val col = sanitizeHeader(cellStr)
            when {
                col.contains("ESTADO") || col.contains("STATUS") || col.contains("ESTATUS") -> map.putIfAbsent("estado_ticket", idx)
                col.contains("ID_TICKET") || col.contains("ID TICKET") || (col.contains("TICKET") && !col.contains("ESTADO") && !col.contains("STATUS")) || col == "ID" -> map.putIfAbsent("id_ticket", idx)
                col.contains("SALA") || col.contains("CASINO") -> map.putIfAbsent("sala", idx)
                col.contains("MARCA") -> map.putIfAbsent("marca", idx)
                col.contains("MODELO") -> map.putIfAbsent("modelo", idx)
                col.contains("SERIE") || col.contains("SERIAL") -> map.putIfAbsent("serie", idx)
                col.contains("ASSET") || col.contains("ACTIVO") -> map.putIfAbsent("asset", idx)
                col.contains("AREA") || col.contains("ZONA") -> map.putIfAbsent("area", idx)
                col.contains("PROPIETARIO") || col.contains("OPERADOR") -> map.putIfAbsent("propietario", idx)
                col.contains("OPERATIVA") -> map.putIfAbsent("operativa", idx)
                col.contains("ORIGEN") || col.contains("FECHA_ORIGEN") || (col.contains("FECHA") && !col.contains("REPARACION")) -> map.putIfAbsent("fecha_origen", idx)
                col.contains("REPARACION") -> map.putIfAbsent("fecha_reparacion", idx)
                col.contains("FALLA") || col.contains("DESCRIPCION") || col.contains("MOTIVO") -> map.putIfAbsent("falla", idx)
                col.contains("PRIORIDAD") -> map.putIfAbsent("prioridad", idx)
                col.contains("ID_TECNICO") || col.contains("ID TECNICO") -> map.putIfAbsent("id_tecnico", idx)
                col == "TECNICO" || (col.contains("TECNICO") && !col.contains("ID")) -> map.putIfAbsent("tecnico", idx)
                col.contains("RESOLUCION") || col.contains("SOLUCION") -> map.putIfAbsent("resolucion", idx)
            }
        }
        return map
    }

    private fun findTechnicianHeaderIndices(rowCells: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        rowCells.forEachIndexed { idx, cellStr ->
            val col = sanitizeHeader(cellStr)
            when {
                col.contains("ID_SALA") || col.contains("ID SALA") || col == "IDSALA" -> map.putIfAbsent("id_sala", idx)
                col.contains("ID_TECNICO") || col.contains("ID TECNICO") || (col == "ID" && !map.containsKey("id_tecnico")) -> map.putIfAbsent("id_tecnico", idx)
                col.contains("NOMBRE") || col.contains("TECNICO") || col.contains("NAME") -> map.putIfAbsent("nombre", idx)
                (col.contains("SALA") || col.contains("CASINO") || col.contains("UBICACION")) && !col.contains("ID_SALA") && !col.contains("ID SALA") && col != "IDSALA" -> map.putIfAbsent("sala", idx)
                col.contains("USUARIO") || col.contains("USER") || col.contains("LOGIN") -> map.putIfAbsent("usuario", idx)
                col.contains("CONTRASE") || col.contains("PASSWORD") || col.contains("CLAVE") || col.contains("PASS") -> map.putIfAbsent("password", idx)
                col.contains("ESTATUS") || col.contains("STATUS") || col.contains("ESTADO") -> map.putIfAbsent("estatus", idx)
                col.contains("ROL") || col.contains("ROLE") || col.contains("PERFIL") || col.contains("NIVEL") -> map.putIfAbsent("rol", idx)
            }
        }
        return map
    }

    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue.trim()
                CellType.NUMERIC -> {
                    val num = cell.numericCellValue
                    if (num == num.toLong().toDouble()) {
                        num.toLong().toString()
                    } else {
                        num.toString()
                    }
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> {
                    try {
                        when (cell.cachedFormulaResultType) {
                            CellType.NUMERIC -> {
                                val num = cell.numericCellValue
                                if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
                            }
                            CellType.STRING -> cell.stringCellValue.trim()
                            CellType.BOOLEAN -> cell.booleanCellValue.toString()
                            else -> ""
                        }
                    } catch (_: Exception) {
                        ""
                    }
                }
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun cleanNumericString(str: String): String {
        val trimmed = str.trim()
        if (trimmed.endsWith(".0") && trimmed.substring(0, trimmed.length - 2).all { it.isDigit() }) {
            return trimmed.substring(0, trimmed.length - 2)
        }
        return trimmed
    }

    private fun findHeaderIndices(rowCells: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()

        // 1. Highest Priority for 'modelo': Explicit 'MODELO REPORTE', 'MODELO_REPORTE', 'MODELO DE REPORTE', 'PP/PV'
        rowCells.forEachIndexed { idx, cellStr ->
            val col = sanitizeHeader(cellStr)
            if (col.contains("MODELO REPORTE") || col.contains("MODELO_REPORTE") || col.contains("MODELO DE REPORTE") || col.contains("PP/PV") || col.contains("PP / PV") || col.contains("TIPO DE MAQUINA")) {
                map["modelo"] = idx
            }
        }

        // 2. Second pass: Fallback for 'modelo' if MODELO REPORTE was not found
        if (!map.containsKey("modelo")) {
            rowCells.forEachIndexed { idx, cellStr ->
                val col = sanitizeHeader(cellStr)
                if (col == "MODELO" || col == "MODEL" || col == "TIPO") {
                    map["modelo"] = idx
                }
            }
        }

        // 3. Third pass for specific headers (Sala, QR, Propietario, etc.)
        rowCells.forEachIndexed { idx, cellStr ->
            val col = sanitizeHeader(cellStr)
            when {
                col.contains("ID_QR") || col.contains("ID QR") || col == "QR" || col.contains("QR_ID") -> map.putIfAbsent("qrid", idx)
                col.contains("SALA") || col.contains("CASINO") || col.contains("COMPLEJO") || col.contains("SUCURSAL") || col == "SALA" -> map.putIfAbsent("sala", idx)
                col.contains("PROPIETARIO") || col.contains("PROPIETARIA") || col.contains("OPERADOR") || col.contains("DUEÑO") -> map.putIfAbsent("propietario", idx)
                col.contains("ASSET") || col.contains("ACTIVO") || col.contains("INVENTARIO") || col.contains("ECONOMICO") -> map.putIfAbsent("asset", idx)
                col.contains("SERIE") || col.contains("SERIAL") || col.contains("S/N") -> map.putIfAbsent("serie", idx)
                col.contains("MARCA") || col.contains("BRAND") || col.contains("FABRICANTE") || col.contains("PROVEEDOR") -> map.putIfAbsent("marca", idx)
                col.contains("TITULO") || col.contains("JUEGO") || col.contains("GAME") -> map.putIfAbsent("juego", idx)
                col.contains("AREA") || col.contains("SECTOR") || col.contains("FUMADORES") || col.contains("ZONE") -> map.putIfAbsent("area", idx)
                col.contains("ISLA") || col.contains("ISLAND") || col.contains("BLOQUE") || col.contains("LINEA") -> map.putIfAbsent("isla", idx)
                (col.contains("MAQUINA") || col.contains("TERMINAL") || col.contains("EQUIPO") || col.contains("ID") || col.contains("NO MAQUINA") || col.contains("N MAQUINA")) && !col.contains("SERIE") && !col.contains("TIPO") && !col.contains("QR") -> map.putIfAbsent("maquina", idx)
            }
        }
        return map
    }

    fun parseCsvToMachines(csvText: String, defaultSala: String = ""): List<MachineEntity> {
        val rawLines = csvText.lines().filter { it.isNotBlank() }
        if (rawLines.isEmpty()) return emptyList()

        val firstLine = rawLines.first()
        val delimiter = when {
            firstLine.contains("\t") -> "\t"
            firstLine.contains(";") -> ";"
            firstLine.contains("|") -> "|"
            else -> ","
        }

        val headerTokens = firstLine.split(delimiter).map { sanitizeHeader(it) }
        val columnIndices = findHeaderIndices(headerTokens)
        val isHeaderFound = columnIndices.isNotEmpty()

        val startIndex = if (isHeaderFound) 1 else 0
        val machines = mutableListOf<MachineEntity>()

        for (i in startIndex until rawLines.size) {
            val line = rawLines[i]
            val tokens = line.split(delimiter).map { it.trim().removeSurrounding("\"") }
            if (tokens.isEmpty() || tokens.all { it.isBlank() }) continue

            fun getValue(key: String, defaultPositionalIndex: Int): String {
                val idx = columnIndices[key] ?: -1
                if (idx != -1 && idx < tokens.size && tokens[idx].isNotBlank()) {
                    return tokens[idx]
                }
                if (!isHeaderFound && defaultPositionalIndex >= 0 && defaultPositionalIndex < tokens.size && tokens[defaultPositionalIndex].isNotBlank()) {
                    return tokens[defaultPositionalIndex]
                }
                return ""
            }

            val asset = cleanNumericString(getValue("asset", 0))
            val salaRaw = getValue("sala", -1)
            val sala = salaRaw.ifBlank { defaultSala }
            val marca = getValue("marca", 1)
            val modelo = getValue("modelo", 2)
            val juego = getValue("juego", -1)
            val area = getValue("area", 4)
            val isla = cleanNumericString(getValue("isla", -1))
            val serie = getValue("serie", 6)
            val propietario = getValue("propietario", -1)
            val qrId = getValue("qrid", -1)
            val maquina = cleanNumericString(getValue("maquina", -1)).ifBlank { asset }.ifBlank { serie }

            if (maquina.isNotBlank() || asset.isNotBlank() || serie.isNotBlank() || marca.isNotBlank()) {
                machines.add(
                    MachineEntity(
                        machineNumber = maquina.ifBlank { "M-${machines.size + 1}" },
                        brand = marca.ifBlank { "General" },
                        model = modelo.ifBlank { "Estándar" },
                        serialNumber = serie.ifBlank { if (maquina.isNotBlank()) "SN-$maquina" else "SN-DESCONOCIDO" },
                        assetNumber = asset.ifBlank { maquina },
                        area = area.ifBlank { "Sala Principal" },
                        game = juego,
                        island = isla,
                        sala = sala,
                        qrId = qrId,
                        propietario = propietario
                    )
                )
            }
        }
        return machines
    }

    private fun sanitizeHeader(header: String): String {
        return header.uppercase()
            .replace("Á", "A")
            .replace("É", "E")
            .replace("Í", "I")
            .replace("Ó", "O")
            .replace("Ú", "U")
            .replace("N°", "N")
            .replace("Nº", "N")
            .replace("NO.", "N")
            .replace("N.", "N")
            .replace("#", "")
            .trim()
            .removeSurrounding("\"")
    }
}
