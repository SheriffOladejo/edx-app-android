package com.grassroot.academy.task;

import android.content.Context;
import android.os.Bundle;

import com.grassroot.academy.authentication.LoginAPI;
import com.grassroot.academy.core.EdxDefaultModule;
import com.grassroot.academy.model.authentication.AuthResponse;
import com.grassroot.academy.social.SocialFactory;

import dagger.hilt.android.EntryPointAccessors;

public abstract class RegisterTask extends Task<AuthResponse> {

    private Bundle parameters;
    private SocialFactory.SOCIAL_SOURCE_TYPE backstoreType;
    private String accessToken;
    private LoginAPI loginAPI;

    public RegisterTask(Context context, Bundle parameters, String accessToken, SocialFactory.SOCIAL_SOURCE_TYPE backstoreType) {
        super(context);
        this.parameters = parameters;
        this.accessToken = accessToken;
        this.backstoreType = backstoreType;
        loginAPI = EntryPointAccessors.fromApplication(
                context, EdxDefaultModule.ProviderEntryPoint.class).getLoginAPI();
    }

    @Override
    protected AuthResponse doInBackground(Void... voids) {
        try {
            switch (backstoreType) {
                case TYPE_GOOGLE:
                    return loginAPI.registerUsingGoogle(parameters, accessToken);
                case TYPE_FACEBOOK:
                    return loginAPI.registerUsingFacebook(parameters, accessToken);
                case TYPE_MICROSOFT:
                    return loginAPI.registerUsingMicrosoft(parameters, accessToken);
            }
            // normal email address login
            return loginAPI.registerUsingEmail(parameters);
        } catch (Exception e) {
            handleException(e);
            return null;
        }
    }
}
