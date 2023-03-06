package com.grassroot.academy.view;

import androidx.fragment.app.Fragment;

import com.grassroot.academy.base.BaseSingleFragmentActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DiscussionAddResponseActivity extends BaseSingleFragmentActivity {

    @Inject
    DiscussionAddResponseFragment discussionAddResponseFragment;

    @Override
    public Fragment getFirstFragment() {
        discussionAddResponseFragment.setArguments(getIntent().getExtras());
        return discussionAddResponseFragment;
    }
}
