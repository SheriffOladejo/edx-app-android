package com.grassroot.academy.test.feature;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.grassroot.academy.base.MainApplication;
import com.grassroot.academy.core.EdxEnvironment;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public abstract class FeatureTest {
    protected EdxEnvironment environment;

    @Before
    public void setup() {
        // Ensure we are not logged in
        final MainApplication application = MainApplication.instance();
        environment = application.getInjector().getInstance(EdxEnvironment.class);
        environment.getLoginPrefs().clear();
        environment.getAnalyticsRegistry().resetIdentifyUser();
    }
}
