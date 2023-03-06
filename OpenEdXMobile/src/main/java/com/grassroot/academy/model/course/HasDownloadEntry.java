package com.grassroot.academy.model.course;

import androidx.annotation.Nullable;

import com.grassroot.academy.model.db.DownloadEntry;
import com.grassroot.academy.module.storage.IStorage;

public interface HasDownloadEntry {
    @Nullable
    DownloadEntry getDownloadEntry(IStorage storage);

    @Nullable
    String getDownloadUrl();
}
