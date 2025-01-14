package com.grassroot.academy.event;

public class MediaStatusChangeEvent extends BaseEvent {

    private final boolean isSdCardAvailable;

    public MediaStatusChangeEvent(boolean isSdCardAvailable) {
        this.isSdCardAvailable = isSdCardAvailable;
    }

    public boolean isSdCardAvailable() {
        return isSdCardAvailable;
    }
}
