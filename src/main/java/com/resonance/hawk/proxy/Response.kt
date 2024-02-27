package com.resonance.hawk.proxy

import com.resonance.hawk.util.*
import org.json.JSONObject
import java.io.InputStream

class Response(
    val version: String, val status: Int, val message: String,
    val header: HashMap<String, String>, val body: ByteArray,
    val encoding: String
) {
    val bytes by lazy {
        var headerString = ""
        header.forEach { (key, value) -> headerString += "$key: $value$SEPARATOR" }
        if (header["Transfer-Encoding"] == null)
            "$version $status $message$SEPARATOR$headerString$SEPARATOR".toByteArray() + body
        else
            "$version $status $message$SEPARATOR$headerString$SEPARATOR${Integer.toHexString(body.size)}$SEPARATOR".toByteArray() + body + "${SEPARATOR}0$SEPARATOR$SEPARATOR".toByteArray()
    }

    override fun toString(): String {
        var headerString = ""
        header.forEach { (key, value) ->
            headerString += "$key: $value$SEPARATOR"
        }
        return "$version $status $message$SEPARATOR$headerString$SEPARATOR${body.decode(encoding).decodeToString()}"
    }

    fun toEntity(): com.resonance.hawk.db.Response {
        return com.resonance.hawk.db.Response(
            version,
            status,
            message,
            header = JSONObject(header.toMap()).toString(),
            body = body.toHex(),
            encoding
        )
    }
    companion object {
        suspend fun fromInputStream(inputStream: InputStream): Response {
            val httpContent = HttpContents(inputStream)
            val headerLines = httpContent.readHeader()
            val leadLineSplit = headerLines[0].split("\\s+".toRegex())
            val version = leadLineSplit[0]
            val status = leadLineSplit[1].trim().toInt()
            val message = leadLineSplit.toString(separator = " ", start = 2)
            val header = parseHeader(headerLines, start = 1)
            val encoding = header["Content-Encoding"] ?: "none"
            val body = httpContent.transfer(
                header["Content-Length"]?.toInt(),
                header["Transfer-Encoding"]
            )
            return Response(
                version,
                status,
                message,
                header,
                body,
                encoding
            )
        }
    }
}
