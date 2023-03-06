package com.grassroot.academy.player;

import java.io.Serializable;

public interface IPlayerEventCallback extends Serializable {

    void onError();
    void onPlaybackStarted();
    void onPlaybackComplete();
    void saveCurrentPlaybackPosition(long currentPosition);
}
