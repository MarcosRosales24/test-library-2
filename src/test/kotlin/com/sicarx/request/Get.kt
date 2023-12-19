package org.example.request

import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders

fun String.getLog(headersMap: Map<String, String> = emptyMap()): ResponseEntity<String> {
    val client = OkHttpClient()

    val requestBuilder = Request.Builder().url(this).get()
    headersMap.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
    val request = requestBuilder.build()

    client.newCall(request).execute().use {
        val responseBody: String = it.body?.string() ?: ""
        val status = HttpStatus.resolve(it.code) ?: HttpStatus.INTERNAL_SERVER_ERROR

        val responseHeaders = HttpHeaders()
        it.headers.names().forEach { name -> responseHeaders[name] = it.headers[name] }

        return ResponseEntity(responseBody, responseHeaders, status)
    }
}
