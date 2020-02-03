package com.itsovertime.overtimecamera.play.network

import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import com.itsovertime.overtimecamera.play.utils.Constants
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthenticatedNetworkInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()



        requestBuilder.apply {
            header(
                Constants.Authorization,
                "Bearer ${UserPreference.authToken}"
            )
        }
        return chain.proceed(requestBuilder.build())
    }
}

