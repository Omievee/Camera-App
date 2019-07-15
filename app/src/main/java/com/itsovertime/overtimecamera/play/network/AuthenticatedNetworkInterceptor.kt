package com.itsovertime.overtimecamera.play.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthenticatedNetworkInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()
        requestBuilder.apply {
           // header(Coxnstants.authorization, "Token token=" + Constants.apiToken)
        }
        return chain.proceed(requestBuilder.build())
    }
}