package com.grassroot.academy.view;

import static com.grassroot.academy.view.Router.EXTRA_DEEP_LINK;
import static com.grassroot.academy.view.Router.EXTRA_PATH_ID;
import static com.grassroot.academy.view.Router.EXTRA_SCREEN_NAME;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import com.grassroot.academy.BuildConfig;
import com.grassroot.academy.R;
import com.grassroot.academy.deeplink.DeepLink;
import com.grassroot.academy.deeplink.ScreenDef;
import com.grassroot.academy.event.MainDashboardRefreshEvent;
import com.grassroot.academy.event.NewVersionAvailableEvent;
import com.grassroot.academy.module.notification.NotificationDelegate;
import com.grassroot.academy.module.prefs.PrefManager;
import com.grassroot.academy.util.AppConstants;
import com.grassroot.academy.util.AppStoreUtils;
import com.grassroot.academy.util.IntentFactory;
import com.grassroot.academy.util.Version;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.ParseException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainDashboardActivity extends OfflineSupportBaseActivity {

    @Inject
    NotificationDelegate notificationDelegate;

    public static Intent newIntent(@Nullable @ScreenDef String screenName, @Nullable String pathId) {
        // These flags will make it so we only have a single instance of this activity,
        // but that instance will not be restarted if it is already running
        return IntentFactory.newIntentForComponent(MainDashboardActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_SCREEN_NAME, screenName)
                .putExtra(EXTRA_PATH_ID, pathId);
    }

    public static Intent newIntent(@Nullable DeepLink deepLink) {
        return IntentFactory.newIntentForComponent(MainDashboardActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_DEEP_LINK, deepLink);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initWhatsNew();
    }

    @Override
    public Object getRefreshEvent() {
        return new MainDashboardRefreshEvent();
    }

    private void initWhatsNew() {
        if (environment.getConfig().isWhatsNewEnabled()) {
            boolean shouldShowWhatsNew = false;
            final PrefManager.AppInfoPrefManager appPrefs = new PrefManager.AppInfoPrefManager(this);
            final String lastWhatsNewShownVersion = appPrefs.getWhatsNewShownVersion();
            if (lastWhatsNewShownVersion == null) {
                shouldShowWhatsNew = true;
            } else {
                try {
                    final Version oldVersion = new Version(lastWhatsNewShownVersion);
                    final Version newVersion = new Version(BuildConfig.VERSION_NAME);
                    if (oldVersion.isNMinorVersionsDiff(newVersion,
                            AppConstants.MINOR_VERSIONS_DIFF_REQUIRED_FOR_WHATS_NEW)) {
                        shouldShowWhatsNew = true;
                    }
                } catch (ParseException e) {
                    shouldShowWhatsNew = false;
                    logger.error(e);
                }
            }
            if (shouldShowWhatsNew) {
                environment.getRouter().showWhatsNewActivity(this);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            /* This is the main Activity, and is where the new version availability
             * notifications are being posted. These events are posted as sticky so
             * that they can be compared against new instances of them to be posted
             * in order to determine whether it has new information content. The
             * events have an intrinsic property to mark them as consumed, in order
             * to not have to remove the sticky events (and thus lose the last
             * posted event information). Finishing this Activity should be
             * considered as closing the current session, and the notifications
             * should be reposted on a new session. Therefore, we clear the session
             * information by removing the sticky new version availability events
             * from the event bus.
             */
            EventBus.getDefault().removeStickyEvent(NewVersionAvailableEvent.class);
        }
    }

    @Override
    public Fragment getFirstFragment() {
        final Fragment fragment = new MainTabsDashboardFragment();
        final Bundle bundle = getIntent().getExtras();
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    protected void onResume() {
        super.onResume();
        notificationDelegate.checkAppUpgrade();
    }

    /**
     * Event bus callback for new app version availability event.
     *
     * @param newVersionAvailableEvent The new app version availability event.
     */
    @Subscribe
    public void onEvent(@NonNull final NewVersionAvailableEvent newVersionAvailableEvent) {
        if (!newVersionAvailableEvent.isConsumed()) {
            final Snackbar snackbar = Snackbar.make(getBinding().coordinatorLayout,
                    newVersionAvailableEvent.getNotificationString(this),
                    Snackbar.LENGTH_INDEFINITE);
            if (AppStoreUtils.canUpdate(this)) {
                snackbar.setAction(R.string.label_update,
                        AppStoreUtils.OPEN_APP_IN_APP_STORE_CLICK_LISTENER);
            }
            snackbar.setCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    newVersionAvailableEvent.markAsConsumed();
                }
            });
            snackbar.show();
        }
    }

    @Override
    protected void configureActionBar() {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowHomeEnabled(false);
            bar.setDisplayHomeAsUpEnabled(false);
            bar.setIcon(android.R.color.transparent);
        }
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getResources().getString(titleId));
    }
}
