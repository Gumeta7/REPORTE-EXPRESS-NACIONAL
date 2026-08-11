package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object EmailIntentUtil {

    fun sendViaGmail(context: Context, recipient: String, subject: String, body: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "plain/text"
            setPackage("com.google.android.gm")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to mailto scheme with Gmail package
            val mailtoIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${Uri.encode(recipient)}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
                setPackage("com.google.android.gm")
            }
            try {
                context.startActivity(mailtoIntent)
                true
            } catch (ex: Exception) {
                // If Gmail isn't available, open standard email chooser
                sendViaStandardEmailChooser(context, recipient, subject, body)
                false
            }
        }
    }

    fun sendViaOutlook(context: Context, recipient: String, subject: String, body: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "plain/text"
            setPackage("com.microsoft.office.outlook")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            val mailtoIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${Uri.encode(recipient)}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
                setPackage("com.microsoft.office.outlook")
            }
            try {
                context.startActivity(mailtoIntent)
                true
            } catch (ex: Exception) {
                sendViaStandardEmailChooser(context, recipient, subject, body)
                false
            }
        }
    }

    fun sendViaStandardEmailChooser(context: Context, recipient: String, subject: String, body: String) {
        val uri = Uri.parse("mailto:${Uri.encode(recipient)}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        try {
            context.startActivity(Intent.createChooser(intent, "Enviar correo con..."))
        } catch (e: Exception) {
            Toast.makeText(context, "No se encontró cliente de correo instalado", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }
}
