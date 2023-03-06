package com.grassroot.academy.http.interceptor

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import com.grassroot.academy.core.EdxDefaultModule.ProviderEntryPoint
import com.grassroot.academy.module.prefs.LoginPrefs
import com.grassroot.academy.util.AppConstants.HEADER_KEY_AUTHORIZATION
import java.io.IOException

/**
 * Injects OAuth token into Authorization header if present
 */
class OauthHeaderRequestInterceptor(
    context: Context
) : Interceptor {
    private val loginPrefs: LoginPrefs

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder: Request.Builder = chain.request().newBuilder()
        val token = loginPrefs.authorizationHeader
        if (token != null) {
            builder.addHeader(HEADER_KEY_AUTHORIZATION, token)
        }
        return chain.proceed(builder.build())
    }

    init {
        loginPrefs = EntryPointAccessors.fromApplication(
            context,
            ProviderEntryPoint::class.java
        ).getLoginPrefs()
    }
}
