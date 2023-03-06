package com.grassroot.academy.authentication

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import com.grassroot.academy.core.EdxDefaultModule.ProviderEntryPoint
import com.grassroot.academy.model.authentication.AuthResponse
import com.grassroot.academy.task.Task

abstract class LoginTask(
    context: Context,
    private val username: String,
    private val password: String
) : Task<AuthResponse?>(context) {

    var loginAPI = EntryPointAccessors
        .fromApplication(context, ProviderEntryPoint::class.java).getLoginAPI()

    override fun doInBackground(vararg voids: Void): AuthResponse? {
        try {
            return loginAPI.logInUsingEmail(username, password)
        } catch (e: Exception) {
            e.printStackTrace()
            handleException(e)
        }
        return null
    }
}
