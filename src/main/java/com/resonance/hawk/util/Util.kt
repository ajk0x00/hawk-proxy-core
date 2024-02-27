package com.resonance.hawk.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.net.Socket
import java.util.zip.GZIPInputStream

fun List<String>.toString(separator:String = "", start: Int = 0): String {
    var string = ""
    for (i: Int in start until this.lastIndex)
        string += "${this[i]}$separator"
    string += this[this.lastIndex]
    return string
}

fun ByteArray.decode(type: String): ByteArray{
    val output = ByteArrayOutputStream()
    if (type == "gzip"){
        if (this.isEmpty()) return this
        val buffer = ByteArray(1024*1024)
        val gzip = GZIPInputStream(ByteArrayInputStream(this))
        var readEnd = gzip.read(buffer)
        while (readEnd > -1){
            output.write(buffer, 0, readEnd)
            readEnd = gzip.read(buffer)
        }
    }else{
        println("Unsupported Encoding")
        return this
    }
    return output.toByteArray()
}

fun Socket.write(byteArray: ByteArray){
    val writer = this.getOutputStream()
    writer.write(byteArray)
    writer.flush()
}
fun ByteArray.toHex(start: Int = 0, end: Int = this.lastIndex): String {
    var str = ""
    for (i in start..end)
        str += String.format("%02X", this[i])
    return str
}
fun String.decodeHex(): ByteArray {
    require(length % 2 == 0) { "Must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun jsonToHashMap(data: String): HashMap<String, String> = Gson().fromJson(
        data,
        object : TypeToken<HashMap<String?, String?>?>() {}.type
    )

fun parseHeader(
    header: List<String>,
    start: Int = 0,
    end: Int = header.size
): HashMap<String, String> {
    val hashMap = HashMap<String, String>()
    for (i: Int in start until end) {
        val line = header[i]
        if (line.contains(":"))
            line.split(":").also { hashMap[it[0]] = it.toString(start = 1).trim() }
    }
    return hashMap
}