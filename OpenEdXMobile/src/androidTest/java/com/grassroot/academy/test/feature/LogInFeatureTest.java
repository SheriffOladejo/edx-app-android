package com.grassroot.academy.test.feature;

import com.grassroot.academy.test.feature.data.TestValues;
import com.grassroot.academy.test.feature.interactor.AppInteractor;

import org.junit.Test;

public class LogInFeatureTest extends FeatureTest {

    @Test
    public void afterEmailLogIn_withActiveAccount_myCoursesScreenIsDisplayed() {
        new AppInteractor()
                .launchApp()
                .observeLandingScreen()
                .navigateToLogInScreen()
                .logIn(TestValues.ACTIVE_USER_CREDENTIALS)
                .observeMyCoursesScreen();
    }
}
