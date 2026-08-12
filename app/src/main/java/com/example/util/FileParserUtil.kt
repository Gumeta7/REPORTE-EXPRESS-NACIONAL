package com.example.util

import com.example.data.db.MachineEntity
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

object FileParserUtil {

    fun parseStreamToMachines(inputStream: InputStream): List<MachineEntity> {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) return emptyList()

        // 1. Try parsing with Apache POI as Excel (.xls or .xlsx)
        try {
            bytes.inputStream().use { stream ->
                val workbook = WorkbookFactory.create(stream)
                val allMachines = mutableListOf<MachineEntity>()

                for (sheetIndex in 0 until workbook.numberOfSheets) {
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
                        if (map.containsKey("asset") || map.containsKey("serie") || map.containsKey("marca") || map.containsKey("maquina")) {
                            headerRowIndex = r
                            columnIndices = map
                            break
                        }
                    }

                    val startRow = if (headerRowIndex != -1) headerRowIndex + 1 else 0

                    for (r in startRow..sheet.lastRowNum) {
                        val row = sheet.getRow(r) ?: continue
                        fun getVal(key: String, fallbackCol: Int): String {
                            val idx = columnIndices[key] ?: fallbackCol
                            if (idx < 0) return ""
                            val cell = row.getCell(idx) ?: return ""
                            return getCellValueAsString(cell)
                        }

                        val asset = getVal("asset", 0)
                        val marca = getVal("marca", 1)
                        val modelo = getVal("modelo", 2)
                        val juego = getVal("juego", 3)
                        val area = getVal("area", 4)
                        val isla = getVal("isla", 5)
                        val serie = getVal("serie", 6)
                        val maquina = getVal("maquina", -1).ifBlank { asset }.ifBlank { serie }

                        if (maquina.isNotBlank() || asset.isNotBlank() || serie.isNotBlank()) {
                            allMachines.add(
                                MachineEntity(
                                    machineNumber = maquina,
                                    brand = marca.ifBlank { "General" },
                                    model = modelo.ifBlank { "Estándar" },
                                    serialNumber = serie.ifBlank { if (maquina.isNotBlank()) "SN-$maquina" else "SN-DESCONOCIDO" },
                                    assetNumber = asset.ifBlank { maquina },
                                    area = area.ifBlank { "Sala Principal" },
                                    game = juego.ifBlank { "General" },
                                    island = isla.ifBlank { "Isla 01" },
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
            // Not a valid Excel file or POI error, fallback to CSV parsing
        }

        // 2. Fallback to CSV / TSV text parsing
        val text = String(bytes, Charsets.UTF_8)
        return parseCsvToMachines(text)
    }

    fun extractTextFromStream(inputStream: InputStream): String {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) return ""

        // Try POI Excel text extraction
        try {
            bytes.inputStream().use { stream ->
                val workbook = WorkbookFactory.create(stream)
                val sb = StringBuilder()
                for (s in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheetAt(s) ?: continue
                    for (r in 0..sheet.lastRowNum) {
                        val row = sheet.getRow(r) ?: continue
                        val line = (0 until row.lastCellNum).joinToString("\t") { c ->
                            getCellValueAsString(row.getCell(c))
                        }.trim()
                        if (line.isNotBlank()) {
                            sb.append(line).append("\n")
                        }
                    }
                }
                workbook.close()
                val extracted = sb.toString().trim()
                if (extracted.isNotBlank()) return extracted
            }
        } catch (_: Throwable) {
            // Not Excel
        }

        return String(bytes, Charsets.UTF_8)
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

        // 3. Third pass for general headers
        rowCells.forEachIndexed { idx, cellStr ->
            val col = sanitizeHeader(cellStr)
            when {
                col.contains("ASSET") || col.contains("ACTIVO") || col.contains("INVENTARIO") -> map.putIfAbsent("asset", idx)
                col.contains("SERIE") || col.contains("SERIAL") || col.contains("S/N") -> map.putIfAbsent("serie", idx)
                col.contains("MARCA") || col.contains("BRAND") || col.contains("FABRICANTE") || col.contains("PROVEEDOR") -> map.putIfAbsent("marca", idx)
                col.contains("TITULO") || col.contains("JUEGO") || col.contains("GAME") -> map.putIfAbsent("juego", idx)
                col.contains("AREA") || col.contains("UBICACION") || col.contains("SALA") || col.contains("ZONE") -> map.putIfAbsent("area", idx)
                col.contains("ISLA") || col.contains("ISLAND") || col.contains("BLOQUE") || col.contains("LINEA") -> map.putIfAbsent("isla", idx)
                (col.contains("MAQUINA") || col.contains("TERMINAL") || col.contains("EQUIPO") || col.contains("ID") || col.contains("NO MAQUINA") || col.contains("N MAQUINA")) && !col.contains("SERIE") && !col.contains("TIPO") -> map.putIfAbsent("maquina", idx)
            }
        }
        return map
    }

    fun parseCsvToMachines(csvText: String): List<MachineEntity> {
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

            val asset = getValue("asset", 0)
            val marca = getValue("marca", 1)
            val modelo = getValue("modelo", 2)
            val juego = getValue("juego", 3)
            val area = getValue("area", 4)
            val isla = getValue("isla", 5)
            val serie = getValue("serie", 6)
            val maquina = getValue("maquina", -1).ifBlank { asset }.ifBlank { serie }

            if (maquina.isNotBlank() || asset.isNotBlank() || serie.isNotBlank()) {
                machines.add(
                    MachineEntity(
                        machineNumber = maquina,
                        brand = marca.ifBlank { "General" },
                        model = modelo.ifBlank { "Estándar" },
                        serialNumber = serie.ifBlank { if (maquina.isNotBlank()) "SN-$maquina" else "SN-DESCONOCIDO" },
                        assetNumber = asset.ifBlank { maquina },
                        area = area.ifBlank { "Sala Principal" },
                        game = juego.ifBlank { "General" },
                        island = isla.ifBlank { "Isla 01" }
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
