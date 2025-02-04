package com.grassroot.academy.view;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.ComponentActivity;

import com.grassroot.academy.base.MainApplication;
import com.grassroot.academy.core.IEdxEnvironment;
import com.grassroot.academy.deeplink.BranchLinkManager;
import com.grassroot.academy.deeplink.PushLinkManager;
import com.grassroot.academy.logger.Logger;
import com.grassroot.academy.util.Config;
import com.grassroot.academy.util.NetworkUtil;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.branch.referral.Branch;

@AndroidEntryPoint
public class SplashActivity extends ComponentActivity {
    protected final Logger logger = new Logger(getClass().getName());

    @Inject
    Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        Recommended solution to avoid opening of multiple tasks of our app's launcher activity.
        For more info:
        - https://issuetracker.google.com/issues/36907463
        - https://stackoverflow.com/questions/4341600/how-to-prevent-multiple-instances-of-an-activity-when-it-is-launched-with-differ/
        - https://stackoverflow.com/questions/16283079/re-launch-of-activity-on-home-button-but-only-the-first-time/16447508#16447508
         */
        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(intent.getAction())) {
                return;
            }
        }

        final IEdxEnvironment environment = MainApplication.getEnvironment(this);
        if (environment.getLoginPrefs().isUserLoggedIn()) {
            environment.getRouter().showMainDashboard(SplashActivity.this);
        } else if (!environment.getConfig().isRegistrationEnabled()) {
            startActivity(environment.getRouter().getLogInIntent());
        } else {
            environment.getRouter().showLaunchScreen(SplashActivity.this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Checking push notification case in onStart() to make sure it will call in all cases
        // when this launcher activity will be started. For more details study onCreate() function.
        PushLinkManager.INSTANCE.checkAndReactIfFCMNotificationReceived(this, getIntent().getExtras());

        if (config.getBranchConfig().isEnabled()) {
            // Initialize Branch
            // ref: https://help.branch.io/developers-hub/docs/android-basic-integration#initialize-branch
            Branch.BranchReferralInitListener branchReferralInitListener = (linkProperties, error) -> {
                // do stuff with deep link data (nav to page, display content, etc)
                if (error == null) {
                    // params are the deep linked params associated with the link that the user
                    // clicked -> was re-directed to this app params will be empty if no data found
                    if (linkProperties.optBoolean(BranchLinkManager.KEY_CLICKED_BRANCH_LINK)) {
                        try {
                            BranchLinkManager.INSTANCE.checkAndReactIfReceivedLink(this, linkProperties);
                        } catch (Exception e) {
                            logger.error(e, true);
                        }
                    }
                } else {
                    // Ignore the logging of errors occurred due to lack of network connectivity
                    if (NetworkUtil.isConnected(getApplicationContext())) {
                        logger.error(new Exception("Branch not configured properly, error:\n"
                                + error.getMessage()), true);
                    }
                }
            };
            Branch.sessionBuilder(this).withCallback(branchReferralInitListener)
                    .withDelay(1000) // wait for 1 sec to complete the Initialization with payload.
                    .withData(getIntent() != null ? getIntent().getData() : null).init();
        }
        finish();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
    }
}
