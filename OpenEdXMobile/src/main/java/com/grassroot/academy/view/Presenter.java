package com.grassroot.academy.view;


import androidx.annotation.NonNull;

public interface Presenter<V> {
    void attachView(@NonNull V view);
    void detachView();
    void destroy();
}
