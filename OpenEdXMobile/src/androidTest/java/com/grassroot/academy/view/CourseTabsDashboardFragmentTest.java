package com.grassroot.academy.view;

import static org.assertj.android.api.Assertions.assertThat;
import static org.edx.mobile.http.util.CallUtil.executeStrict;
import static org.junit.Assert.assertNotNull;

import android.view.View;

import androidx.viewpager2.widget.ViewPager2;

import org.edx.mobile.R;
import org.edx.mobile.base.UiTest;
import org.edx.mobile.deeplink.Screen;
import org.edx.mobile.model.api.EnrolledCoursesResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.support.v4.SupportFragmentController;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;

@HiltAndroidTest
@Config(application = HiltTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class CourseTabsDashboardFragmentTest extends UiTest {
    @Rule()
    public HiltAndroidRule hiltAndroidRule = new HiltAndroidRule(this);

    @Before
    public void init() {
        hiltAndroidRule.inject();
    }

    protected EnrolledCoursesResponse getCourseData() {
        EnrolledCoursesResponse courseData;
        try {
            courseData = executeStrict(courseAPI.getEnrolledCourses()).get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return courseData;
    }

    /**
     * Testing initialization
     */
    @Test
    public void initializeTest() {
        CourseTabsDashboardFragment fragment = CourseTabsDashboardFragment.newInstance(getCourseData(), "testsCourseId", Screen.COURSE_DASHBOARD);
        SupportFragmentController.setupFragment(fragment, HiltTestActivity.class,
                android.R.id.content, null);
        View view = fragment.getView();
        assertNotNull(view);

        View viewPager2 = view.findViewById(R.id.viewPager2);
        assertNotNull(viewPager2);
        Assertions.assertThat(viewPager2).isInstanceOf(ViewPager2.class);
    }

// TODO: Following commented test cases will be revisited in LEARNER-5277 story.
//    /**
//     * Testing options menu initialization
//     */
//    @Test
//    public void initializeOptionsMenuTest() {
//        BaseVideosDownloadStateActivity activity =
//                Robolectric.buildActivity(getActivityClass())
//                        .withIntent(getIntent()).setup().get();
//        Menu menu = Shadows.shadowOf(activity).getOptionsMenu();
//        assertNotNull(menu);
//        // Can't see any method to confirm action layout source as well
//        MenuItem progressItem = menu.findItem(R.id.download_progress);
//        assertNotNull(progressItem);
//        assertThat(progressItem).hasTitle(activity.getText(R.string.action_settings));
//    }
//
//    /**
//     * Testing download progress menu visibility states and click behaviour
//     * (starting DownloadActivity). Only when both AppConstants.offline_flag
//     * is true and there is a downloading entry in the database, should the
//     * progress bar be visible.
//     */
//    @Test
//    public void downloadProgressViewTest() {
//        connectToNetwork();
//        assertFalse(Shadows.shadowOf(Robolectric.buildActivity(getActivityClass())
//                .withIntent(getIntent()).setup().get())
//                .getOptionsMenu()
//                .findItem(R.id.download_progress)
//                .isVisible());
//
//        disconnectFromNetwork();
//        assertFalse(Shadows.shadowOf(Robolectric.buildActivity(getActivityClass())
//                .withIntent(getIntent()).setup().get())
//                .getOptionsMenu()
//                .findItem(R.id.download_progress)
//                .isVisible());
//
//        IDatabase db = environment.getDatabase();
//        DownloadEntry de = new DownloadEntry();
//        de.username = "unittest";
//        de.title = "title";
//        de.videoId = "videoId-" + System.currentTimeMillis();
//        de.size = 1024;
//        de.duration = 3600;
//        de.filepath = "/fakepath";
//        de.url = "http://fake/url";
//        de.eid = "fake_eid";
//        de.chapter = "fake_chapter";
//        de.section = "fake_section";
//        de.lastPlayedOffset = 0;
//        de.lmsUrl = "http://fake/lms/url";
//        de.isCourseActive = 1;
//        de.downloaded = DownloadEntry.DownloadedState.DOWNLOADING;
//        Long rowId = db.addVideoData(de, null);
//        assertNotNull(rowId);
//        Java6Assertions.assertThat(rowId).isGreaterThan(0);
//        assertFalse(Shadows.shadowOf(Robolectric.buildActivity(getActivityClass())
//                .withIntent(getIntent()).setup().get())
//                .getOptionsMenu()
//                .findItem(R.id.download_progress)
//                .isVisible());
//
//        connectToNetwork();
//        BaseVideosDownloadStateActivity activity =
//                Robolectric.buildActivity(getActivityClass())
//                        .withIntent(getIntent()).setup().get();
//        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
//        MenuItem downloadProgressMenuItem = shadowActivity
//                .getOptionsMenu()
//                .findItem(R.id.download_progress);
//        assertTrue(downloadProgressMenuItem.isVisible());
//        assertTrue(downloadProgressMenuItem.getActionView().performClick());
//        assertNextStartedActivity(activity, DownloadListActivity.class);
//    }
}
