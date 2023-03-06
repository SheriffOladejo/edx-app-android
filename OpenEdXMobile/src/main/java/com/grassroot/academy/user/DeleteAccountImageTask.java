package com.grassroot.academy.user;

import android.content.Context;

import androidx.annotation.NonNull;

import com.grassroot.academy.core.EdxDefaultModule;
import com.grassroot.academy.event.ProfilePhotoUpdatedEvent;
import com.grassroot.academy.module.prefs.LoginPrefs;
import com.grassroot.academy.task.Task;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import dagger.hilt.android.EntryPointAccessors;

public class DeleteAccountImageTask extends Task<Void> {

    UserService userService;
    LoginPrefs loginPrefs;

    @NonNull
    private final String username;

    public DeleteAccountImageTask(@NonNull Context context, @NonNull String username) {
        super(context);
        this.username = username;
        EdxDefaultModule.ProviderEntryPoint provider = EntryPointAccessors.fromApplication(
                context, EdxDefaultModule.ProviderEntryPoint.class);
        userService = provider.getUserService();
        loginPrefs = provider.getLoginPrefs();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            userService.deleteProfileImage(username).execute();
        } catch (IOException e) {
            logger.error(e);
            handleException(e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
        super.onPostExecute(unused);
        EventBus.getDefault().post(new ProfilePhotoUpdatedEvent(username, null));
        // Delete the logged in user's ProfileImage
        loginPrefs.setProfileImage(username, null);
    }

    @Override
    public void onException(Exception ex) {
        // nothing to do
    }
}
