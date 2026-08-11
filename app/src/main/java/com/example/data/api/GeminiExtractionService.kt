package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ExtractedMachineData(
    val serialNumber: String = "",
    val brand: String = "",
    val model: String = "",
    val assetNumber: String = "",
    val machineNumber: String = "",
    val issueDescription: String = "",
    val rawSummary: String = ""
)

object GeminiExtractionService {
    private const val TAG = "GeminiService"
    private const val MODEL = "gemini-3.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun extractDataFromTextOrDocument(
        textInput: String,
        mimeType: String = "text/plain",
        base64Data: String? = null
    ): ExtractedMachineData = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not set or using default placeholder.")
            return@withContext parseWithFallbackRegex(textInput)
        }

        try {
            val jsonRequest = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            val promptText = """
                Analiza la información proporcionada y extrae únicamente en formato JSON los siguientes campos de la máquina o dispositivo:
                - serialNumber (Número de serie / S/N)
                - brand (Marca / Fabricante, ej. Zitro, IGT, Aristocrat, Novomatic)
                - model (Modelo de la máquina)
                - assetNumber (Número de asset, etiqueta o no. económico)
                - machineNumber (Número de la máquina o ID)
                - issueDescription (Descripción de la falla si existe)
                
                Devuelve estrictamente un objeto JSON válido con estas claves:
                {"serialNumber":"","brand":"","model":"","assetNumber":"","machineNumber":"","issueDescription":""}
                
                Texto o contexto a analizar:
                $textInput
            """.trimIndent()

            val textPart = JSONObject().apply {
                put("text", promptText)
            }
            partsArray.put(textPart)

            if (!base64Data.isNullOrEmpty()) {
                val inlineData = JSONObject().apply {
                    put("mimeType", mimeType)
                    put("data", base64Data)
                }
                val mediaPart = JSONObject().apply {
                    put("inlineData", inlineData)
                }
                partsArray.put(mediaPart)
            }

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            jsonRequest.put("contents", contentsArray)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonRequest.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error ${response.code}: $responseBody")
                return@withContext parseWithFallbackRegex(textInput)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            parseExtractedJsonResponse(rawText)
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API", e)
            parseWithFallbackRegex(textInput)
        }
    }

    private fun parseExtractedJsonResponse(rawText: String): ExtractedMachineData {
        return try {
            val cleanJson = rawText.substringAfter("{").substringBeforeLast("}")
            val jsonObject = JSONObject("{$cleanJson}")
            ExtractedMachineData(
                serialNumber = jsonObject.optString("serialNumber", ""),
                brand = jsonObject.optString("brand", ""),
                model = jsonObject.optString("model", ""),
                assetNumber = jsonObject.optString("assetNumber", ""),
                machineNumber = jsonObject.optString("machineNumber", ""),
                issueDescription = jsonObject.optString("issueDescription", ""),
                rawSummary = rawText
            )
        } catch (e: Exception) {
            parseWithFallbackRegex(rawText)
        }
    }

    private fun parseWithFallbackRegex(text: String): ExtractedMachineData {
        var serial = ""
        var brand = ""
        var model = ""
        var asset = ""
        var machine = ""
        var issue = ""

        val lines = text.lines()
        for (line in lines) {
            val lower = line.lowercase()
            when {
                lower.contains("serie") || lower.contains("s/n") || lower.contains("serial") -> {
                    serial = line.substringAfter(":").ifEmpty { line.substringAfter(" ").trim() }.trim()
                }
                lower.contains("marca") || lower.contains("brand") -> {
                    brand = line.substringAfter(":").ifEmpty { line.substringAfter(" ").trim() }.trim()
                }
                lower.contains("modelo") || lower.contains("model") -> {
                    model = line.substringAfter(":").ifEmpty { line.substringAfter(" ").trim() }.trim()
                }
                lower.contains("asset") || lower.contains("etiq") || lower.contains("economico") -> {
                    asset = line.substringAfter(":").ifEmpty { line.substringAfter(" ").trim() }.trim()
                }
                lower.contains("maquina") || lower.contains("máquina") || lower.contains("terminal") -> {
                    machine = line.substringAfter(":").ifEmpty { line.substringAfter(" ").trim() }.trim()
                }
                lower.contains("falla") || lower.contains("problema") || lower.contains("reporta") -> {
                    issue = line.trim()
                }
            }
        }

        return ExtractedMachineData(
            serialNumber = serial,
            brand = brand,
            model = model,
            assetNumber = asset,
            machineNumber = machine,
            issueDescription = issue,
            rawSummary = text
        )
    }
}
