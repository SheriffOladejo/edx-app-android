package com.grassroot.academy.view;

import androidx.fragment.app.Fragment;

import com.grassroot.academy.base.BaseSingleFragmentActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DiscussionAddCommentActivity extends BaseSingleFragmentActivity {

    @Inject
    DiscussionAddCommentFragment discussionAddCommentFragment;

    @Override
    public Fragment getFirstFragment() {
        discussionAddCommentFragment.setArguments(getIntent().getExtras());
        return discussionAddCommentFragment;
    }
}
