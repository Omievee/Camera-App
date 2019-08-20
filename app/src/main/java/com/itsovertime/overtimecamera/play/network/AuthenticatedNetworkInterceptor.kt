package com.itsovertime.overtimecamera.play.network

import com.itsovertime.overtimecamera.play.utils.Constants
import okhttp3.Interceptor
import okhttp3.Response

class AuthenticatedNetworkInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        requestBuilder.apply {
            header(
                Constants.Authorization,
                "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjp7ImlkIjoiOTQ4OS5BIiwiZHluYW1vZGJfaWQiOiI5NDg5LkEiLCJyb2xlcyI6W10sInVzZXJuYW1lIjoib21pZXZlZSIsInV1aWQiOiI0YWJlZjBiNC1iMjc0LTQ5ZTgtYWVkMy01ZTEwYjNhMDU2MTEifSwiaWF0IjoxNTYzMzg4MjcwfQ.O3aIIbwEDoyO2DJZmI0eCo7DzQuC3fcNdcQiWa3Hujc"
            )
        }
        return chain.proceed(requestBuilder.build())
    }
}

//Todo: User pref for tokens..