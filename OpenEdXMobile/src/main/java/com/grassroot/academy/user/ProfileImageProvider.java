package com.grassroot.academy.user;

import androidx.annotation.Nullable;

import com.grassroot.academy.model.user.ProfileImage;

public interface ProfileImageProvider {
    @Nullable
    ProfileImage getProfileImage();
}
