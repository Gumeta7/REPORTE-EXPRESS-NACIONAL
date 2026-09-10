package com.example.data.remote

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.TimeUnit

object GoogleSheetsUpdateService {
    private const val TAG = "GoogleSheetsUpdate"
    private const val SPREADSHEET_ID = "1HSyA-GdDOmwdGwK5n1u3eNrggENZjqQqJNHInFbeHeU"
    private const val CLIENT_EMAIL = "sheets-backend-reader@reportes-express-backend.iam.gserviceaccount.com"

    private const val PRIVATE_KEY_PEM = """-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDoKZbe2/7zknQ6
PTQPkjlx0s34e/EFsd2ib6NzKUjbbSgkDAxO04X6Tz0NB1ajekiFtAejnIz0naop
/U9p6BLDuzibTNwYInEIC8jWkLzoMfLnn5AOYfvBuzXe1U7n92L/XiCISPOeaeQKP
/LMtMReqwvyUpxHYH52uKQXEoZXhT1ffA65kz3kPgi1+26eBfwfn4DYJw5RKVzz9
ZQWZihrJFyoZQmKuzvHHbFe1Uz3ZkUf2EL+TxKubXFuYr6Pyu04DjCrgdCWPjfb0
JkrOzBBdb55rWsz865MP2mrxFKqUAorZfk3srIEVcc0k8ugZhdIPBXN028gqwvtB
sXKT4OlBAgMBAAECggEAAjS2N35RofIGs7oSZ9vqJ+PJXELNXfNWSgi3EzMVoUi0
DDuiaBTji+3x7Jgss0Wdi3UDYUUWrMaQdbIB9nzvNwFvCkEPvZRGY+TNP5nWcd2SDg
qk5fj41/mbxXms4p57u+3vLQMeAMy0+BoZQW/KJ1L/HXrFJb/ZnmtcHZgkq/Muam
YsDtyBs7p2GYkbL3FwStD0IK0yfLxGT/tfmF+GPxHRAQzZUxwympvvG0Q51CMenT
r5L1RyskaunpSTbKEFSzdE4VcxxuyCt+LA0JbyJgGqypYQcKhJLTOMFH7yl8om2KV
htyTEIhk0pez5xu5zcHdv6sSyR0It+lzstGKkJ4RjQKBgQD9y6cVWEo1dT/2bNg9
whQo56jYtO5mNr4Uw8kQCHIL3YsZ3XQ5goLkVU9GsBMb71hurO/aLV9rjtWGFXsR
jjF9m74rT07fx1o8IixfQmW5YPOITVQMIiRLtewhGJnIvH2iRAaZaLfOaZavVh1w
zW5pF42KofwSN+Q3z1+MjDSJWwKBgQDqLdUu7/7iRF7zzQ/k396etkf1KbsNkPIz
9c0xfMHePVxO1iPpGyzgrdxdelq4S6GKy/J0rFcEJ/N7p/3bJ9I2UA8N+3k+eCK/
tnPZ2JRFWMMPWPo57Nsvl54u1yFrz/bp6HR+BaXY1YMl+IYgnPjm0iFnqIMkmvWk
KseMAfY+kwKBgQDrS9xkWa63UTUz6kWxPDYklmJcR6Ke8THZXosSakIR7hwApugS
SYXA27bb+nI8+/Io6fyVcmt89LfR0CiBZitMIRB2Ztjhb5mhFq28w3o8HD6sp8S8
Wwr7hnadWIl7KgXwYtEbeZKvtAUYGebBpjQvCr8XQIbK/TaA56nWTEaI8QKBgEFA
NheXDclUTbTXHaejbW+B0PkSAKneCm7J/bvdTFO1e0QITQdF6DJOn4l4b1DM2h1H
iK5T2wcwbpqq1C3oGVX+GvTsjJ/xtExmiKBO6Uk4kfo1XOJntUoGnWI8qqIhW8TDp
8ZOv1S9fBuUMo6rwsjX2tRIiFTCc8PcmaGWtokR7AoGBANQ19SanVDIwyYs06kO
i6aNM5Jyf7hFoBSBYN4KyiRVSjPhSiEsvq3GsNBt3pjOngdZi9j51YZvQ/9Yiyy0
/Pl0ysZpdmWFU64p8Fe7ShW1ZtiqBr1X0Qkf5IM5cCed6YnfYTp4I4tnam+uNWAW
Aqh+qUvynCZ9qhEoIRoWku+u
-----END PRIVATE KEY-----"""

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cachedToken: String? = null
    @Volatile
    private var tokenExpiryEpochSeconds: Long = 0L

    private fun getPrivateKey(): java.security.PrivateKey {
        val cleanKey = PRIVATE_KEY_PEM
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val keyBytes = Base64.decode(cleanKey, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec)
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val nowSec = System.currentTimeMillis() / 1000
        if (cachedToken != null && nowSec < tokenExpiryEpochSeconds - 60) {
            return@withContext cachedToken!!
        }

        val privateKey = getPrivateKey()
        val header = base64UrlEncode("""{"alg":"RS256","typ":"JWT"}""".toByteArray(Charsets.UTF_8))
        val exp = nowSec + 3600
        val claim = base64UrlEncode(
            """{"iss":"$CLIENT_EMAIL","scope":"https://www.googleapis.com/auth/spreadsheets","aud":"https://oauth2.googleapis.com/token","exp":$exp,"iat":$nowSec}"""
                .toByteArray(Charsets.UTF_8)
        )

        val unsignedJwt = "$header.$claim"
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(privateKey)
        signer.update(unsignedJwt.toByteArray(Charsets.UTF_8))
        val signature = base64UrlEncode(signer.sign())

        val signedAssertion = "$unsignedJwt.$signature"

        val formBody = FormBody.Builder()
            .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            .add("assertion", signedAssertion)
            .build()

        val req = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(formBody)
            .build()

        val resp = client.newCall(req).execute()
        val bodyStr = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
            throw Exception("Fallo de autenticación OAuth2 (${resp.code}): $bodyStr")
        }

        val json = JSONObject(bodyStr)
        val token = json.getString("access_token")
        cachedToken = token
        tokenExpiryEpochSeconds = exp
        return@withContext token
    }

    private fun colToLetter(colIdx: Int): String {
        var letter = ""
        var temp = colIdx
        while (temp >= 0) {
            letter = ('A'.code + (temp % 26)).toChar() + letter
            temp = (temp / 26) - 1
        }
        return letter
    }

    suspend fun updateIncidenciaStatus(
        idTicket: String,
        nuevoEstado: String,
        operativa: String,
        resolucion: String,
        fechaReparacion: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()

            // 1. Obtener datos de la pestaña Incidencias
            val getReq = Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/$SPREADSHEET_ID/values/Incidencias!A1:Z2000")
                .header("Authorization", "Bearer $token")
                .build()

            val getResp = client.newCall(getReq).execute()
            val getBody = getResp.body?.string().orEmpty()
            if (!getResp.isSuccessful) {
                return@withContext Result.failure(Exception("Error al consultar hoja (${getResp.code}): $getBody"))
            }

            val rootJson = JSONObject(getBody)
            val valuesArray = rootJson.optJSONArray("values")
                ?: return@withContext Result.failure(Exception("No se encontraron filas en la hoja Incidencias."))

            if (valuesArray.length() < 2) {
                return@withContext Result.failure(Exception("No hay registros suficientes en la hoja Incidencias."))
            }

            // Headers
            val headerRow = valuesArray.getJSONArray(0)
            val headers = (0 until headerRow.length()).map { idx ->
                headerRow.optString(idx).trim().lowercase()
            }

            val idxId = headers.indexOfFirst { it == "id_ticket" || it == "id ticket" || it == "id" }
            val idxEstado = headers.indexOfFirst { it.contains("estado") }
            val idxOperativa = headers.indexOfFirst { it == "operativa" }
            val idxFechaReparacion = headers.indexOfFirst { it.contains("reparacion") }
            val idxResolucion = headers.indexOfFirst { it.contains("resolucion") || it.contains("solucion") }

            if (idxId == -1 || idxEstado == -1) {
                return@withContext Result.failure(Exception("No se encontraron las columnas requeridas en la hoja de incidencias."))
            }

            var targetRowIndex = -1
            var targetRow: JSONArray? = null
            for (i in 1 until valuesArray.length()) {
                val r = valuesArray.getJSONArray(i)
                val rowId = if (idxId < r.length()) r.optString(idxId).trim() else ""
                if (rowId.equals(idTicket.trim(), ignoreCase = true)) {
                    targetRowIndex = i
                    targetRow = r
                    break
                }
            }

            if (targetRowIndex == -1 || targetRow == null) {
                return@withContext Result.failure(Exception("No se encontró el ticket $idTicket en la hoja."))
            }

            val sheetRowNumber = targetRowIndex + 1
            val cleanEstado = nuevoEstado.trim().uppercase()
            val isResolved = cleanEstado.contains("RESUELTO") || cleanEstado.contains("CERRADO")

            var cleanOperativa = operativa.trim().uppercase()
            if (cleanOperativa.isBlank()) {
                cleanOperativa = if (isResolved) "SI" else "NO"
            }

            val finalFechaReparacion = if (isResolved) {
                if (fechaReparacion.isNotBlank()) {
                    fechaReparacion.trim()
                } else {
                    val existing = if (idxFechaReparacion != -1 && idxFechaReparacion < targetRow.length()) {
                        targetRow.optString(idxFechaReparacion).trim()
                    } else ""
                    existing.ifBlank {
                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                    }
                }
            } else {
                ""
            }

            val cleanResolucion = resolucion.trim()

            // Armar data para batchUpdate
            val dataArray = JSONArray()

            // 1. Estado
            val estadoObj = JSONObject()
            estadoObj.put("range", "Incidencias!${colToLetter(idxEstado)}$sheetRowNumber")
            estadoObj.put("values", JSONArray().put(JSONArray().put(cleanEstado)))
            dataArray.put(estadoObj)

            // 2. Operativa
            if (idxOperativa != -1) {
                val opObj = JSONObject()
                opObj.put("range", "Incidencias!${colToLetter(idxOperativa)}$sheetRowNumber")
                opObj.put("values", JSONArray().put(JSONArray().put(cleanOperativa)))
                dataArray.put(opObj)
            }

            // 3. Fecha Reparación
            if (idxFechaReparacion != -1) {
                val repObj = JSONObject()
                repObj.put("range", "Incidencias!${colToLetter(idxFechaReparacion)}$sheetRowNumber")
                repObj.put("values", JSONArray().put(JSONArray().put(finalFechaReparacion)))
                dataArray.put(repObj)
            }

            // 4. Descripción Resolución
            if (idxResolucion != -1) {
                val resObj = JSONObject()
                resObj.put("range", "Incidencias!${colToLetter(idxResolucion)}$sheetRowNumber")
                resObj.put("values", JSONArray().put(JSONArray().put(cleanResolucion)))
                dataArray.put(resObj)
            }

            val batchBody = JSONObject()
            batchBody.put("valueInputOption", "USER_ENTERED")
            batchBody.put("data", dataArray)

            val updateReq = Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/$SPREADSHEET_ID/values:batchUpdate")
                .header("Authorization", "Bearer $token")
                .post(batchBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val updateResp = client.newCall(updateReq).execute()
            val updateRespBody = updateResp.body?.string().orEmpty()

            if (!updateResp.isSuccessful) {
                return@withContext Result.failure(Exception("Error al actualizar celdas (${updateResp.code}): $updateRespBody"))
            }

            Log.d(TAG, "Ticket $idTicket actualizado exitosamente a $cleanEstado")
            return@withContext Result.success("Estatus del ticket $idTicket actualizado a $cleanEstado")
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando ticket $idTicket", e)
            return@withContext Result.failure(e)
        }
    }
}
