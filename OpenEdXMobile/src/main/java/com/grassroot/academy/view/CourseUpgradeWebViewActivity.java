package com.grassroot.academy.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.grassroot.academy.base.BaseSingleFragmentActivity;
import com.grassroot.academy.model.api.EnrolledCoursesResponse;
import com.grassroot.academy.model.course.CourseComponent;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CourseUpgradeWebViewActivity extends BaseSingleFragmentActivity {
    private static final String ARG_URL = "url";
    private static final String ARG_TITLE = "title";

    public static Intent newIntent(@NonNull Context context, @NonNull String url,
                                   @NonNull String title, @NonNull EnrolledCoursesResponse courseData,
                                   @Nullable CourseComponent unit) {
        return new Intent(context, CourseUpgradeWebViewActivity.class)
                .putExtra(ARG_URL, url)
                .putExtra(ARG_TITLE, title)
                .putExtra(Router.EXTRA_COURSE_UNIT, unit)
                .putExtra(Router.EXTRA_COURSE_DATA, courseData);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String title = getIntent().getStringExtra(ARG_TITLE);
        if (!TextUtils.isEmpty(title)) {
            setTitle(title);
        }
    }

    @Override
    public Fragment getFirstFragment() {
        final EnrolledCoursesResponse courseData = (EnrolledCoursesResponse) getIntent().getSerializableExtra(Router.EXTRA_COURSE_DATA);
        final CourseComponent courseUnit = (CourseComponent) getIntent().getSerializableExtra(Router.EXTRA_COURSE_UNIT);
        return CourseUpgradeWebViewFragment.newInstance(getIntent().getStringExtra(ARG_URL),
                null, true, courseData, courseUnit);
    }
}
