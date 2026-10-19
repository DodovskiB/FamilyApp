package com.example.familyapp.data

import java.net.URI
import java.util.Locale

object ProductLinks {
    fun normalize(input: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return null
        val value = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(text)) text else "https://$text"
        val uri = URI(value)
        require(uri.scheme.lowercase(Locale.ROOT) in setOf("http", "https") &&
            !uri.host.isNullOrBlank() && uri.rawUserInfo == null) { "Enter a valid http or https link" }
        return uri.toASCIIString()
    }
}
