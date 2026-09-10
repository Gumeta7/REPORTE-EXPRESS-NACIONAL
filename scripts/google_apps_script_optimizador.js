/**
 * SCRIPT DE OPTIMIZACIÓN PARA GOOGLE SHEETS / GOOGLE APPS SCRIPT
 * Proyecto: Reportes Express Nacional (Winpot / Cirsa)
 *
 * Instrucciones de instalación:
 * 1. Abre tu hoja de cálculo en Google Sheets:
 *    https://docs.google.com/spreadsheets/d/1HSyA-GdDOmwdGwK5n1u3eNrggENZjqQqJNHInFbeHeU
 * 2. En el menú superior ve a: Extensiones -> Apps Script.
 * 3. Reemplaza el código del archivo Código.gs con este contenido.
 * 4. Haz clic en "Implementar" (Deploy) -> "Nueva implementación" (New deployment).
 * 5. Tipo: "Aplicación web" (Web app).
 *    - Ejecutar como: "Yo" (Tu cuenta de Google).
 *    - Quién tiene acceso: "Cualquier persona" (Anyone).
 * 6. Haz clic en "Implementar" y autoriza los permisos si te los solicita.
 */

function doGet(e) {
  try {
    var params = e ? e.parameter : {};
    var action = (params.action || "").trim().toLowerCase();
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    // 1. Endpoint: Obtener máquinas filtradas por sala o catálogo completo en JSON
    if (action === "getmaquinas" || action === "maquinas") {
      var salaParam = (params.sala || "").trim().toUpperCase();
      var sheetMaquinas = ss.getSheetByName("maquinas") || ss.getSheetByName("Maquinas") || ss.getSheetByName("MAQUINAS");
      if (!sheetMaquinas) {
        return jsonResponse({ success: false, message: "Pestaña 'maquinas' no encontrada", data: [] });
      }

      var data = sheetMaquinas.getDataRange().getValues();
      if (data.length <= 1) {
        return jsonResponse({ success: true, count: 0, data: [] });
      }

      var headers = data[0].map(function(h) { return String(h || "").trim().toUpperCase(); });
      var colIdx = {
        maquina: headers.indexOf("MAQUINA") !== -1 ? headers.indexOf("MAQUINA") : headers.indexOf("NUMERO"),
        marca: headers.indexOf("MARCA"),
        modelo: headers.indexOf("MODELO"),
        serie: headers.indexOf("SERIE") !== -1 ? headers.indexOf("SERIE") : headers.indexOf("SERIAL"),
        asset: headers.indexOf("ASSET") !== -1 ? headers.indexOf("ASSET") : headers.indexOf("ACTIVO"),
        area: headers.indexOf("AREA") !== -1 ? headers.indexOf("AREA") : headers.indexOf("ZONA"),
        game: headers.indexOf("JUEGO"),
        isla: headers.indexOf("ISLA"),
        sala: headers.indexOf("SALA") !== -1 ? headers.indexOf("SALA") : headers.indexOf("CASINO"),
        qrid: headers.indexOf("QRID") !== -1 ? headers.indexOf("QRID") : headers.indexOf("QR"),
        propietario: headers.indexOf("PROPIETARIO") !== -1 ? headers.indexOf("PROPIETARIO") : headers.indexOf("OPERADOR")
      };

      var isCorporate = (salaParam === "" || salaParam === "TODAS" || salaParam === "CORPORATIVO GDL" || salaParam === "DIRECTOR DE OPERACIONES" || salaParam === "CGDL" || salaParam === "DOPE");
      var results = [];

      for (var i = 1; i < data.length; i++) {
        var row = data[i];
        var rowSala = colIdx.sala !== -1 ? String(row[colIdx.sala] || "").trim() : "";
        var rowSalaUpper = rowSala.toUpperCase();

        // Si es corporativo o coincide con la sala solicitada
        if (isCorporate || rowSalaUpper.indexOf(salaParam) !== -1 || salaParam.indexOf(rowSalaUpper) !== -1) {
          var maquinaVal = colIdx.maquina !== -1 ? String(row[colIdx.maquina] || "").trim() : "";
          var assetVal = colIdx.asset !== -1 ? String(row[colIdx.asset] || "").trim() : "";
          var serieVal = colIdx.serie !== -1 ? String(row[colIdx.serie] || "").trim() : "";

          results.push({
            machineNumber: maquinaVal || assetVal || serieVal || ("M-" + i),
            brand: colIdx.marca !== -1 ? String(row[colIdx.marca] || "").trim() : "General",
            model: colIdx.modelo !== -1 ? String(row[colIdx.modelo] || "").trim() : "Estándar",
            serialNumber: serieVal || (maquinaVal ? "SN-" + maquinaVal : "SN-DESCONOCIDO"),
            assetNumber: assetVal || maquinaVal,
            area: colIdx.area !== -1 ? String(row[colIdx.area] || "").trim() : "Sala Principal",
            game: colIdx.game !== -1 ? String(row[colIdx.game] || "").trim() : "",
            island: colIdx.isla !== -1 ? String(row[colIdx.isla] || "").trim() : "",
            sala: rowSala,
            qrId: colIdx.qrid !== -1 ? String(row[colIdx.qrid] || "").trim() : "",
            propietario: colIdx.propietario !== -1 ? String(row[colIdx.propietario] || "").trim() : "WINPOT"
          });
        }
      }

      return jsonResponse({
        success: true,
        count: results.length,
        sala: salaParam || "TODAS",
        data: results
      });
    }

    // 2. Endpoint: Sincronización completa en JSON ligero
    if (action === "syncall" || action === "sync") {
      var salaFiltro = (params.sala || "").trim().toUpperCase();
      var isCorp = (salaFiltro === "" || salaFiltro === "TODAS" || salaFiltro.indexOf("CORPORATIVO") !== -1 || salaFiltro.indexOf("DIRECTOR") !== -1);

      // Tecnicos
      var sheetTecnicos = ss.getSheetByName("tecnicos") || ss.getSheetByName("Tecnicos") || ss.getSheetByName("usuarios");
      var tecnicos = [];
      if (sheetTecnicos) {
        var tData = sheetTecnicos.getDataRange().getValues();
        var tHeaders = tData[0].map(function(h) { return String(h || "").trim().toUpperCase(); });
        var uIdx = tHeaders.indexOf("USUARIO") !== -1 ? tHeaders.indexOf("USUARIO") : tHeaders.indexOf("USER");
        var pIdx = tHeaders.indexOf("PASSWORD") !== -1 ? tHeaders.indexOf("PASSWORD") : tHeaders.indexOf("CONTRASEÑA");
        var nIdx = tHeaders.indexOf("NOMBRE");
        var sIdx = tHeaders.indexOf("SALA");
        var rIdx = tHeaders.indexOf("ROL");
        var idSIdx = tHeaders.indexOf("ID_SALA");

        for (var t = 1; t < tData.length; t++) {
          var tRow = tData[t];
          var uVal = uIdx !== -1 ? String(tRow[uIdx] || "").trim() : "";
          if (uVal) {
            tecnicos.push({
              usuario: uVal,
              password: pIdx !== -1 ? String(tRow[pIdx] || "").trim() : "",
              nombre: nIdx !== -1 ? String(tRow[nIdx] || "").trim() : "",
              sala: sIdx !== -1 ? String(tRow[sIdx] || "").trim() : "",
              idSala: idSIdx !== -1 ? String(tRow[idSIdx] || "").trim() : "",
              rol: rIdx !== -1 ? String(tRow[rIdx] || "").trim() : "TECNICO",
              estatus: "ACTIVO"
            });
          }
        }
      }

      // Incidencias
      var sheetInc = ss.getSheetByName("incidencias") || ss.getSheetByName("Incidencias");
      var incidencias = [];
      if (sheetInc) {
        var incData = sheetInc.getDataRange().getValues();
        if (incData.length > 1) {
          var incHeaders = incData[0].map(function(h) { return String(h || "").trim().toUpperCase(); });
          var idTickIdx = incHeaders.indexOf("ID_TICKET") !== -1 ? incHeaders.indexOf("ID_TICKET") : incHeaders.indexOf("ID TICKET");
          var salaIncIdx = incHeaders.indexOf("SALA");
          var estIdx = incHeaders.indexOf("ESTADO") !== -1 ? incHeaders.indexOf("ESTADO") : incHeaders.indexOf("STATUS");
          var repIdx = incHeaders.indexOf("FECHA REPARACION") !== -1 ? incHeaders.indexOf("FECHA REPARACION") : incHeaders.indexOf("FECHA_REPARACION");
          var resIdx = incHeaders.indexOf("DESCRIPCION RESOLUCION") !== -1 ? incHeaders.indexOf("DESCRIPCION RESOLUCION") : incHeaders.indexOf("RESOLUCION");
          var fldIdx = incHeaders.indexOf("DESCRIPCION FALLA") !== -1 ? incHeaders.indexOf("DESCRIPCION FALLA") : incHeaders.indexOf("FALLA");

          for (var k = 1; k < incData.length; k++) {
            var iRow = incData[k];
            var iSala = salaIncIdx !== -1 ? String(iRow[salaIncIdx] || "").trim() : "";
            if (isCorp || iSala.toUpperCase().indexOf(salaFiltro) !== -1) {
              incidencias.push({
                idTicket: idTickIdx !== -1 ? String(iRow[idTickIdx] || "").trim() : ("INC-" + k),
                sala: iSala,
                estadoTicket: estIdx !== -1 ? String(iRow[estIdx] || "").trim().toUpperCase() : "ABIERTO",
                falla: fldIdx !== -1 ? String(iRow[fldIdx] || "").trim() : "",
                fechaReparacion: repIdx !== -1 ? formatDateCell(iRow[repIdx]) : "",
                resolucion: resIdx !== -1 ? String(iRow[resIdx] || "").trim() : ""
              });
            }
          }
        }
      }

      return jsonResponse({
        success: true,
        tecnicos: tecnicos,
        incidencias: incidencias
      });
    }

    // 3. Fallback: Exportar archivo XLSX completo
    var id = ss.getId();
    var exportUrl = "https://docs.google.com/spreadsheets/d/" + id + "/export?format=xlsx";
    var response = UrlFetchApp.fetch(exportUrl, {
      headers: { Authorization: "Bearer " + ScriptApp.getOAuthToken() },
      muteHttpExceptions: true
    });

    if (response.getResponseCode() === 200) {
      var blob = response.getBlob();
      var base64 = Utilities.base64Encode(blob.getBytes());
      return ContentService.createTextOutput(base64)
        .setMimeType(ContentService.MimeType.TEXT);
    }

    return ContentService.createTextOutput("Error al exportar archivo").setMimeType(ContentService.MimeType.TEXT);

  } catch (err) {
    return jsonResponse({ success: false, error: err.toString() });
  }
}

function doPost(e) {
  try {
    var contents = e.postData ? e.postData.contents : "";
    if (!contents) {
      return jsonResponse({ result: "error", message: "Sin datos recibidos" });
    }

    var payload = JSON.parse(contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName("incidencias") || ss.getSheetByName("Incidencias");
    if (!sheet) {
      return jsonResponse({ result: "error", message: "Pestaña 'incidencias' no encontrada" });
    }

    var lock = LockService.getScriptLock();
    try {
      lock.waitLock(10000);
    } catch (le) {
      return jsonResponse({ result: "error", message: "Servidor ocupado. Intenta de nuevo." });
    }

    try {
      var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
      var headersUpper = headers.map(function(h) { return String(h || "").trim().toUpperCase(); });

      var idCol = headersUpper.indexOf("ID_TICKET") !== -1 ? headersUpper.indexOf("ID_TICKET") : headersUpper.indexOf("ID TICKET");
      var estadoCol = headersUpper.indexOf("ESTADO") !== -1 ? headersUpper.indexOf("ESTADO") : headersUpper.indexOf("STATUS");
      var fechaRepCol = headersUpper.indexOf("FECHA REPARACION") !== -1 ? headersUpper.indexOf("FECHA REPARACION") : headersUpper.indexOf("FECHA_REPARACION");
      var resolucionCol = headersUpper.indexOf("DESCRIPCION RESOLUCION") !== -1 ? headersUpper.indexOf("DESCRIPCION RESOLUCION") : headersUpper.indexOf("RESOLUCION");

      // Buscar si el ticket ya existe para actualizarlo
      var ticketId = (payload.id_ticket || "").trim();
      var data = sheet.getDataRange().getValues();
      var rowIndex = -1;

      for (var r = 1; r < data.length; r++) {
        var curId = idCol !== -1 ? String(data[r][idCol] || "").trim() : "";
        if (curId && curId.toUpperCase() === ticketId.toUpperCase()) {
          rowIndex = r + 1;
          break;
        }
      }

      if (rowIndex !== -1) {
        // Actualizar ticket existente
        var newEstado = payload.estado || payload.estado_ticket || payload.estadoTicket;
        var newFechaRep = payload.fecha_reparacion || payload.fechaReparacion;
        var newResolucion = payload.resolucion || payload.solucion;

        if (newEstado && estadoCol !== -1) {
          sheet.getRange(rowIndex, estadoCol + 1).setValue(newEstado);
        }
        if (newFechaRep && fechaRepCol !== -1) {
          sheet.getRange(rowIndex, fechaRepCol + 1).setValue(newFechaRep);
        }
        if (newResolucion && resolucionCol !== -1) {
          sheet.getRange(rowIndex, resolucionCol + 1).setValue(newResolucion);
        }
        return jsonResponse({ result: "success", message: "Ticket actualizado", id_ticket: ticketId });
      }

      // Si es nuevo ticket, agregarlo al final
      var ticketId = (payload.id_ticket || payload.idTicket || "").trim();
      var estadoVal = payload.estado || payload.estado_ticket || payload.estadoTicket || "ABIERTO";
      var fechaOrigVal = payload.fecha_origen || payload.fechaOrigen || Utilities.formatDate(new Date(), "America/Mexico_City", "dd/MM/yyyy HH:mm:ss");
      var fechaRepVal = payload.fecha_reparacion || payload.fechaReparacion || "";
      var resolucionVal = payload.resolucion || payload.solucion || "";
      var idTecnicoVal = payload.id_tecnico || payload.idTecnico || "";

      var newRow = [];
      for (var c = 0; c < headers.length; c++) {
        var h = headersUpper[c];
        if (h.indexOf("ID_TICKET") !== -1 || h === "ID") newRow.push(ticketId);
        else if (h.indexOf("SALA") !== -1 || h.indexOf("CASINO") !== -1) newRow.push(payload.sala || "");
        else if (h.indexOf("MARCA") !== -1) newRow.push(payload.marca || "");
        else if (h.indexOf("MODELO") !== -1) newRow.push(payload.modelo || "");
        else if (h.indexOf("SERIE") !== -1 || h.indexOf("SERIAL") !== -1) newRow.push(payload.serie || "");
        else if (h.indexOf("ASSET") !== -1 || h.indexOf("ACTIVO") !== -1) newRow.push(payload.asset || "");
        else if (h.indexOf("AREA") !== -1 || h.indexOf("ZONA") !== -1) newRow.push(payload.area || "");
        else if (h.indexOf("PROPIETARIO") !== -1) newRow.push(payload.propietario || "WINPOT");
        else if (h.indexOf("OPERATIVA") !== -1) newRow.push(payload.operativa || "NO");
        else if (h.indexOf("ESTADO") !== -1 || h.indexOf("STATUS") !== -1) newRow.push(estadoVal);
        else if (h.indexOf("ORIGEN") !== -1 || (h.indexOf("FECHA") !== -1 && h.indexOf("REPARACION") === -1)) newRow.push(fechaOrigVal);
        else if (h.indexOf("REPARACION") !== -1) newRow.push(fechaRepVal);
        else if (h.indexOf("RESOLUCION") !== -1 || h.indexOf("SOLUCION") !== -1) newRow.push(resolucionVal);
        else if (h.indexOf("FALLA") !== -1 || h.indexOf("DESCRIPCION") !== -1) newRow.push(payload.falla || "");
        else if (h.indexOf("PRIORIDAD") !== -1) newRow.push(payload.prioridad || "MEDIA");
        else if (h.indexOf("ID_TECNICO") !== -1) newRow.push(idTecnicoVal);
        else if (h.indexOf("TECNICO") !== -1) newRow.push(payload.tecnico || "");
        else newRow.push("");
      }

      sheet.appendRow(newRow);
      return jsonResponse({ result: "success", message: "Ticket registrado", id_ticket: ticketId });

    } finally {
      lock.releaseLock();
    }

  } catch (e) {
    return jsonResponse({ result: "error", message: e.toString() });
  }
}

function formatDateCell(val) {
  if (!val) return "";
  if (val instanceof Date) {
    return Utilities.formatDate(val, "America/Mexico_City", "dd/MM/yyyy");
  }
  return String(val).trim();
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
