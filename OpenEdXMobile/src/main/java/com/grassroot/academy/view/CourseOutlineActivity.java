package com.grassroot.academy.view;

import static com.grassroot.academy.view.Router.EXTRA_BUNDLE;
import static com.grassroot.academy.view.Router.EXTRA_COURSE_COMPONENT_ID;
import static com.grassroot.academy.view.Router.EXTRA_COURSE_DATA;
import static com.grassroot.academy.view.Router.EXTRA_COURSE_UPGRADE_DATA;
import static com.grassroot.academy.view.Router.EXTRA_IS_VIDEOS_MODE;
import static com.grassroot.academy.view.Router.EXTRA_LAST_ACCESSED_ID;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.grassroot.academy.base.BaseSingleFragmentActivity;
import com.grassroot.academy.event.CourseUpgradedEvent;
import com.grassroot.academy.model.api.CourseUpgradeResponse;
import com.grassroot.academy.model.api.EnrolledCoursesResponse;
import com.grassroot.academy.module.analytics.Analytics;
import org.greenrobot.eventbus.Subscribe;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CourseOutlineActivity extends BaseSingleFragmentActivity {

    private Bundle courseBundle;
    private String courseComponentId = null;
    private boolean isVideoMode = false;

    public static Intent newIntent(Activity activity,
                                   EnrolledCoursesResponse courseData,
                                   CourseUpgradeResponse courseUpgradeData,
                                   String courseComponentId, String lastAccessedId,
                                   boolean isVideosMode) {
        final Bundle courseBundle = new Bundle();
        courseBundle.putSerializable(EXTRA_COURSE_DATA, courseData);
        courseBundle.putParcelable(EXTRA_COURSE_UPGRADE_DATA, courseUpgradeData);
        courseBundle.putString(EXTRA_COURSE_COMPONENT_ID, courseComponentId);

        final Intent intent = new Intent(activity, CourseOutlineActivity.class);
        intent.putExtra(EXTRA_BUNDLE, courseBundle);
        intent.putExtra(EXTRA_LAST_ACCESSED_ID, lastAccessedId);
        intent.putExtra(EXTRA_IS_VIDEOS_MODE, isVideosMode);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseExtras();

        if (courseComponentId == null) {
            EnrolledCoursesResponse courseData = (EnrolledCoursesResponse) courseBundle.getSerializable(EXTRA_COURSE_DATA);
            environment.getAnalyticsRegistry().trackScreenView(
                    isVideoMode ? Analytics.Screens.VIDEOS_COURSE_VIDEOS : Analytics.Screens.COURSE_OUTLINE,
                    courseData.getCourse().getId(), null);

            setTitle(courseData.getCourse().getName());
        }
    }

    private void parseExtras() {
        courseBundle = getIntent().getBundleExtra(EXTRA_BUNDLE);
        courseComponentId = getIntent().getStringExtra(EXTRA_COURSE_COMPONENT_ID);
        isVideoMode = getIntent().getBooleanExtra(EXTRA_IS_VIDEOS_MODE, false);
    }

    @Override
    public Fragment getFirstFragment() {
        final Fragment fragment = new CourseOutlineFragment();
        fragment.setArguments(getIntent().getExtras());
        return fragment;
    }

    @Subscribe
    public void onEvent(CourseUpgradedEvent event) {
        finish();
    }
}
