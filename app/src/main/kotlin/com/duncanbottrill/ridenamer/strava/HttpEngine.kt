package com.duncanbottrill.ridenamer.strava

import com.duncanbottrill.ridenamer.karoo.httpRequest
import io.hammerhead.karooext.KarooSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class HttpResult(val statusCode: Int, val body: String?) {
    val isSuccess: Boolean get() = statusCode in 200..299
}

/**
 * Minimal HTTP abstraction so [StravaClient] can run either through the Karoo system
 * network stack (from the background service) or a plain connection (from the UI,
 * during the OAuth handshake when the Karoo service may not be bound).
 */
fun interface HttpEngine {
    suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: ByteArray?,
    ): HttpResult
}

/** Routes requests through the Karoo system service. */
fun karooHttpEngine(karoo: KarooSystemService) = HttpEngine { method, url, headers, body ->
    val res = karoo.httpRequest(method, url, headers, body)
    HttpResult(res.statusCode, res.body?.toString(Charsets.UTF_8))
}

/** Plain HttpURLConnection — used from the Activity for the OAuth token exchange. */
fun directHttpEngine() = HttpEngine { method, url, headers, body ->
    withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 20_000
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body) }
            }
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }
            HttpResult(code, text)
        } finally {
            conn.disconnect()
        }
    }
}
