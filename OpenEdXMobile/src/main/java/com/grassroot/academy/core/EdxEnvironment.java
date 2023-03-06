package com.grassroot.academy.core;

import com.grassroot.academy.module.analytics.AnalyticsRegistry;
import com.grassroot.academy.module.db.IDatabase;
import com.grassroot.academy.module.download.IDownloadManager;
import com.grassroot.academy.module.notification.NotificationDelegate;
import com.grassroot.academy.module.prefs.CourseCalendarPrefs;
import com.grassroot.academy.module.prefs.LoginPrefs;
import com.grassroot.academy.module.prefs.AppFeaturesPrefs;
import com.grassroot.academy.module.prefs.UserPrefs;
import com.grassroot.academy.module.storage.IStorage;
import com.grassroot.academy.util.Config;
import com.grassroot.academy.view.Router;
import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EdxEnvironment implements IEdxEnvironment {

    @Inject
    IDatabase database;

    @Inject
    IStorage storage;

    @Inject
    IDownloadManager downloadManager;

    @Inject
    UserPrefs userPrefs;

    @Inject
    LoginPrefs loginPrefs;

    @Inject
    CourseCalendarPrefs courseCalendarPrefs;

    @Inject
    AppFeaturesPrefs appFeaturesPrefs;

    @Inject
    AnalyticsRegistry analyticsRegistry;

    @Inject
    NotificationDelegate notificationDelegate;

    @Inject
    Router router;

    @Inject
    Config config;

    @Inject
    EventBus eventBus;

    @Inject
    public EdxEnvironment() {
    }

    @Override
    public IDatabase getDatabase() {
        return database;
    }

    @Override
    public IDownloadManager getDownloadManager() {
        return downloadManager;
    }

    @Override
    public UserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public LoginPrefs getLoginPrefs() {
        return loginPrefs;
    }

    public CourseCalendarPrefs getCourseCalendarPrefs() {
        return courseCalendarPrefs;
    }

    @Override
    public AppFeaturesPrefs getAppFeaturesPrefs() {
        return appFeaturesPrefs;
    }

    public AnalyticsRegistry getAnalyticsRegistry() {
        return analyticsRegistry;
    }

    @Override
    public NotificationDelegate getNotificationDelegate() {
        return notificationDelegate;
    }

    @Override
    public Router getRouter() {
        return router;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public IStorage getStorage() {
        return storage;
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
