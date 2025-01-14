package com.grassroot.academy.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.grassroot.academy.core.IEdxEnvironment;
import com.grassroot.academy.event.MediaStatusChangeEvent;
import com.grassroot.academy.model.VideoModel;
import com.grassroot.academy.model.db.DownloadEntry;
import com.grassroot.academy.module.db.DataCallback;
import com.grassroot.academy.module.db.IDatabase;
import com.grassroot.academy.util.FileUtil;
import com.grassroot.academy.util.Sha1Util;
import com.grassroot.academy.util.VideoUtil;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * BroadcastReceiver to receive the removable storage (such as SD-card) status events.
 */
@AndroidEntryPoint
public class MediaStatusReceiver extends BroadcastReceiver {

    @Inject
    IDatabase db;

    @Inject
    protected IEdxEnvironment environment;

    public MediaStatusReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String hashedUsername = environment.getLoginPrefs().isUserLoggedIn() ?
                Sha1Util.SHA1(environment.getLoginPrefs().getUsername()) : null;

        final String sdCardPath = intent.getDataString().replace("file://", "");
        final String action = intent.getAction();
        if (action != null) {
            boolean sdCardAvailable = false;
            switch (action) {
                case Intent.ACTION_MEDIA_REMOVED:
                case Intent.ACTION_MEDIA_UNMOUNTED:
                    sdCardAvailable = false;
                    handleSDCardUnmounted(hashedUsername, sdCardPath);
                    break;
                case Intent.ACTION_MEDIA_MOUNTED:
                    sdCardAvailable = true;
                    handleSDCardMounted(context, hashedUsername);
                    break;
            }
            EventBus.getDefault().postSticky(new MediaStatusChangeEvent(sdCardAvailable));
        }
    }

    private void handleSDCardUnmounted(String hashedUsername, final String sdCardPath) {
        db.getAllVideos(hashedUsername, new DataCallback<List<VideoModel>>() {
            @Override
            public void onResult(List<VideoModel> result) {
                for (VideoModel videoModel : result) {
                    if (videoModel.getFilePath() != null && videoModel.getFilePath().contains(sdCardPath)) {
                        VideoUtil.updateVideoDownloadState(db,
                                videoModel,
                                DownloadEntry.DownloadedState.ONLINE.ordinal()
                        );
                    }
                }
            }

            @Override
            public void onFail(Exception ex) {
                Log.e(this.getClass().getSimpleName(),
                        "Unable to get to get list of Videos"
                );
            }
        });
    }

    private void handleSDCardMounted(final Context context, String hashedUsername) {
        db.getAllVideos(hashedUsername, new DataCallback<List<VideoModel>>() {
            @Override
            public void onResult(List<VideoModel> result) {
                final String externalAppDir = FileUtil.getExternalAppDir(context).getAbsolutePath();
                final String removableStorageAppDir = FileUtil.getRemovableStorageAppDir(context).getAbsolutePath();
                final boolean downloadToSdCard = environment.getUserPrefs().isDownloadToSDCardEnabled();
                for (VideoModel videoModel : result) {
                    updateVideoDownloadFilePathState(
                            videoModel,
                            downloadToSdCard,
                            externalAppDir,
                            removableStorageAppDir
                    );
                }
            }

            @Override
            public void onFail(Exception ex) {
                Log.e(this.getClass().getSimpleName(),
                        "Unable to get list of Videos"
                );
            }
        });
    }

    /**
     * Utility method to update the downloaded video file info(if multiple file are exist in phone memory / SD-Card for
     * the single video).
     *
     * @param videoModel             Video info need to update.
     * @param downloadToSdCard       User preference from settings screen.
     * @param externalAppDir         Phone memory path where downloaded video files exist
     * @param removableStorageAppDir SD-Card storage path where downloaded video files exist.
     */
    private void updateVideoDownloadFilePathState(VideoModel videoModel, boolean downloadToSdCard,
                                                  String externalAppDir, String removableStorageAppDir) {
        final String videoPath = videoModel.getFilePath();
        final File file = new File(videoPath);
        if (file.exists()) {

            final File duplicateFile = getDuplicateFile(
                    videoPath,
                    externalAppDir,
                    removableStorageAppDir);

            if (duplicateFile != null && duplicateFile.exists()) {
                /*  If duplicate file exist in SD-Card, and download to SdCard is true from settings screen.
                    keep the duplicate file and delete file from Phone memory. */
                if (duplicateFile.getAbsolutePath().contains(removableStorageAppDir) && downloadToSdCard) {
                    videoModel = updateVideoModelWithPreferredFilePath(
                            videoModel,
                            duplicateFile,
                            file);

                    /*  If file exist in Phone memory, and download to SdCard is false from settings screen.
                    keep the phone memory file and delete duplicate file(file exist in SD-Card). */
                } else if (file.getAbsolutePath().contains(externalAppDir) && !downloadToSdCard) {
                    videoModel = updateVideoModelWithPreferredFilePath(
                            videoModel,
                            file,
                            duplicateFile);
                } else {
                    FileUtil.deleteRecursive(duplicateFile);
                }
            }

            VideoUtil.updateVideoDownloadState(db,
                    videoModel,
                    DownloadEntry.DownloadedState.DOWNLOADED.ordinal()
            );
        }
    }

    /**
     * Utility method to update the video info based on the preferred file and delete duplicate file.
     *
     * @param videoModel    Video model need to update.
     * @param preferredFile User preferred file.
     * @param duplicateFile Duplicate file need to delete.
     * @return return updated video model.
     */
    private VideoModel updateVideoModelWithPreferredFilePath(VideoModel videoModel, File preferredFile, File duplicateFile) {
        final DownloadEntry videoDownloadEntry = new DownloadEntry();
        videoDownloadEntry.setDownloadInfo(videoModel);
        videoDownloadEntry.filepath = preferredFile.getAbsolutePath();
        videoDownloadEntry.videoId = videoModel.getVideoId();
        videoModel = videoDownloadEntry;
        FileUtil.deleteRecursive(duplicateFile);
        return videoModel;
    }

    /**
     * Utility method to find the duplicate file path from Phone memory / SD-Card if there are two
     * files for the same video in Phone memory and SD-Card.
     *
     * @param videoPath              video path from the database.
     * @param externalAppDir         Phone memory storage path.
     * @param removableStorageAppDir SD-Card storage path.
     * @return return duplicate file.
     */
    private File getDuplicateFile(String videoPath, String externalAppDir, String removableStorageAppDir) {
        File downloadedFile = null;
        if (videoPath.contains(externalAppDir)) {
            downloadedFile = new File(videoPath.replace(externalAppDir, removableStorageAppDir));
        } else if (videoPath.contains(removableStorageAppDir)) {
            downloadedFile = new File(videoPath.replace(removableStorageAppDir, externalAppDir));
        }
        return downloadedFile;
    }
}
