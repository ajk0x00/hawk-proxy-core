package com.resonance.hawk.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.resonance.hawk.util.decode
import com.resonance.hawk.util.decodeHex
import com.resonance.hawk.util.SEPARATOR
import org.json.JSONObject

@Entity
data class Response(
    val version: String,
    val status: Int,
    val message: String,
    val header: String,
    val body: String,
    val encoding: String
){
    override fun toString(): String {
        var headerString = ""
        val obj = JSONObject(header)
        for (key: String in obj.keys())
            headerString += "$key: ${obj.get(key)}$SEPARATOR"
        return "$version $status $message$SEPARATOR$headerString$SEPARATOR${body.decodeHex().decode(encoding).decodeToString()}"
    }
}

@Entity
data class Request(
    val host: String,
    val port: Int,
    val method: String,
    val path: String,
    val version: String,
    val header: String,
    val body: String,
    val https: Boolean
){
    override fun toString(): String {
        var headerString = ""
        val obj = JSONObject(header)
        val encoding = if (obj.has("Content-Encoding")) obj.getString("Content-Encoding") else "none"
        for (key: String in obj.keys())
            headerString += "$key: ${obj.get(key)}$SEPARATOR"
        return "$method $path $version$SEPARATOR$headerString$SEPARATOR${body.decodeHex().decode(encoding).decodeToString()}"
    }
}

@Entity
data class RequestHistory (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val time: Long,
    @Embedded(prefix = "orig_req") val originalRequest: Request,
    @Embedded(prefix = "edit_req") val editedRequest: Request?,
    @Embedded(prefix = "orig_res") val originalResponse: Response,
    @Embedded(prefix = "edit_res") val editedResponse: Response?
)