package com.resonance.hawk.proxy

import com.resonance.hawk.util.toString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.InputStream

@Suppress("DEPRECATION")
class HttpContents(inputStream: InputStream) {
    private val inputStream = DataInputStream(inputStream)

    suspend fun readHeader(): ArrayList<String> = withContext(Dispatchers.IO) {
        var inputLine: String
        val list = ArrayList<String>()
        while (inputStream.readLine().also { inputLine = it ?: "" } != "") {
            list.add(inputLine)
        }
        list
    }

    fun getMethod(method: String): String {
        return when (method) {
            "ET" -> "GET"
            "OST" -> "POST"
            "EAD" -> "HEAD"
            "UT" -> "PUT"
            "ELETE" -> "DELETE"
            "ONNECT" -> "CONNECT"
            "PTIONS" -> "OPTIONS"
            "RACE" -> "TRACE"
            else -> method
        }
    }

    fun getUrlElements(url: String): Array<String> {
        return if (url.contains("://")) {
            val first = url.split("://")
            println(url)
            val second = first[1].split("/")
            val port: String
            val host: String
            if (second[0].contains(":")) second[0].split(":").also { host = it[0]; port = it[1] }
            else host = second[0].also { port = "80" }
            val path =
                "/${if (second.lastIndex > 0) second.toString(separator = "/", start = 1) else ""}"
            arrayOf(host, port, path)
        } else {
            val first = url.split(":")
            arrayOf(first[0], first[1], "/")
        }

    }

    private fun getTransferEncodedBody(): ByteArray {
        val output = ByteArrayOutputStream()
        while (true) {
            val length = Integer.parseInt(inputStream.readLine(), 16)
            if (length < 1) break
            val buffer = ByteArray(length)
            inputStream.readFully(buffer)
            output.write(buffer)
            inputStream.readShort()
        }
        return output.toByteArray()
    }

    private fun getContentLengthBody(length: Int): ByteArray {
        val buffer = ByteArray(length)
        inputStream.readFully(buffer)
        return buffer
    }

    fun transfer(cl: Int?, te: String?): ByteArray {
        return if (cl == null)
            if (te == null)
                ByteArray(0)
            else
                getTransferEncodedBody()
        else
            getContentLengthBody(cl)
    }
}