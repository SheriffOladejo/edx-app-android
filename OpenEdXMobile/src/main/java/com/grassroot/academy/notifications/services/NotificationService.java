package com.grassroot.academy.notifications.services;

import com.braze.push.BrazeFirebaseMessagingService;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.grassroot.academy.base.MainApplication;
import com.grassroot.academy.core.IEdxEnvironment;
import com.grassroot.academy.deeplink.PushLinkManager;
import com.grassroot.academy.logger.Logger;

public class NotificationService extends FirebaseMessagingService {
    protected static final Logger logger = new Logger(NotificationService.class.getName());

    @Override
    public void onNewToken(String s) {
        logger.debug("Refreshed FCM token: " + s);
        super.onNewToken(s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        final IEdxEnvironment environment = MainApplication.getEnvironment(this);

        if (environment.getConfig().areFirebasePushNotificationsEnabled()) {
            if (BrazeFirebaseMessagingService.isBrazePushNotification(remoteMessage)) {
                BrazeFirebaseMessagingService.handleBrazeRemoteMessage(this, remoteMessage);
            } else {
                PushLinkManager.INSTANCE.onFCMForegroundNotificationReceived(remoteMessage);
            }
        }
    }
}
