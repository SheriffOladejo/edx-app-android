package com.grassroot.academy.social;

import android.app.Activity;
import android.content.Context;

import com.grassroot.academy.social.facebook.FacebookAuth;
import com.grassroot.academy.social.google.GoogleOauth2;
import com.grassroot.academy.social.microsoft.MicrosoftAuth;
import com.grassroot.academy.util.Config;
import com.grassroot.academy.util.NetworkUtil;

public class SocialFactory {

    //TODO - we should create a central place for application wide constants.
    public enum SOCIAL_SOURCE_TYPE {
        TYPE_UNKNOWN(-1, "unknown"), TYPE_GOOGLE(100, "google-oauth2"),
        TYPE_FACEBOOK(101, "facebook"), TYPE_MICROSOFT(102, "microsoft");

        private int code;
        private String value;

        private SOCIAL_SOURCE_TYPE(int code, String value) {
            this.value = value;
            this.code = code;
        }

        public static SOCIAL_SOURCE_TYPE fromString(String source) {
            if ("facebook".equalsIgnoreCase(source))
                return TYPE_FACEBOOK;
            if ("google-oauth2".equalsIgnoreCase(source) || "google".equalsIgnoreCase(source))
                return TYPE_GOOGLE;
            if ("azuread-oauth2".equalsIgnoreCase(source) || "azuread".equalsIgnoreCase(source))
                return TYPE_MICROSOFT;
            return TYPE_UNKNOWN;
        }
    }


    public static ISocial getInstance(Activity activity, SOCIAL_SOURCE_TYPE type, Config config) {
        if (isSocialFeatureEnabled(activity.getApplicationContext(), type, config)) {
            switch (type) {
                case TYPE_GOOGLE:
                    return new GoogleOauth2(activity);
                case TYPE_FACEBOOK:
                    return new FacebookAuth(activity);
                case TYPE_MICROSOFT:
                    return new MicrosoftAuth(activity);
            }
        }
        return new ISocialEmptyImpl();
    }

    public static boolean isSocialFeatureEnabled(Context context, SOCIAL_SOURCE_TYPE type, Config config) {
        boolean isOnZeroRatedNetwork = NetworkUtil.isOnZeroRatedNetwork(context, config);
        if (isOnZeroRatedNetwork)
            return false;
        if (type == SOCIAL_SOURCE_TYPE.TYPE_GOOGLE) {
            return config.getGoogleConfig().isEnabled();
        } else if (type == SOCIAL_SOURCE_TYPE.TYPE_FACEBOOK) {
            return config.getFacebookConfig().isEnabled();
        } else if (type == SOCIAL_SOURCE_TYPE.TYPE_MICROSOFT) {
            return config.getMicrosoftConfig().isEnabled();
        }
        return true;
    }
}
