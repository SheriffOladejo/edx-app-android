package com.grassroot.academy.user;

import android.content.Context;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.grassroot.academy.core.EdxDefaultModule;
import com.grassroot.academy.event.AccountDataLoadedEvent;
import com.grassroot.academy.http.callback.CallTrigger;
import com.grassroot.academy.http.callback.ErrorHandlingCallback;
import com.grassroot.academy.http.notifications.ErrorNotification;
import com.grassroot.academy.model.user.Account;
import com.grassroot.academy.module.prefs.LoginPrefs;
import com.grassroot.academy.view.common.TaskMessageCallback;
import com.grassroot.academy.view.common.TaskProgressCallback;
import org.greenrobot.eventbus.EventBus;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.EntryPointAccessors;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;

@Singleton
public class UserAPI {

    UserService userService;

    @Inject
    public UserAPI(@NonNull UserService userService) {
        this.userService = userService;
    }

    @Singleton
    public static class AccountDataUpdatedCallback extends ErrorHandlingCallback<Account> {

        @NonNull
        private final String username;

        public AccountDataUpdatedCallback(@NonNull final Context context,
                                          @NonNull final String username,
                                          @Nullable final ErrorNotification errorNotification) {
            this(context, username, null, errorNotification);
        }

        public AccountDataUpdatedCallback(@NonNull final Context context,
                                          @NonNull final String username,
                                          @Nullable final TaskProgressCallback progressCallback,
                                          @Nullable final ErrorNotification errorNotification) {
            super(context, progressCallback, errorNotification);
            this.username = username;
        }

        //TODO: Remove this legacy code starting from here, when modern error design has been implemented on all screens i.e. SnackBar, FullScreen and Dialog based errors.
        public AccountDataUpdatedCallback(@NonNull final Context context,
                                          @NonNull final String username,
                                          @Nullable final TaskProgressCallback progressCallback,
                                          @Nullable TaskMessageCallback messageCallback,
                                          @Nullable CallTrigger callTrigger) {
            super(context, progressCallback, messageCallback, callTrigger);
            this.username = username;
        }
        // LEGACY CODE ENDS HERE, all occurrences of this constructor should also be updated in future

        @Override
        protected void onResponse(@NonNull final Account account) {

            LoginPrefs loginPrefs = EntryPointAccessors.fromApplication(context,
                    EdxDefaultModule.ProviderEntryPoint.class).getLoginPrefs();
            // Store the logged in user Info for Profile Screen
            if (account.getEmail() != null) {
                loginPrefs.setUserInfo(username, account.getEmail(), account.getProfileImage(),
                        !account.requiresParentalConsent() && account.getAccountPrivacy() == Account.Privacy.PRIVATE);
            }
            EventBus.getDefault().post(new AccountDataLoadedEvent(account));
        }
    }

    public Call<ResponseBody> setProfileImage(@NonNull String username, @NonNull final File file) {
        final String mimeType = "image/jpeg";
        return userService.setProfileImage(
                username,
                "attachment;filename=filename." + MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType),
                RequestBody.create(MediaType.parse(mimeType), file));
    }
}
