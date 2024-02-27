package com.resonance.hawk.proxy

import com.resonance.hawk.db.RequestHistory
import com.resonance.hawk.db.RequestHistoryDao
import com.resonance.hawk.util.write
import kotlinx.coroutines.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.*

class HawkProxy(val dao: RequestHistoryDao, port: Int) {
    private var switch = true
    private val server = ServerSocket(port)
    private var serverCoroutine: Job? = null
    private val uniSocket = UniSocket()
    var callback: InterceptCallback? = null

    private suspend fun handle(client: Socket) {
        try {
            val (socket, connCred) = uniSocket.create(client)
            val request = Request.fromInputStream(socket.getInputStream(), connCred)
            val onReq = callback?.onRequest(request)
            if (onReq != null) {
                val (result,_, _) = onReq
                if (result.isEmpty()) return
                TODO("Request edited action logic ! reassign request with the edited request")
            }
            val response = request.getResponse()
            val result = callback?.onResponse(response)
            if (result != null) {
                if (result.isEmpty()) return
                TODO("Response edited action logic ! reassign request with the edited response")
            }
            println(response)
            socket.write(response.bytes)
            dao.insert(RequestHistory(
                id = 0,      // auto generated field
                time = Date().time,
                originalRequest = request.toEntity(),
                editedRequest = null,
                originalResponse = response.toEntity(),
                editedResponse = null
            ))
            socket.close()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    suspend fun start() = withContext(Dispatchers.IO) {
        serverCoroutine = launch {
            while (switch) {
                try {
                    val client = server.accept()
                    launch { handle(client) }
                }catch (e: SocketException){ e.printStackTrace() }
            }
        }
    }

    suspend fun stop() {
        switch = false
        try { server.close() }catch (ee: Exception){}
        serverCoroutine?.cancelAndJoin()
        server.close()
    }
}

