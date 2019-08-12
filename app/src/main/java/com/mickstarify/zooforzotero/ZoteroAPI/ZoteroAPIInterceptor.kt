package com.mickstarify.zooforzotero.ZoteroAPI

import okhttp3.Interceptor
import okhttp3.Response

class ZoteroAPIInterceptor (val API_KEY : String): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        request
            .newBuilder()
            .addHeader("Zotero-API-Version", "3")
            .addHeader("Zotero-API-Key", API_KEY)
            .build()
        return chain.proceed(request)
    }
}