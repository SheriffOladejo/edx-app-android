package com.grassroot.academy.view;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.grassroot.academy.R;
import com.grassroot.academy.base.BaseSingleFragmentActivity;
import com.grassroot.academy.base.WebViewCourseInfoFragment;
import com.grassroot.academy.module.analytics.Analytics;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CourseInfoActivity extends BaseSingleFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        environment.getAnalyticsRegistry().trackScreenView(Analytics.Screens.COURSE_INFO_SCREEN);
    }

    @Override
    public void onResume() {
        super.onResume();
        AuthPanelUtils.configureAuthPanel(findViewById(R.id.auth_panel), environment);
    }

    @Override
    public Fragment getFirstFragment() {
        final WebViewCourseInfoFragment fragment = new WebViewCourseInfoFragment();
        fragment.setArguments(getIntent().getExtras());
        return fragment;
    }
}
