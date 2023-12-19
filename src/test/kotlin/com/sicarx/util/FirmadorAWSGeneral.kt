package utils

import java.net.URI
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.HashMap

class FirmadorAWSGeneral(
    private val secretKey: String,
    private val accessKey: String
) {

    companion object {
        private const val s3 = "us-east-1"
        private const val service = "lambda"
        private const val credential = "aws4_request"
        private const val HEX_LENGTH_8 = 8
        private const val FF_LOCATION = 6
    }

    private val formatter = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale("en", "US", "POSIX")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun signBody(body: String, url: URI, method: String): Map<String, String> {
        val headers = HashMap<String, String>()
        val bodyDigest = getHasBody(body)
        val date = formatter.format(Date())


        val signatureNode = getSignatureNode(
            date = date,
            bodyDigest = bodyDigest,
            method = method,
            url,
            secretKey = secretKey,

            )


        val bodyHeader = getBodyHeader(bodyDigest)
        headers[bodyHeader.first] = bodyHeader.second

        val dateHeader = getDateHeader(date)
        headers[dateHeader.first] = dateHeader.second

        val authHeader = getAuthHeader(date = date, signatureNode = signatureNode, accessKey = accessKey)
        headers[authHeader.first] = authHeader.second

        val signedHeaders = getSignedHeaders()
        headers[signedHeaders.first] = signedHeaders.second

        val signatureHeader = getSignatureHeader(signatureNode)
        headers[signatureHeader.first] = signatureHeader.second

        headers["x-amz-acl"] = "public-read"

        return headers
    }

    private fun getSignatureHeader(signatureNode: String): Pair<String, String> {
        return "Signature" to signatureNode
    }

    private fun getAuthHeader(
        signatureNode: String,
        date: String,
        accessKey: String
    ): Pair<String, String> {
        return "Authorization" to signingHeader(
            signatureNode = signatureNode,
            date = date,
            accessKey = accessKey
        )

    }

    private fun getBodyHeader(bodyDigest: String): Pair<String, String> {
        return "X-Amz-Content-Sha256" to bodyDigest
    }

    private fun getSignedHeaders(): Pair<String, String> {
        return "SignedHeaders" to "host;x-amz-acl;x-amz-content-sha256;x-amz-date"
    }

    private fun getDateHeader(date: String): Pair<String, String> {
        return "X-Amz-Date" to date
    }

    fun signingHeader(
        signatureNode: String,
        date: String,
        accessKey: String
    ): String {


        return "AWS4-HMAC-SHA256 Credential=${
            getCredentialNode(
                date = date,
                accessKey = accessKey
            )
        }, " +
                "SignedHeaders=host;x-amz-acl;x-amz-content-sha256;x-amz-date, " +
                "Signature=${signatureNode}"
    }

    private fun getCredentialNode(date: String, accessKey: String): String {
        return "${accessKey}/${canonicalScope(date)}"
    }

    fun getSignatureNode(
        date: String,
        bodyDigest: String,
        method: String,
        url: URI,
        secretKey: String

    ): String {
        val secretFormat = "AWS4$secretKey"
        val dateHmac =
            hmac(date.substring(0, 8), secretFormat.toByteArray(Charsets.UTF_8))
        val regionHmac = hmac(s3, dateHmac)
        val serviceHmac = hmac(service, regionHmac)
        val credentialsHmac = hmac(credential, serviceHmac)


        val licuaChela = """
                AWS4-HMAC-SHA256
                $date
                ${canonicalScope(date)}
                ${
            toHex(
                hash(
                    canonicalRequest(
                        bodyDigest = bodyDigest,
                        date = date,
                        method = method,
                        url = url
                    ).toByteArray(Charsets.UTF_8)
                )
            )
        }
            """.trimIndent()

        val signedFinal = hmac(licuaChela, credentialsHmac)
        return toHex(signedFinal)
    }

    fun getHasBody(body: String): String {
        return toHex(hash(body.toByteArray()))
    }

    private fun hash(data: ByteArray): ByteArray {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(data)
            md.digest()
        } catch (e: Exception) {
            throw Exception(
                "Unable to compute hash while signing request: "
                        + e.message, e
            )
        }
    }

    private fun toHex(data: ByteArray): String {
        val sb = StringBuilder(data.size * 2)
        for (i in data.indices) {
            var hex = Integer.toHexString(data[i].toInt())
            if (hex.length == 1) {
                sb.append("0")
            } else if (hex.length == HEX_LENGTH_8) {
                hex = hex.substring(FF_LOCATION)
            }
            sb.append(hex)
        }
        return sb.toString().lowercase()
    }

    private fun hmac(stringData: String, key: ByteArray): ByteArray {
        try {
            val data = stringData.toByteArray(Charsets.UTF_8)
            return sign(data, key)
        } catch (e: Exception) {
            throw Exception(
                "Unable to calculate a request signature: "
                        + e.message, e
            )
        }
    }

    private fun sign(
        data: ByteArray,
        key: ByteArray,
    ): ByteArray {
        try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            return mac.doFinal(data)
        } catch (e: Exception) {
            throw Exception(
                ("Unable to calculate a request signature: "
                        + e.message), e
            )
        }
    }

    private fun canonicalScope(date: String): String {
        return "${date.substring(0, 8)}/$s3/$service/$credential"
    }

    private fun canonicalRequest(
        bodyDigest: String,
        date: String,
        method: String,
        url: URI
    ): String {
        return "$method\n" +
                "${url.path.ifBlank { "/" }}\n" +
                "${url.query ?: ""}\n" +
                "${signedHeaders(bodyDigest = bodyDigest, date = date, url = url)}\n" +
                "\n" +
                "host;x-amz-acl;x-amz-content-sha256;x-amz-date\n" +
                bodyDigest
    }

    private fun signedHeaders(bodyDigest: String, date: String, url: URI): String {
        return "host:${url.host}\n" +
                "x-amz-acl:public-read\n" +
                "x-amz-content-sha256:${bodyDigest}\n" +
                "x-amz-date:${date}"
    }

}