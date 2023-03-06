package com.grassroot.academy.discussion;

import androidx.annotation.NonNull;

import com.grassroot.academy.model.discussion.DiscussionThread;

public class DiscussionThreadUpdatedEvent {

    @NonNull
    private final DiscussionThread discussionThread;

    public DiscussionThreadUpdatedEvent(@NonNull DiscussionThread discussionThread) {
        this.discussionThread = discussionThread;
    }

    @NonNull
    public DiscussionThread getDiscussionThread() {
        return discussionThread;
    }
}
