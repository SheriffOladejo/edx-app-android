package com.grassroot.academy.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.telephony.TelephonyManager;

import androidx.annotation.Nullable;

import com.grassroot.academy.R;
import com.grassroot.academy.base.BaseFragmentActivity;
import com.grassroot.academy.module.prefs.PrefManager;

import java.util.List;

public class NetworkUtil {

    /**
     * Returns true if device is connected to wifi or mobile network, false
     * otherwise.
     *
     * @param context
     * @return
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo infoWifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (infoWifi != null) {
            State wifi = infoWifi.getState();
            if (wifi == NetworkInfo.State.CONNECTED) {
                return true;
            }
        }

        NetworkInfo infoMobile = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (infoMobile != null) {
            State mobile = infoMobile.getState();
            return mobile == State.CONNECTED;
        }
        return false;
    }

    /**
     * Check if there is any connectivity to a Wifi network
     *
     * @param context
     * @return
     */
    public static boolean isConnectedWifi(Context context) {
        NetworkInfo info = getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
    }

    /**
     * Check if there is any connectivity to a mobile network
     *
     * @param context
     * @return
     */
    public static boolean isConnectedMobile(Context context) {
        NetworkInfo info = getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE);
    }

    /**
     * Get the network info
     *
     * @param context
     * @return
     */
    @Nullable
    public static NetworkInfo getNetworkInfo(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo();
    }

    /**
     * Returns true if Zero-Rating is enabled and app is running on a carrier id mentioned in zero-rated configuration,
     * false otherwise.
     *
     * @param context
     * @return
     */
    public static boolean isOnZeroRatedNetwork(Context context, Config config) {
        if (config.getZeroRatingConfig().isEnabled()) {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String carrierId = manager.getNetworkOperator();

            List<String> zeroRatedCarriers = config.getZeroRatingConfig().getCarriers();

            for (String carrier : zeroRatedCarriers) {
                if (carrier.equalsIgnoreCase(carrierId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class ZeroRatedNetworkInfo {
        private final Context context;
        private final Config config;

        public ZeroRatedNetworkInfo(Context context, Config config) {
            this.context = context.getApplicationContext();
            this.config = config;
        }

        public boolean isOnZeroRatedNetwork() {
            return NetworkUtil.isOnZeroRatedNetwork(context, config);
        }
    }

    /**
     * Verify that there is an active network connection on which downloading is allowed. If
     * there is no such connection, then an appropriate message is displayed.
     *
     * @param activity Delegate of type {@link BaseFragmentActivity} to show proper error messages
     * @return If downloads can be performed, returns true; else returns false.
     */

    public static boolean verifyDownloadPossible(BaseFragmentActivity activity) {
        if (new PrefManager(activity, PrefManager.Pref.WIFI).getBoolean(PrefManager.Key
                .DOWNLOAD_ONLY_ON_WIFI, true)) {
            if (!isConnectedWifi(activity)) {
                activity.showInfoMessage(activity.getString(R.string.wifi_off_message));
                return false;
            }
        } else {
            if (!isConnected(activity)) {
                activity.showInfoMessage(activity.getString(R.string.network_not_connected));
                return false;
            }
        }
        return true;
    }
}
