package com.grassroot.academy.core;


import com.grassroot.academy.module.analytics.AnalyticsRegistry;
import com.grassroot.academy.module.db.IDatabase;
import com.grassroot.academy.module.download.IDownloadManager;
import com.grassroot.academy.module.notification.NotificationDelegate;
import com.grassroot.academy.module.prefs.CourseCalendarPrefs;
import com.grassroot.academy.module.prefs.AppFeaturesPrefs;
import com.grassroot.academy.module.prefs.LoginPrefs;
import com.grassroot.academy.module.prefs.UserPrefs;
import com.grassroot.academy.module.storage.IStorage;
import com.grassroot.academy.util.Config;
import com.grassroot.academy.view.Router;

/**
 * TODO - we should decompose this class into environment setting and service provider settings.
 */
public interface IEdxEnvironment {

    IDatabase getDatabase();

    IStorage getStorage();

    IDownloadManager getDownloadManager();

    UserPrefs getUserPrefs();

    LoginPrefs getLoginPrefs();

    CourseCalendarPrefs getCourseCalendarPrefs();

    AppFeaturesPrefs getAppFeaturesPrefs();

    AnalyticsRegistry getAnalyticsRegistry();

    NotificationDelegate getNotificationDelegate();

    Router getRouter();

    Config getConfig();
}
