package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object DriveSyncService {
    private const val TAG = "DriveSyncService"

    // Default Spreadsheet URL in Google Drive provided by user
    const val DEFAULT_DRIVE_SHEET_URL =
        "https://docs.google.com/spreadsheets/d/1gMdeRZUao2VWrUsmQ83OMWJxTdFulgYrKyV5XD-Olbs/edit?usp=sharing"

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun normalizeToExportUrl(url: String): String {
        val trimmed = url.trim()
        val sheetIdRegex = Regex("""/spreadsheets/d/([a-zA-Z0-9-_]+)""")
        val match = sheetIdRegex.find(trimmed)
        return if (match != null) {
            val sheetId = match.groupValues[1]
            "https://docs.google.com/spreadsheets/d/$sheetId/export?format=xlsx"
        } else if (trimmed.contains("/export?")) {
            trimmed
        } else {
            "https://docs.google.com/spreadsheets/d/1gMdeRZUao2VWrUsmQ83OMWJxTdFulgYrKyV5XD-Olbs/export?format=xlsx"
        }
    }

    suspend fun downloadSpreadsheetBytes(url: String = DEFAULT_DRIVE_SHEET_URL): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val exportUrl = normalizeToExportUrl(url)
                Log.d(TAG, "Downloading spreadsheet from: $exportUrl")

                val request = Request.Builder()
                    .url(exportUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; ReportesExpress/2.0)")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        val bytes = body.bytes()
                        Log.d(TAG, "Spreadsheet downloaded successfully (${bytes.size} bytes)")
                        return@withContext bytes
                    }
                } else {
                    Log.e(TAG, "Failed to download sheet. HTTP Status: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading spreadsheet from Drive", e)
            }
            return@withContext null
        }
}
