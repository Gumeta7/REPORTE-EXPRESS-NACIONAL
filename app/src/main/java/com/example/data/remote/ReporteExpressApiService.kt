package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.db.MachineEntity
import com.example.data.db.ProviderEmailEntity
import com.example.data.db.TechnicianEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LoginResult(
    val token: String,
    val technician: TechnicianEntity
)

object ReporteExpressApiService {
    private const val TAG = "ReporteExpressApi"

    // URL por defecto para la red local (Wi-Fi de la PC donde corre el servidor)
    const val DEFAULT_LOCAL_BASE_URL = "http://192.168.0.122:4000"
    const val DEFAULT_EMULATOR_BASE_URL = "http://10.0.2.2:4000"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun getEffectiveBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences("reportes_express_prefs", Context.MODE_PRIVATE)
        val customUrl = prefs.getString("custom_api_base_url", "")?.trim()
            ?: context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("custom_api_base_url", "")?.trim()
            ?: ""
        return if (customUrl.isNotBlank()) {
            customUrl.removeSuffix("/")
        } else {
            DEFAULT_LOCAL_BASE_URL
        }
    }

    /**
     * POST /api/login
     */
    suspend fun login(
        baseUrl: String,
        usuarioInput: String,
        passwordInput: String
    ): Result<LoginResult> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.removeSuffix("/")}/api/login"
            val json = JSONObject().apply {
                put("usuario", usuarioInput.trim())
                put("password", passwordInput.trim())
            }

            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errMessage = try {
                    JSONObject(bodyString).optString("error", "Error de autenticación ($code)")
                } catch (_: Exception) {
                    "Error de autenticación ($code)"
                }
                return@withContext Result.failure(Exception(errMessage))
            }

            val jsonRes = JSONObject(bodyString)
            val token = jsonRes.optString("token", "")
            val userObj = jsonRes.optJSONObject("user")
                ?: return@withContext Result.failure(Exception("Formato de respuesta inválido: falta objeto 'user'"))

            val tech = TechnicianEntity(
                technicianId = userObj.optString("id", "0"),
                nombre = userObj.optString("nombre", usuarioInput),
                idSala = userObj.optString("idSala", ""),
                sala = userObj.optString("sala", "General"),
                usuario = userObj.optString("usuario", usuarioInput),
                password = passwordInput,
                estatus = "ACTIVO",
                rol = userObj.optString("rol", "ADMIN")
            )

            Result.success(LoginResult(token = token, technician = tech))
        } catch (e: Exception) {
            Log.e(TAG, "Error en login API", e)
            Result.failure(e)
        }
    }

    /**
     * GET /api/maquinas
     */
    suspend fun getMaquinas(
        baseUrl: String,
        sala: String? = null
    ): Result<List<MachineEntity>> = withContext(Dispatchers.IO) {
        try {
            var url = "${baseUrl.removeSuffix("/")}/api/maquinas"
            if (!sala.isNullOrBlank()) {
                url += "?sala=${java.net.URLEncoder.encode(sala, "UTF-8")}"
            }

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Error al consultar máquinas: HTTP ${response.code}"))
            }

            val jsonRes = JSONObject(bodyString)
            val dataArr = jsonRes.optJSONArray("data") ?: JSONArray()
            val machines = mutableListOf<MachineEntity>()

            for (i in 0 until dataArr.length()) {
                val obj = dataArr.getJSONObject(i)
                val rawProp = obj.optString("propietario", "PROVEEDOR").trim().uppercase()
                val cleanProp = if (rawProp == "PROPIA" || rawProp == "WINPOT") "PROPIA" else "PROVEEDOR"

                machines.add(
                    MachineEntity(
                        machineNumber = obj.optString("numeroMaquina", obj.optString("asset", "")),
                        assetNumber = obj.optString("asset", ""),
                        serialNumber = obj.optString("serie", ""),
                        brand = obj.optString("marca", ""),
                        model = obj.optString("modelo", ""),
                        area = obj.optString("area", "SALA"),
                        game = obj.optString("juego", ""),
                        island = obj.optString("isla", ""),
                        sala = obj.optString("sala", ""),
                        qrId = obj.optString("qrId", ""),
                        propietario = cleanProp
                    )
                )
            }

            Result.success(machines)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener máquinas desde la API", e)
            Result.failure(e)
        }
    }

    /**
     * GET /api/proveedores
     */
    suspend fun getProveedores(baseUrl: String): Result<List<ProviderEmailEntity>> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.removeSuffix("/")}/api/proveedores"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Error al consultar proveedores: HTTP ${response.code}"))
            }

            val jsonRes = JSONObject(bodyString)
            val dataArr = jsonRes.optJSONArray("data") ?: JSONArray()
            val providers = mutableListOf<ProviderEmailEntity>()

            for (i in 0 until dataArr.length()) {
                val obj = dataArr.getJSONObject(i)
                val provName = obj.optString("proveedor", "").trim()
                val email = obj.optString("email", "").trim()
                val cc = obj.optString("ccEmails", "").trim()

                if (provName.isNotBlank() && email.isNotBlank()) {
                    providers.add(
                        ProviderEmailEntity(
                            id = obj.optInt("id", 0),
                            providerName = provName,
                            email = email,
                            ccEmails = cc
                        )
                    )
                }
            }

            Result.success(providers)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener proveedores desde la API", e)
            Result.failure(e)
        }
    }

    /**
     * GET /api/incidencias
     */
    suspend fun getIncidencias(
        baseUrl: String,
        token: String? = null
    ): Result<List<IncidenciaItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.removeSuffix("/")}/api/incidencias"
            val reqBuilder = Request.Builder().url(url).get()
            if (!token.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer $token")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Error al consultar incidencias: HTTP ${response.code}"))
            }

            val jsonRes = JSONObject(bodyString)
            val dataArr = jsonRes.optJSONArray("data") ?: JSONArray()
            val items = mutableListOf<IncidenciaItem>()

            for (i in 0 until dataArr.length()) {
                val obj = dataArr.getJSONObject(i)
                val rawProp = obj.optString("propietario", "PROPIA").trim().uppercase()
                val cleanProp = if (rawProp == "PROPIA" || rawProp == "WINPOT") "PROPIA" else "PROVEEDOR"

                items.add(
                    IncidenciaItem(
                        idTicket = obj.optString("idTicket", obj.optString("id", "")),
                        sala = obj.optString("sala", ""),
                        marca = obj.optString("marca", ""),
                        modelo = obj.optString("modelo", ""),
                        serie = obj.optString("serie", ""),
                        asset = obj.optString("asset", ""),
                        area = obj.optString("area", ""),
                        propietario = cleanProp,
                        operativa = obj.optString("operativa", "NO"),
                        estadoTicket = obj.optString("estadoTicket", "ABIERTO"),
                        fechaOrigen = obj.optString("fechaOrigen", ""),
                        fechaReparacion = obj.optString("fechaReparacion", ""),
                        falla = obj.optString("falla", ""),
                        prioridad = obj.optString("prioridad", "MEDIA"),
                        idTecnico = obj.optString("idTecnico", ""),
                        tecnico = obj.optString("tecnico", obj.optString("asignado", "")),
                        resolucion = obj.optString("resolucion", "")
                    )
                )
            }

            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error al consultar incidencias desde la API", e)
            Result.failure(e)
        }
    }

    /**
     * POST /api/incidencias
     */
    suspend fun createIncidencia(
        baseUrl: String,
        payload: IncidenciaTicketPayload,
        token: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.removeSuffix("/")}/api/incidencias"
            val reqBuilder = Request.Builder()
                .url(url)
                .post(payload.toJsonString().toRequestBody(jsonMediaType))

            if (!token.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer $token")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Error al crear ticket en API: HTTP ${response.code}"))
            }

            val jsonRes = try { JSONObject(bodyString) } catch (_: Exception) { JSONObject() }
            val ticketIdResult = jsonRes.optString("idTicket", jsonRes.optString("id_ticket", payload.idTicket))

            Result.success(ticketIdResult)
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear ticket en la API", e)
            Result.failure(e)
        }
    }

    /**
     * POST /api/incidencias/update-status
     */
    suspend fun updateIncidenciaStatus(
        baseUrl: String,
        idTicket: String,
        nuevoEstado: String,
        resolucion: String,
        operativa: String,
        token: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.removeSuffix("/")}/api/incidencias/update-status"
            val json = JSONObject().apply {
                put("idTicket", idTicket)
                put("nuevoEstado", nuevoEstado)
                put("resolucion", resolucion)
                put("operativa", operativa)
            }

            val reqBuilder = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(jsonMediaType))

            if (!token.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer $token")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Error al actualizar estatus: HTTP ${response.code}"))
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar estado del ticket en la API", e)
            Result.failure(e)
        }
    }

    private fun String?.isNullOrBlank(): Boolean = this == null || this.trim().isEmpty()
}
