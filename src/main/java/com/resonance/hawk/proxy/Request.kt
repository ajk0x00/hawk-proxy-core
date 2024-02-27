package com.resonance.hawk.proxy

import com.resonance.hawk.util.*
import org.json.JSONObject
import java.io.InputStream
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class Request(
    val host: String, val port: Int, val method: String,
    val path: String, val version: String, val header: HashMap<String, String>,
    val body: ByteArray, val https: Boolean
) {
    private val factory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
    private val bytes by lazy {
        var stringHeader = ""
        header["Connection"] =
            "close"                                                  //TODO Dumb proxy
        header.forEach { (key, value) -> stringHeader += "$key: $value$SEPARATOR" }
        "$method $path $version$SEPARATOR$stringHeader$SEPARATOR".toByteArray() + body
    }

    override fun toString(): String {
        var headerString = ""
        header.forEach { (key, value) ->
            headerString += "$key: $value$SEPARATOR"
        }
        return "$method $path $version$SEPARATOR$headerString$SEPARATOR${body.decode("none").decodeToString()}"
    }

    fun toEntity(): com.resonance.hawk.db.Request{
        return com.resonance.hawk.db.Request(
            host,
            port,
            method,
            path,
            version,
            header = JSONObject(header.toMap()).toString(),
            body = body.toHex(),
            https
        )
    }

    suspend fun getResponse(): Response {
        val socket: Socket = if (https) {
            factory.createSocket(host, port) as SSLSocket
        } else
            Socket(host, port)
        socket.write(bytes)
        return Response.fromInputStream(socket.getInputStream())
    }

    companion object {
        suspend fun fromInputStream(
            inputStream: InputStream,
            connCred: String?
        ): Request {
            val host: String
            val port: Int
            val method: String
            val path: String
            val version: String
            val header: HashMap<String, String>
            val body: ByteArray
            val https = connCred != null

            val httpContents = HttpContents(inputStream)
            val headerLines = httpContents.readHeader()
            val leadLineSplit = headerLines[0].split("\\s+".toRegex())
            if (https) {
                val split = connCred!!.split(":")
                host = split[0].trim()
                port = split[1].trim().toInt()
                path = leadLineSplit[1].trim()
            } else {
                val urlElements = httpContents.getUrlElements(leadLineSplit[1])
                host = urlElements[0].trim()
                port = urlElements[1].trim().toInt()
                path = urlElements[2].trim()
            }
            method = httpContents.getMethod(leadLineSplit[0])
            version = leadLineSplit[2]
            header = parseHeader(headerLines, 1)
            body = httpContents.transfer(
                header["Content-Length"]?.toInt(),
                header["Transfer-Encoding"]
            )
            return Request(
                host,
                port,
                method,
                path,
                version,
                header,
                body,
                https
            )
        }

        fun fromDbObject(req: com.resonance.hawk.db.Request) = Request(
            req.host,
            req.port,
            req.method,
            req.path,
            req.version,
            header = jsonToHashMap(req.header),
            body = req.body.decodeHex(),
            req.https
        )

        fun fromString(data: String, host: String, port: Int, https: Boolean, adjustLength: Boolean): Request {
            val headerBody = data.split("\\r?\\n\\r?\\n".toRegex())
            val lines = headerBody[0].split("\\r?\\n".toRegex())
            val leadLine = lines[0].split("\\s+".toRegex())
            val method = leadLine[0]
            val path = leadLine[1]
            val version = leadLine[2]
            val header = parseHeader(lines, 1)

            if (adjustLength)
                header["Content-Length"] = if (headerBody.lastIndex > 0) headerBody[1].length.toString() else "0"
            header.remove("Content-Encoding") // TODO find a way out
            return Request(
                host,
                port,
                method,
                path,
                version,
                header,
                body = if (headerBody.lastIndex > 0) headerBody[1].toByteArray() else "".toByteArray(),
                https
            )
        }

    }
}
