package com.resonance.hawk.proxy

interface InterceptCallback {
    suspend fun onRequest(request: Request): Triple<String, String, Int>?
    suspend fun onResponse(response: Response): String?
}

class InterceptCallbackBuilder() {
    private var onRequest: ((request: Request) -> Triple<String, String, Int>?)? = null
    private var onResponse: ((response: Response) -> String?)? = null
    private val result = object : InterceptCallback {
        override suspend fun onRequest(request: Request): Triple<String, String, Int>? {
            return onRequest?.invoke(request)
        }

        override suspend fun onResponse(response: Response): String? {
            return onResponse?.invoke(response)
        }
    }

    fun onRequest(function: (request: Request) -> Triple<String, String, Int>?): InterceptCallbackBuilder {
        onRequest = function
        return this
    }

    fun onResponse(function: (response: Response) -> String?): InterceptCallbackBuilder {
        onResponse = function
        return this
    }

    fun build() = result
}