package com.grassroot.academy.test;

import com.grassroot.academy.test.CourseComponentTest;
import com.grassroot.academy.test.NotificationPreferenceTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({NotificationPreferenceTest.class, CourseComponentTest.class})
public class AllTest {
}
