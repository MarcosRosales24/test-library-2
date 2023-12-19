package org.example.request

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

fun String.postLog(jsonBody: String, headersMap: Map<String, String> = emptyMap()): ResponseEntity<String> {
    val client = OkHttpClient()
    val mediaType = "application/json".toMediaTypeOrNull()
    val requestBody = jsonBody.toRequestBody(mediaType)

    val requestBuilder = Request.Builder().url(this).post(requestBody)
    headersMap.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
    val request = requestBuilder.build()

    client.newCall(request).execute().use { response ->
        val responseBody = response.body?.string() ?: ""
        val status = HttpStatus.resolve(response.code) ?: HttpStatus.INTERNAL_SERVER_ERROR

        val responseHeaders = HttpHeaders()
        response.headers.names().forEach { name -> responseHeaders[name] = response.headers[name] }
        return ResponseEntity(responseBody, responseHeaders, status)
    }
}