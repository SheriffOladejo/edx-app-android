package com.grassroot.academy.discussion;

import androidx.annotation.NonNull;

import com.grassroot.academy.model.discussion.DiscussionThread;

public class DiscussionThreadPostedEvent {
    @NonNull
    private final DiscussionThread discussionThread;

    public DiscussionThreadPostedEvent(@NonNull DiscussionThread discussionThread) {
        this.discussionThread = discussionThread;
    }

    @NonNull
    public DiscussionThread getDiscussionThread() {
        return discussionThread;
    }
}
