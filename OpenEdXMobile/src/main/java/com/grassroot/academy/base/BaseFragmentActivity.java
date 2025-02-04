package com.grassroot.academy.base;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.grassroot.academy.R;
import com.grassroot.academy.core.IEdxEnvironment;
import com.grassroot.academy.deeplink.PushLinkManager;
import com.grassroot.academy.event.LogoutEvent;
import com.grassroot.academy.event.NetworkConnectivityChangeEvent;
import com.grassroot.academy.event.PushLinkReceivedEvent;
import com.grassroot.academy.interfaces.NetworkObserver;
import com.grassroot.academy.interfaces.NetworkSubject;
import com.grassroot.academy.interfaces.OnActivityResultListener;
import com.grassroot.academy.logger.Logger;
import com.grassroot.academy.util.NetworkUtil;
import com.grassroot.academy.util.ViewAnimationUtil;
import com.grassroot.academy.view.ICommonUI;
import com.grassroot.academy.view.dialog.AlertDialogFragment;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public abstract class BaseFragmentActivity extends BaseAppActivity
        implements NetworkSubject, ICommonUI, OnActivityResultListener {
    private final Handler handler = new Handler();
    protected final Logger logger = new Logger(getClass().getName());

    private boolean isConnectedToWifi = false;
    private boolean isActivityStarted = false;

    @Inject
    protected IEdxEnvironment environment;
    private List<NetworkObserver> networkObservers = new ArrayList<>();

    public void registerNetworkObserver(NetworkObserver observer) {
        if (observer != null && !networkObservers.contains(observer)) {
            networkObservers.add(observer);
        }
    }

    public void unregisterNetworkObserver(NetworkObserver observer) {
        if (observer != null && networkObservers.contains(observer)) {
            networkObservers.remove(observer);
        }
    }

    @Override
    public void notifyNetworkDisconnect() {
        for (NetworkObserver o : networkObservers) {
            o.onOffline();
        }
    }

    @Override
    public void notifyNetworkConnect() {
        for (NetworkObserver o : networkObservers) {
            o.onOnline();
        }
    }

    protected void setToolbarAsActionBar() {
        final View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null && toolbar instanceof Toolbar) {
            setSupportActionBar((Toolbar) toolbar);
            configureActionBar();
        }
    }

    /**
     * Hides the shadow that appears below {@link Toolbar}.
     */
    protected void hideToolbarShadow() {
        findViewById(R.id.appbar).setOutlineProvider(null);
    }

    protected void configureActionBar() {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowHomeEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setIcon(android.R.color.transparent);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!NetworkUtil.isConnected(this)) {
            // Currently we are sending this event again, so that offline SnackBar can appear
            // when we return to a screen.
            EventBus.getDefault().post(new NetworkConnectivityChangeEvent());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActivityStarted = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityStarted = false;
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            // App crashes on a few devices (mostly 4.0.+) on super method call
            // This is a workaround to avoid app crash, app still works even if Exception occurs
            super.onRestoreInstanceState(savedInstanceState);
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar buttons click
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setActionBarVisible(boolean visible) {
        try {
            ActionBar bar = getSupportActionBar();
            if (bar != null) {
                if (visible)
                    bar.show();
                else
                    bar.hide();
            }
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    public void animateLayouts(View view) {
        if (view == null) {
            logger.warn("Null view cannot be animated!");
            return;
        }
        ViewAnimationUtil.showMessageBar(view);
    }

    public void stopAnimation(View view) {
        if (view != null) {
            ViewAnimationUtil.stopAnimation(view);
        }
    }

    /**
     * Animate / show the download started message
     *
     * @param message - Message to display on the Download Panel
     * @return boolean - Returns true if message shown, false otherwise.
     */
    public boolean showInfoMessage(String message) {
        TextView infoMessageTv = (TextView) findViewById(R.id.flying_message);
        if (infoMessageTv != null) {
            infoMessageTv.setText(message);
            animateLayouts(infoMessageTv);
            return true;
        } else {
            logger.warn("TextView not available, so couldn't show flying message");
        }

        return false;
    }

    /**
     * Hides the info message view if its visible with animation
     *
     * @return <code>true<code/> if the view was hidden successfully otherwise <code>false</code>
     */
    public boolean hideInfoMessage() {
        View messageView = findViewById(R.id.flying_message);
        if (messageView == null) {
            logger.warn("Message view not available, so couldn't hide flying message");
            return false;
        }
        ViewAnimationUtil.hideMessageBar(messageView);
        return true;
    }

    public boolean isActivityStarted() {
        return isActivityStarted;
    }

    /**
     * Returns true if current orientation is LANDSCAPE, false otherwise.
     */
    protected boolean isLandscape() {
        return (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    /**
     * callback from EventBus
     *
     * @param event
     */
    @Subscribe(sticky = true)
    @SuppressWarnings("unused")
    public void onEvent(LogoutEvent event) {
        environment.getRouter().forceLogout(this,
                environment.getAnalyticsRegistry(),
                environment.getNotificationDelegate());
        finish();
    }

    /**
     * callback from EventBus
     *
     * @param event
     */
    @Subscribe(sticky = true)
    @SuppressWarnings("unused")
    public void onEvent(NetworkConnectivityChangeEvent event) {

        logger.debug("network state changed");
        if (NetworkUtil.isConnected(this)) {
            handler.post(new Runnable() {
                public void run() {
                    onOnline();
                    notifyNetworkConnect();
                }
            });

            if (NetworkUtil.isConnectedWifi(this)) {
                if (!isConnectedToWifi) {
                    isConnectedToWifi = true;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onConnectedToWifi();
                        }
                    });
                }
            } else if (NetworkUtil.isConnectedMobile(this)) {
                if (isConnectedToWifi) {
                    isConnectedToWifi = false;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onConnectedToMobile();
                        }
                    });
                }
            }
        } else {
            handler.post(new Runnable() {
                public void run() {
                    onOffline();
                    notifyNetworkDisconnect();
                }
            });

        }
    }

    @Subscribe
    public void onEvent(final PushLinkReceivedEvent event) {
        if (event.getPushLink().isDeepLink()) {
            showAlertDialog(event.getPushLink().getTitle(), event.getPushLink().getBody(),
                    getString(R.string.view_button_text),
                    // On View Click
                    (dialog, which) -> PushLinkManager.INSTANCE.onPushLinkActionGranted(this, event.getPushLink()),
                    getString(R.string.label_cancel), null
            );
        } else {
            showAlertDialog(event.getPushLink().getTitle(), event.getPushLink().getBody(),
                    getString(R.string.label_ok), null,
                    getString(R.string.label_cancel), null
            );
        }
    }

    public boolean showErrorMessage(String header, String message) {
        return showErrorMessage(header, message, true);
    }

    public boolean showErrorMessage(String header, String message, boolean isPersistent) {
        LinearLayout error_layout = (LinearLayout) findViewById(R.id.error_layout);
        if (error_layout == null) {
            logger.warn("Error Layout not available, so couldn't show flying message");
            return false;
        }
        TextView errorHeader = (TextView) findViewById(R.id.error_header);
        TextView errorMessageView = (TextView) findViewById(R.id.error_message);
        if (header == null || header.isEmpty()) {
            errorHeader.setVisibility(View.GONE);
        } else {
            errorHeader.setVisibility(View.VISIBLE);
            errorHeader.setText(header);
        }
        if (message != null) {
            errorMessageView.setText(message);
        }
        ViewAnimationUtil.showMessageBar(error_layout, isPersistent);
        return true;
    }

    /**
     * Sub-classes may override this method to handle connected state.
     */
    protected void onOnline() {
        logger.debug("You are now online");
    }

    /**
     * Sub-classes may override this method to handle disconnected state.
     */
    protected void onOffline() {
        logger.debug("You are now offline");
    }

    /**
     * Gets called whenever network state is changed and device is now connected to mobile data.
     * Sub-classes may override this method to handle when mobile data is connected.
     * This method is called after {@link #onOnline()} method.
     */
    protected void onConnectedToMobile() {
    }

    /**
     * Gets called whenever network state is changed and device is now connected to wifi.
     * Sub-classes may override this method to handle when wifi is connected.
     * This method is called after {@link #onOnline()} method.
     */
    protected void onConnectedToWifi() {
    }

    /**
     * Blocks touch event for this activity.
     * Use {@link #unblockTouch()} method to unblock and activate touch events.
     */
    protected void blockTouch() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    /**
     * Unblocks touch event for this activity.
     * This might should be called to unblock touch events that were blocked by {@link #blockTouch()} method.
     */
    protected void unblockTouch() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    protected void hideSoftKeypad() {
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public void showAlertDialog(@Nullable String title, @NonNull String message) {
        showAlertDialog(title, message, null);
    }

    public void showAlertDialog(@Nullable String title, @NonNull String message, @Nullable DialogInterface.OnClickListener onPositiveClick) {
        if (isInForeground()) {
            AlertDialogFragment.newInstance(title, message, onPositiveClick).show(getSupportFragmentManager(), null);
        }
    }

    public void showAlertDialog(@Nullable String title, @NonNull String message,
                                @NonNull String positiveButtonText,
                                @Nullable DialogInterface.OnClickListener onPositiveClick,
                                @Nullable String negativeButtonText,
                                @Nullable DialogInterface.OnClickListener onNegativeClick) {
        if (isInForeground()) {
            AlertDialogFragment.newInstance(title, message, positiveButtonText, onPositiveClick, negativeButtonText, onNegativeClick)
                    .show(getSupportFragmentManager(), null);
        }
    }

    @Override
    public boolean tryToSetUIInteraction(boolean enable) {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>To conform with the {@link OnActivityResultListener} interface this function has been
     * implemented emptily, making it publicly accessible.</p>
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
