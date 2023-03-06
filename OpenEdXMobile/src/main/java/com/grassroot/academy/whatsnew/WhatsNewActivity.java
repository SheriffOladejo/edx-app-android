package com.grassroot.academy.whatsnew;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.grassroot.academy.BuildConfig;
import com.grassroot.academy.R;
import com.grassroot.academy.base.BaseFragmentActivity;
import com.grassroot.academy.base.MainApplication;
import com.grassroot.academy.module.prefs.PrefManager;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WhatsNewActivity extends BaseFragmentActivity {

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, WhatsNewActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whats_new);

        Fragment singleFragment = new WhatsNewFragment();

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fragment_container, singleFragment, null);
        fragmentTransaction.disallowAddToBackStack();
        fragmentTransaction.commit();

        final PrefManager.AppInfoPrefManager appPrefs = new PrefManager.AppInfoPrefManager(MainApplication.application);
        appPrefs.setWhatsNewShown(BuildConfig.VERSION_NAME);
    }
}
