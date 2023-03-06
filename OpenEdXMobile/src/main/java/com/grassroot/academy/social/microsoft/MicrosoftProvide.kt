package com.grassroot.academy.social.microsoft

import android.content.Context
import android.text.TextUtils
import dagger.hilt.android.EntryPointAccessors
import okhttp3.Request
import com.grassroot.academy.core.EdxDefaultModule
import com.grassroot.academy.http.callback.ErrorHandlingOkCallback
import com.grassroot.academy.http.provider.OkHttpClientProvider
import com.grassroot.academy.social.SocialFactory
import com.grassroot.academy.social.SocialLoginDelegate
import com.grassroot.academy.social.SocialMember
import com.grassroot.academy.social.SocialProvider

class MicrosoftProvide : SocialProvider {

    override fun login(context: Context?, callback: SocialProvider.Callback<Void>?) {
        throw UnsupportedOperationException("Not implemented / Not supported")
    }

    override fun getUserInfo(
        context: Context?, socialType: SocialFactory.SOCIAL_SOURCE_TYPE?,
        accessToken: String?,
        userInfoCallback: SocialLoginDelegate.SocialUserInfoCallback?
    ) {
        context?.run {
            val okHttpClientProvider: OkHttpClientProvider = EntryPointAccessors
                .fromApplication(context, EdxDefaultModule.ProviderEntryPoint::class.java)
                .getOkHttpClientProvider()
            okHttpClientProvider.get().newCall(
                Request.Builder()
                    .url(MS_GRAPH_URL)
                    .get()
                    .build()
            )
                .enqueue(object : ErrorHandlingOkCallback<MicrosoftUserProfile>(
                    this, MicrosoftUserProfile::class.java, null
                ) {
                    override fun onResponse(userProfile: MicrosoftUserProfile) {
                        var name = userProfile.fullName
                        if (TextUtils.isEmpty(name)) {
                            if (!TextUtils.isEmpty(userProfile.firstName)) {
                                name = userProfile.firstName + " "
                            }
                            if (!TextUtils.isEmpty(userProfile.surName)) {
                                if (TextUtils.isEmpty(name)) {
                                    name = userProfile.surName
                                } else {
                                    name += userProfile.surName
                                }
                            }
                        }
                        userInfoCallback?.setSocialUserInfo(userProfile.email, name)
                    }
                })
        }
    }

    override fun getUser(callback: SocialProvider.Callback<SocialMember>?) {
        throw UnsupportedOperationException("Not implemented / Not supported")
    }

    override fun isLoggedIn(): Boolean {
        throw UnsupportedOperationException("Not implemented / Not supported")
    }

    companion object {
        private const val MS_GRAPH_URL = "https://graph.microsoft.com/v1.0/me"
    }
}
