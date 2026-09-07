package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object EmailIntentUtil {

    fun sendViaGmail(context: Context, recipient: String, subject: String, body: String, cc: String = ""): Boolean {
        val toArray = parseEmailArray(recipient)
        val ccArray = parseEmailArray(cc)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "plain/text"
            setPackage("com.google.android.gm")
            if (toArray.isNotEmpty()) putExtra(Intent.EXTRA_EMAIL, toArray)
            if (ccArray.isNotEmpty()) putExtra(Intent.EXTRA_CC, ccArray)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to mailto scheme with Gmail package
            val mailtoIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = buildMailtoUri(recipient, cc, subject, body)
                setPackage("com.google.android.gm")
            }
            try {
                context.startActivity(mailtoIntent)
                true
            } catch (ex: Exception) {
                // If Gmail isn't available, open standard email chooser
                sendViaStandardEmailChooser(context, recipient, subject, body, cc)
                false
            }
        }
    }

    fun sendViaOutlook(context: Context, recipient: String, subject: String, body: String, cc: String = ""): Boolean {
        val toArray = parseEmailArray(recipient)
        val ccArray = parseEmailArray(cc)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "plain/text"
            setPackage("com.microsoft.office.outlook")
            if (toArray.isNotEmpty()) putExtra(Intent.EXTRA_EMAIL, toArray)
            if (ccArray.isNotEmpty()) putExtra(Intent.EXTRA_CC, ccArray)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            val mailtoIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = buildMailtoUri(recipient, cc, subject, body)
                setPackage("com.microsoft.office.outlook")
            }
            try {
                context.startActivity(mailtoIntent)
                true
            } catch (ex: Exception) {
                sendViaStandardEmailChooser(context, recipient, subject, body, cc)
                false
            }
        }
    }

    fun sendViaStandardEmailChooser(context: Context, recipient: String, subject: String, body: String, cc: String = "") {
        val uri = buildMailtoUri(recipient, cc, subject, body)
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        val toArray = parseEmailArray(recipient)
        val ccArray = parseEmailArray(cc)
        if (toArray.isNotEmpty()) intent.putExtra(Intent.EXTRA_EMAIL, toArray)
        if (ccArray.isNotEmpty()) intent.putExtra(Intent.EXTRA_CC, ccArray)

        try {
            context.startActivity(Intent.createChooser(intent, "Enviar correo con..."))
        } catch (e: Exception) {
            Toast.makeText(context, "No se encontró cliente de correo instalado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseEmailArray(input: String): Array<String> {
        if (input.isBlank()) return emptyArray()
        return input.split(',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toTypedArray()
    }

    private fun buildMailtoUri(recipient: String, cc: String, subject: String, body: String): Uri {
        val base = "mailto:${Uri.encode(recipient.trim())}"
        val params = mutableListOf<String>()
        if (cc.isNotBlank()) {
            params.add("cc=${Uri.encode(cc.trim())}")
        }
        if (subject.isNotBlank()) {
            params.add("subject=${Uri.encode(subject)}")
        }
        if (body.isNotBlank()) {
            params.add("body=${Uri.encode(body)}")
        }
        val uriStr = if (params.isNotEmpty()) {
            "$base?${params.joinToString("&")}"
        } else {
            base
        }
        return Uri.parse(uriStr)
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }
}
