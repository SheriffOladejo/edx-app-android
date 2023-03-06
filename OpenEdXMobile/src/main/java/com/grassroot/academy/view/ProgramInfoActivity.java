package com.grassroot.academy.view;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.grassroot.academy.base.BaseSingleFragmentActivity;
import com.grassroot.academy.base.WebViewProgramInfoFragment;
import com.grassroot.academy.module.analytics.Analytics;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProgramInfoActivity extends BaseSingleFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        environment.getAnalyticsRegistry().trackScreenView(Analytics.Screens.PROGRAM_INFO_SCREEN);
    }

    @Override
    public Fragment getFirstFragment() {
        final WebViewProgramInfoFragment fragment = new WebViewProgramInfoFragment();
        fragment.setArguments(getIntent().getExtras());
        return fragment;
    }
}
