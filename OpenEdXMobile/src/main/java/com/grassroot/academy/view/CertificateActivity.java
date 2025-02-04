package com.grassroot.academy.view;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.grassroot.academy.base.BaseSingleFragmentActivity;
import com.grassroot.academy.model.api.EnrolledCoursesResponse;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CertificateActivity extends BaseSingleFragmentActivity {

    public static Intent newIntent(@NonNull Context context, @NonNull EnrolledCoursesResponse courseData) {
        return new Intent(context, CertificateActivity.class)
                .putExtra(CertificateFragment.ENROLLMENT, courseData);
    }

    @Override
    public Fragment getFirstFragment() {
        final CertificateFragment certificateFragment = new CertificateFragment();
        certificateFragment.setArguments(getIntent().getExtras());
        return certificateFragment;
    }
}
