package com.grassroot.academy.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.grassroot.academy.R;
import com.grassroot.academy.base.BaseSingleFragmentActivity;
import com.grassroot.academy.module.analytics.Analytics;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditUserProfileActivity extends BaseSingleFragmentActivity {
    public static final String EXTRA_USERNAME = "username";

    public static Intent newIntent(@NonNull Context context, @NonNull String username) {
        return new Intent(context, EditUserProfileActivity.class)
                .putExtra(EXTRA_USERNAME, username);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.edit_user_profile_title));
        environment.getAnalyticsRegistry().trackScreenView(Analytics.Screens.PROFILE_EDIT);
    }

    @Override
    public Fragment getFirstFragment() {
        final Fragment fragment = new EditUserProfileFragment();
        fragment.setArguments(getIntent().getExtras());
        return fragment;
    }
}
