package com.grassroot.academy.profiles;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.grassroot.academy.event.AccountDataLoadedEvent;
import com.grassroot.academy.event.ProfilePhotoUpdatedEvent;
import com.grassroot.academy.http.callback.Callback;
import com.grassroot.academy.interfaces.RefreshListener;
import com.grassroot.academy.logger.Logger;
import com.grassroot.academy.model.api.ProfileModel;
import com.grassroot.academy.model.profile.UserProfileBioModel;
import com.grassroot.academy.model.profile.UserProfileViewModel;
import com.grassroot.academy.model.user.Account;
import com.grassroot.academy.module.prefs.UserPrefs;
import com.grassroot.academy.user.UserService;
import com.grassroot.academy.util.InvalidLocaleException;
import com.grassroot.academy.util.LocaleUtils;
import com.grassroot.academy.util.observer.CachingObservable;
import com.grassroot.academy.util.observer.Observable;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Exposes a given user's profile data and photo as a pair of observable view models
 */
public class UserProfileInteractor implements RefreshListener {
    @NonNull
    private final String username;

    @NonNull
    private final EventBus eventBus;

    @NonNull
    private final UserService userService;

    private final boolean viewingOwnProfile;

    @NonNull
    private final CachingObservable<UserProfileViewModel> profileObservable = new CachingObservable<>();

    @NonNull
    private final CachingObservable<UserProfileImageViewModel> photo = new CachingObservable<>();

    @NonNull
    private final Logger logger = new Logger(getClass().getName());

    public UserProfileInteractor(@NonNull final String username, @NonNull final UserService userService, @NonNull EventBus eventBus, @NonNull UserPrefs userPrefs) {
        this.username = username;
        this.eventBus = eventBus;
        this.userService = userService;

        final ProfileModel model = userPrefs.getProfile();
        viewingOwnProfile = model.username.equalsIgnoreCase(username);

        eventBus.register(this);
        onRefresh();
    }

    @NonNull
    public Observable<UserProfileViewModel> observeProfile() {
        return profileObservable;
    }

    @NonNull
    public Observable<UserProfileImageViewModel> observeProfileImage() {
        return photo;
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onEventMainThread(@NonNull AccountDataLoadedEvent event) {
        if (!event.getAccount().getUsername().equalsIgnoreCase(username)) {
            return;
        }
        handleNewAccount(event.getAccount());
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onEventMainThread(@NonNull ProfilePhotoUpdatedEvent event) {
        if (!event.getUsername().equalsIgnoreCase(username)) {
            return;
        }
        photo.onData(new UserProfileImageViewModel(event.getUri(), false));
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public boolean isViewingOwnProfile() {
        return viewingOwnProfile;
    }

    public void destroy() {
        eventBus.unregister(this);
    }

    private void handleNewAccount(@NonNull Account accountResponse) {
        {
            final UserProfileViewModel.LimitedProfileMessage limitedProfileMessage;
            String languageName = null;
            String countryName = null;
            // If the user doesn't have YOB or is less than 13
            if (accountResponse.requiresParentalConsent()) {
                limitedProfileMessage = UserProfileViewModel.LimitedProfileMessage.NONE;
            } else if (accountResponse.getAccountPrivacy() == Account.Privacy.PRIVATE) {
                limitedProfileMessage = viewingOwnProfile
                        ? UserProfileViewModel.LimitedProfileMessage.OWN_PROFILE
                        : UserProfileViewModel.LimitedProfileMessage.NONE;
            } else {
                limitedProfileMessage = UserProfileViewModel.LimitedProfileMessage.NONE;
                if (!accountResponse.getLanguageProficiencies().isEmpty()) {
                    try {
                        languageName = LocaleUtils.getLanguageNameFromCode(accountResponse.getLanguageProficiencies().get(0).getCode());
                    } catch (InvalidLocaleException e) {
                        logger.error(e, true);
                    }
                }

                if (!TextUtils.isEmpty(accountResponse.getCountry())) {
                    try {
                        countryName = LocaleUtils.getCountryNameFromCode(accountResponse.getCountry());
                    } catch (InvalidLocaleException e) {
                        logger.error(e, true);
                    }
                }
            }

            final UserProfileBioModel.ContentType contentType;
            if (viewingOwnProfile && accountResponse.requiresParentalConsent()) {
                contentType = UserProfileBioModel.ContentType.PARENTAL_CONSENT_REQUIRED;

            } else if (viewingOwnProfile && TextUtils.isEmpty(accountResponse.getBio()) && accountResponse.getAccountPrivacy() != Account.Privacy.ALL_USERS) {
                contentType = UserProfileBioModel.ContentType.INCOMPLETE;

            } else if (accountResponse.getAccountPrivacy() != Account.Privacy.PRIVATE) {
                if (TextUtils.isEmpty(accountResponse.getBio())) {
                    contentType = UserProfileBioModel.ContentType.NO_ABOUT_ME;
                } else {
                    contentType = UserProfileBioModel.ContentType.ABOUT_ME;
                }
            } else {
                contentType = UserProfileBioModel.ContentType.EMPTY;
            }
            UserProfileBioModel bioModel = new UserProfileBioModel(contentType, accountResponse.getBio());
            profileObservable.onData(new UserProfileViewModel(limitedProfileMessage, languageName, countryName, bioModel));
        }
        photo.onData(new UserProfileImageViewModel(accountResponse.getProfileImage().hasImage() ? Uri.parse(accountResponse.getProfileImage().getImageUrlFull()) : null, true));
    }

    @Override
    public void onRefresh() {
        userService.getAccount(username).enqueue(new Callback<Account>() {
            @Override
            protected void onResponse(@NonNull Account account) {
                eventBus.post(new AccountDataLoadedEvent(account));
            }

            @Override
            protected void onFailure(@NonNull Throwable error) {
                profileObservable.onError(error);
            }
        });
    }
}
