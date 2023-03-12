package com.grassroot.academy.util.observer;

import androidx.annotation.NonNull;

public interface Observer<T> {
    void onData(@NonNull T data);
    void onError(@NonNull Throwable error);
}


