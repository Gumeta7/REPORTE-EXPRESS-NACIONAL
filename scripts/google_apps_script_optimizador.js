/**
 * ============================================================================
 * SISTEMA CENTRALIZADO DE GESTIÓN DE INCIDENCIAS Y CATÁLOGO NACIONAL (5,000+)
 * PROYECTO: REPORTE EXPRESS NACIONAL (WINPOT / CIRSA)
 * ============================================================================
 * Este script actúa como el "cerebro" o API Webhook en Google Sheets:
 * 1. Recibe los reportes desde la app Android con control de concurrencia (LockService),
 *    evitando duplicados o pérdida de folios consecutivos.
 * 2. OPTIMIZACIÓN: Permite consultar el catálogo de máquinas filtrado por sala
 *    en formato JSON ligero (milisegundos) para que técnicos de salas individuales
 *    no tengan que descargar ni procesar 5,000 registros en el teléfono.
 * 3. PERFILES CORPORATIVOS: Los usuarios de "Corporativo GDL" y "Director de Operaciones"
 *    reciben todas las máquinas de todas las salas a nivel nacional.
 * 4. Exporta el archivo Excel completo (DB_WINPOT_FORMS.xlsx en Base64) para
 *    sincronización tradicional offline.
 */

/**
 * Función que se ejecuta automáticamente cuando la app móvil envía un reporte o actualización (vía HTTP POST).
 */
function doPost(e) {
  // PASO 1: Bloqueo de seguridad atómico (LockService)
  // Atiende ordenadamente uno por uno durante un máximo de 30 segundos si varios técnicos reportan al mismo tiempo.
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(30000); // Espera pacientemente en la fila hasta 30 segundos
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      result: "error",
      message: "El servidor está ocupado procesando otros reportes. Por favor intenta de nuevo en unos segundos."
    })).setMimeType(ContentService.MimeType.JSON);
  }

  try {
    // PASO 2: Leer los datos que mandó la aplicación en formato JSON
    var data = JSON.parse(e.postData.contents);
    
    // Conectarse a este archivo de Google Sheets y buscar la pestaña "Incidencias"
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName("Incidencias") || ss.getSheetByName("incidencias");
    
    // Verificación de seguridad
    if (!sheet) {
      return ContentService.createTextOutput(JSON.stringify({
        result: "error",
        message: "No se encontró la pestaña 'Incidencias' en esta hoja de cálculo."
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    var lastRow = sheet.getLastRow();
    var proposedTicketId = (data.id_ticket || data.idTicket || "").trim();

    // Comprobar si es una actualización de un ticket existente (por ejemplo, al resolverlo o agregar notas)
    if (proposedTicketId && lastRow > 1) {
      var idValues = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
      for (var r = 0; r < idValues.length; r++) {
        var existingId = String(idValues[r][0] || "").trim();
        if (existingId && existingId.toUpperCase() === proposedTicketId.toUpperCase()) {
          var targetRow = r + 2;
          var newEstado = data.estado || data.estado_ticket || data.estadoTicket;
          var newFechaRep = data.fecha_reparacion || data.fechaReparacion;
          var newResolucion = data.resolucion || data.solucion || data.descripcion_resolucion;

          if (newEstado) sheet.getRange(targetRow, 10).setValue(newEstado.toUpperCase()); // Col J: Estado
          if (newFechaRep) sheet.getRange(targetRow, 12).setValue(newFechaRep);          // Col L: Fecha Reparación
          if (newResolucion) sheet.getRange(targetRow, 17).setValue(newResolucion);      // Col Q: Descripción Resolución

          return ContentService.createTextOutput(JSON.stringify({
            result: "success",
            message: "Ticket actualizado correctamente",
            id_ticket: proposedTicketId
          })).setMimeType(ContentService.MimeType.JSON);
        }
      }
    }

    // PASO 3: Revisar los folios que ya existen en la columna A (ID Ticket) para cálculo consecutivo
    var maxConsecutive = 0;
    var idsExistentes = [];

    if (lastRow > 1) {
      var rangeValues = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
      for (var i = 0; i < rangeValues.length; i++) {
        var tId = String(rangeValues[i][0] || "").trim();
        if (tId) {
          idsExistentes.push(tId);
          // Extrae el número consecutivo final (ej: de "PHIE-22/55243-15" extrae el 15)
          var match = tId.match(/-(\d+)$/);
          if (match) {
            var num = parseInt(match[1], 10);
            if (num > maxConsecutive) {
              maxConsecutive = num;
            }
          }
        }
      }
    }

    // PASO 4: Resolver y garantizar el Folio Único
    var finalTicketId = proposedTicketId;
    var isDuplicate = idsExistentes.indexOf(proposedTicketId) !== -1;
    
    // Si ya existe o si la app no mandó ninguno, calculamos atómicamente el siguiente folio libre
    if (isDuplicate || !finalTicketId) {
      var nextConsecutive = maxConsecutive + 1;
      var sala = (data.sala || "").trim();
      var idSala = getIdSalaFromNombre(sala);
      var serie = String(data.serie || "").trim().replace(/\s+/g, "");
      if (!serie) serie = "SN-" + (data.asset || "PENDIENTE");

      // Nueva Nomenclatura oficial: ID_SALA-SERIE-CONSECUTIVO (ej: PHIE-22/55243-16)
      finalTicketId = idSala + "-" + serie + "-" + nextConsecutive;
    }

    // PASO 5: Generar la marca de tiempo (Fecha y hora actual de Ciudad de México)
    var fechaOrigen = Utilities.formatDate(new Date(), "America/Mexico_City", "yyyy-MM-dd HH:mm:ss");

    // PASO 6: Insertar la fila completa en la pestaña Incidencias de Google Sheets (Columnas A - Q)
    sheet.appendRow([
      finalTicketId,                                          // Columna A: ID Ticket
      data.sala || "",                                        // Columna B: Sala
      data.marca || "",                                       // Columna C: Marca
      data.modelo || "",                                      // Columna D: Modelo
      data.serie || "",                                       // Columna E: Serie
      data.asset || "",                                       // Columna F: Asset
      data.area || "Sala Principal",                          // Columna G: Área
      data.propietario || "WINPOT",                           // Columna H: Propietario
      (data.operativa || "NO").toUpperCase(),                 // Columna I: Operativa (SI / NO)
      (data.estado_ticket || data.estado || "ABIERTO").toUpperCase(), // Columna J: Estado Ticket
      fechaOrigen,                                            // Columna K: Fecha Origen
      data.fecha_reparacion || data.fechaReparacion || "",    // Columna L: Fecha Reparación
      data.falla || "",                                       // Columna M: Descripción de la falla
      (data.prioridad || "MEDIA").toUpperCase(),              // Columna N: Prioridad (BAJA, MEDIA, ALTA, CRITICA)
      data.id_tecnico || data.idTecnico || "",                // Columna O: ID del Técnico
      data.tecnico || "",                                     // Columna P: Nombre del Técnico
      data.resolucion || data.solucion || ""                  // Columna Q: Descripción Resolución
    ]);
    
    // PASO 7: Responder a la app móvil con éxito y el ID de ticket final registrado
    return ContentService.createTextOutput(JSON.stringify({
      result: "success",
      id_ticket: finalTicketId,
      consecutive: maxConsecutive + 1
    })).setMimeType(ContentService.MimeType.JSON);
    
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      result: "error",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
    
  } finally {
    // PASO 8: Liberar el candado de seguridad
    lock.releaseLock();
  }
}

/**
 * Función que se ejecuta cuando la app móvil consulta o descarga la base de datos (vía HTTP GET).
 * - Si recibe ?action=getMaquinas&sala=... filtra en el servidor y devuelve solo las máquinas necesarias en JSON.
 * - Si no recibe parámetros o pide export, entrega el archivo Excel completo (DB_WINPOT_FORMS.xlsx) en Base64.
 */
function doGet(e) {
  try {
    var params = e ? e.parameter : {};
    var action = (params.action || "").trim().toLowerCase();
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    // =========================================================================
    // OPTIMIZACIÓN 1: Consulta rápida de máquinas por sala o Corporativo (JSON)
    // =========================================================================
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

      // Los perfiles "Corporativo GDL" y "Director de Operaciones" ven TODAS las máquinas de todas las salas
      var isCorporate = (salaParam === "" || salaParam === "TODAS" || salaParam.indexOf("CORPORATIVO") !== -1 || salaParam.indexOf("DIRECTOR") !== -1 || salaParam === "CGDL" || salaParam === "DOPE");
      var results = [];

      for (var i = 1; i < data.length; i++) {
        var row = data[i];
        var rowSala = colIdx.sala !== -1 ? String(row[colIdx.sala] || "").trim() : "";
        var rowSalaUpper = rowSala.toUpperCase();

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

    // =========================================================================
    // EXPORTACIÓN ORIGINAL POR DEFECTO: Descargar archivo Excel completo Base64
    // =========================================================================
    var sheetId = ss.getId();
    var url = "https://docs.google.com/spreadsheets/d/" + sheetId + "/export?format=xlsx";
    var token = ScriptApp.getOAuthToken();
    var response = UrlFetchApp.fetch(url, {
      headers: {
        'Authorization': 'Bearer ' + token
      },
      muteHttpExceptions: true
    });
    
    var blob = response.getBlob();
    blob.setName("DB_WINPOT_FORMS.xlsx");
    
    // Enviamos el archivo Excel codificado en Base64 para que la app lo lea localmente
    return ContentService.createTextOutput(Utilities.base64Encode(blob.getBytes()))
      .setMimeType(ContentService.MimeType.TEXT);

  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Función auxiliar para convertir el nombre completo de una sala a su clave oficial corta.
 * Incluye soportes oficiales para "Corporativo GDL" (CGDL) y "Director de Operaciones" (DOPE).
 */
function getIdSalaFromNombre(sala) {
  var s = (sala || "").toUpperCase();
  if (s.indexOf("METROCENTRO") !== -1) return "METR";
  if (s.indexOf("PUERTA DE HIERRO") !== -1 || s.indexOf("HIERRO") !== -1) return "PHIE";
  if (s.indexOf("SATELITE") !== -1) return "SATE";
  if (s.indexOf("DIAMONDS GUADALAJARA") !== -1 || s.indexOf("DIAMONDS") !== -1) return "CORD";
  if (s.indexOf("INTERLOMAS") !== -1 || s.indexOf("VENETO") !== -1) return "INTE";
  if (s.indexOf("TUXTLA") !== -1) return "TUXT";
  if (s.indexOf("CIRCUNVALACION") !== -1) return "CIRC";
  if (s.indexOf("PACHUCA") !== -1) return "PACH";
  if (s.indexOf("MERIDA") !== -1) return "MERI";
  if (s.indexOf("PLAYA") !== -1) return "PLAY";
  if (s.indexOf("PUEBLA") !== -1) return "PUEB";
  if (s.indexOf("CARRANZA") !== -1) return "CARR";
  if (s.indexOf("POZA RICA") !== -1) return "POZA";
  if (s.indexOf("TONALA") !== -1) return "TONA";
  if (s.indexOf("CORDILLERAS") !== -1) return "CORD";
  if (s.indexOf("METEPEC") !== -1) return "METE";
  if (s.indexOf("MANDARIN") !== -1) return "MAND";
  if (s.indexOf("GUAYMAS") !== -1) return "GUAY";
  if (s.indexOf("BOCA DEL RIO") !== -1) return "BOCA";
  if (s.indexOf("PUNTO SUR") !== -1) return "PSUR";
  if (s.indexOf("DIRECTOR") !== -1 || s.indexOf("DOPE") !== -1) return "DOPE";
  if (s.indexOf("CORPORATIVO") !== -1 || s.indexOf("CGDL") !== -1) return "CGDL";
  return (sala || "SALA").substring(0, 4).toUpperCase();
}

/**
 * Función de utilidad para solicitar permisos de Drive e Internet a Google Apps Script
 */
function testAuth() {
  DriveApp.getRootFolder();
  UrlFetchApp.fetch("https://www.google.com");
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
