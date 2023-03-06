package com.grassroot.academy.test;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.grassroot.academy.view.Presenter;

@VisibleForTesting
public interface PresenterInjector {
    @Nullable
    Presenter<?> getPresenter();
}
